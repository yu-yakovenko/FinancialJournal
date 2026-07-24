# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

FinancialJournal ("Журнал оплат вокальної школи") is a payment-tracking app for a vocal school that gets
paid via a Monobank FOP account. It ingests daily bank statements, matches each payment to a student and
tariff by parsing the hand-typed comment, and shows a color-coded student×month journal so the owner can
see who's paid, partially paid, or missed a month. Stack: Spring Boot 3.5 / Java 21 / PostgreSQL backend
(`src/`) with a REST API, and a separate React + TypeScript + Vite SPA (`frontend/`).

## Commands

Backend (Gradle wrapper — don't assume a system-wide `gradle` is installed):

```
./gradlew build
./gradlew bootRun     # requires env vars, see Configuration below
./gradlew test
./gradlew test --tests "org.tonique.vocal.SomeClassTest"
./gradlew test --tests "org.tonique.vocal.SomeClassTest.someMethod"
```

Frontend (`cd frontend` first):

```
npm run dev      # Vite dev server on :5173, proxies /api to localhost:8080
npm run build    # tsc -b && vite build
npm run lint      # oxlint
```

## Configuration

`src/main/resources/application.yml` reads these environment variables at startup:

- `DB_HOST` / `DB_PORT` / `DB_NAME` (default `localhost` / `5432` / `financial_journal`)
- `DB_USER` (default `postgres`) / `DB_PASSWORD` — PostgreSQL connection; `ddl-auto: update`, so schema
  changes apply automatically on boot, no migrations tool
- `MONOBANK_TOKEN` — Monobank API personal token (`X-Token` header)
- `MONOBANK_ACCOUNT_ID` — the Monobank account to fetch statements for
- `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`) — see `config/CorsConfig`

These are secrets; never hardcode them or commit real values.

## Architecture

### Ingestion pipeline (`monobank/`, `payment/`, `ingestion/`)

- `MonobankClient` calls `GET /personal/statement/{accountId}/{from}/{to}`. `loadStatement(LocalDate)`
  fetches one Kyiv calendar day (used by the daily job). `loadStatement(LocalDate from, LocalDate to)`
  is for historical backfill: Monobank enforces a 31-day window and roughly 1 request/60s per token, so
  it splits the range via `DateRangeChunker` (pure, unit-tested) and sleeps between chunk requests.
- `MonobankIngestionScheduler` is `@Scheduled` (cron `0 5 0 * * *`, zone `Europe/Kyiv`) and fetches
  *yesterday's* statement nightly via `ingest(LocalDate)`. `backfill(from, to)` is the one-off historical
  path, wired to `POST /api/admin/ingest/backfill` (and the `/admin` frontend page) for loading old
  payments the daily job never saw. Both swallow exceptions and log in Ukrainian rather than crash.
- `PaymentIngestionService.ingest(List<StatementItem>)` turns raw items into `Payment` rows. Per item:
  1. Skip if `amount <= 0` (outgoing/fees) or `monobankTransactionId` already exists (idempotency —
     unique DB constraint backs this, so re-running a backfill over an already-ingested range is safe).
  2. Each item's own `time` (epoch seconds) — not a shared batch date — determines its transaction date,
     so a single backfill request spanning many months still resolves periods correctly.
  3. `PaymentCommentParser` matches `"Оплата за уроки вокалу, МІСЯЦЬ, ПІБ"` against a Unicode-aware regex
     (`Pattern.UNICODE_CASE` is required — plain `CASE_INSENSITIVE` is ASCII-only and misses capitalized
     Cyrillic). Unparseable comments → `NEEDS_REVIEW`.
  4. The declared month has no year; `resolvePeriod` defaults to the transaction's year and shifts by one
     if the declared month is >6 months away (handles paying in January for December).
  5. **The amount must uniquely identify one tariff plan's price for that period** (via
     `TariffPricingService.plansForAmountAt`) *before* any name matching happens. A student can hold
     several tariffs at once, so there's no single "expected" amount to fall back on — an amount that's
     ambiguous or matches no plan goes straight to `NEEDS_REVIEW` without even touching the roster.
  6. `NameMatcher` then matches the payer name against the active student roster; zero candidates
     auto-creates a `Student`, more than one is `NEEDS_REVIEW`, exactly one links the payment.
  7. `EnrollmentService.ensureActive` links student↔tariff for that period.
