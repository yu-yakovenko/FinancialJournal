# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

FinancialJournal is a Spring Boot 3.5 / Java 21 application that pulls daily bank statements from the
Monobank personal API and logs them. It's an early-stage project — currently a single scheduled service
with no persistence layer or tests yet.

## Commands

Build (uses the Gradle wrapper — don't assume a system-wide `gradle` is installed):

```
./gradlew build
```

Run the app (requires `MONOBANK_TOKEN` and `MONOBANK_ACCOUNT_ID` env vars, see below):

```
./gradlew bootRun
```

Run all tests:

```
./gradlew test
```

Run a single test class or method:

```
./gradlew test --tests "org.tonique.vocal.SomeClassTest"
./gradlew test --tests "org.tonique.vocal.SomeClassTest.someMethod"
```

Note: `src/test/java` currently has no test sources.

## Configuration

`src/main/resources/application.yml` requires two environment variables at startup:

- `MONOBANK_TOKEN` — Monobank API personal token (`X-Token` header)
- `MONOBANK_ACCOUNT_ID` — the Monobank account to fetch statements for

These are secrets; never hardcode them or commit real values.

## Architecture

- `FinancialJournal` — `@SpringBootApplication` entry point with `@EnableScheduling` enabled.
- `service/MonobankStatementService` — the only component so far:
  - `loadPreviousDayStatement()` is `@Scheduled` (cron `0 5 0 * * *`, zone `Europe/Kyiv`) and runs daily
    at 00:05 Kyiv time, fetching the *previous* calendar day's statement and printing each transaction.
    Errors are caught and logged, not rethrown, so a failed fetch doesn't crash the scheduler.
  - `loadStatement(LocalDate)` is the reusable, throwing entry point (used by the scheduled job, and
    intended for direct/testable use) that computes the Kyiv-day `from`/`to` epoch-second window and
    calls `GET https://api.monobank.ua/personal/statement/{accountId}/{from}/{to}`.
  - `StatementItem` is a record mirroring the Monobank statement JSON shape (`@JsonIgnoreProperties(ignoreUnknown = true)`
    so unrecognized API fields don't break deserialization). Amounts (`amount`, `operationAmount`, etc.)
    are in the smallest currency unit (kopiykas) — divide by 100 for the decimal value, as done in the
    logging code.
  - `MonobankApiException` (nested `RuntimeException`) is thrown for non-200 responses from the Monobank API.
- There is no database/repository layer yet — the comment in `loadPreviousDayStatement()` marks where
  persistence (`statementRepository.saveAll(...)`) is intended to go.
- Log/console messages in this service are in Ukrainian; keep that convention when touching this file.