- Anything left `NEEDS_REVIEW` is fixed up manually via `PaymentController` (`resolve`, `ignore`, `patch`).

### Name matching (`student/NameMatcher`)

Deterministic, no fuzzy/ML matching — ambiguity always routes to manual review rather than guessing.
Handles two comment styles: full name (3 tokens, positional match) and surname+initials
(`Прізвище О.П.` — surname plus first/patronymic initial). Note: initials are compared with `.equals()`,
not `!=`/`==` — Cyrillic `Character` boxing falls outside Java's cache range, so reference comparison is
a real bug trap here.

### Tariffs (`tariff/`)

Tariffs are admin-editable, not a fixed enum. `TariffPlan` is stable identity (label, service type);
`TariffRate` is one immutable row per price change (`amountKopiykas` + `effectiveFrom`) — changing a price
never rewrites history. `TariffPricing` is the pure date-aware lookup logic (`amountForPlanAt`,
`plansForAmountAt`, picks the latest rate with `effectiveFrom <= asOf`); `TariffPricingService` wraps it
with persistence. `TariffSeeder` seeds the four tariffs that used to be hard-coded, once, on first boot.

`TariffPlan` overrides `equals()`/`hashCode()` to be ID-based (with identity fallback pre-persist) —
without this, the same DB row loaded in separate `@Transactional` calls compares unequal, which silently
broke price lookups before it was added. Keep this in mind when adding other entities that get compared
across service boundaries.

### Multi-tariff enrollment (`enrollment/`)

A student can be on several tariffs simultaneously (e.g. choir *and* individual lessons). `TariffEnrollment`
(student + tariffPlan + `validFrom` + nullable `validTo`, null = currently active) tracks each independently.
`EnrollmentService.ensureActive` reuses an existing active enrollment for a (student, tariff) pair instead
of creating duplicates — called both from ingestion and from manual admin actions.

### Journal (`journal/JournalService`)

Builds one row per (student, tariff enrollment) that overlaps the requested year — a student on two
tariffs gets two independent rows/colors. Cells outside `[validFrom, validTo]` are `null` (not shown).
Color rule per cell: sum of `MATCHED` payments for that student+tariff+month vs. the tariff's price *as of
that month* (so a later price change doesn't retroactively recolor old months) — `0` → RED, `>= expected`
→ GREEN, otherwise YELLOW (including "paid something but no confirmed expected amount").

### REST API (`api/`)

`JournalController`, `StudentController`, `PaymentController`, `TariffController`, `EnrollmentController`,
`AdminController` (manual/backfill ingestion triggers). DTOs are records in `api/dto/`; money crosses the
API boundary in UAH (`BigDecimal`) and is converted to kopiykas via `MoneyConversion.toKopiykas` —
internally everything is kopiykas (`long`) to avoid floating point. `ApiExceptionHandler` maps
`*NotFoundException` → 404 and `IllegalArgumentException`/`ArithmeticException` → 400.

### Frontend (`frontend/`)

Routes (`App.tsx` + `NavBar`): `/` journal grid, `/students`, `/tariffs`, `/unmatched`, `/admin` (backfill
trigger). `src/api/client.ts` is a single fetch wrapper (`request<T>`) with one method per endpoint on the
`api` object; types live in `src/api/types.ts`. Forms follow a consistent `saving`/`error` state pattern
(disable submit button, swap its label, show an `error-banner`) — see `CashPaymentForm.tsx` as the
reference implementation.

## Testing conventions

No Spring-context tests exist (`@SpringBootTest`/`@DataJpaTest`) — pure logic (`NameMatcher`,
`PaymentCommentParser`, `TariffPricing`, `DateRangeChunker`) is tested as plain static-method unit tests,
and services are tested with Mockito (`@ExtendWith(MockitoExtension.class)`) mocking their repository/
collaborator dependencies. AssertJ (`assertThat`) for assertions. Keep new tests in this style unless a
change genuinely needs a real DB/Spring context.

Log/console messages throughout the backend are in Ukrainian; keep that convention when touching ingestion
or admin-facing code.
