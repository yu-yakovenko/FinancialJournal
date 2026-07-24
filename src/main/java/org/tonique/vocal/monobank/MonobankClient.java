package org.tonique.vocal.monobank;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class MonobankClient {

    private static final String API_URL =
            "https://api.monobank.ua/personal/statement";

    public static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");

    /** Monobank's real limit on this endpoint is one request per ~60s per token. */
    private static final Duration REQUEST_DELAY = Duration.ofSeconds(61);

    /** Monobank's real limit on this endpoint is a 31-day window per request. */
    private static final int MAX_DAYS_PER_REQUEST = 31;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;
    private final String accountId;

    public MonobankClient(
            ObjectMapper objectMapper,
            @Value("${monobank.token}") String token,
            @Value("${monobank.account-id}") String accountId
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.token = token;
        this.accountId = accountId;
    }

    public List<StatementItem> loadStatement(LocalDate date)
            throws IOException, InterruptedException {

        long from = date
                .atStartOfDay(KYIV_ZONE)
                .toEpochSecond();

        long to = date
                .plusDays(1)
                .atStartOfDay(KYIV_ZONE)
                .minusSeconds(1)
                .toEpochSecond();

        return fetchChunk(from, to);
    }

    /**
     * Loads a statement across an arbitrary date range, splitting it into ≤31-day
     * requests (Monobank's per-request window limit) and pausing between them to
     * respect Monobank's ~1-request-per-60s rate limit on this endpoint.
     */
    public List<StatementItem> loadStatement(LocalDate from, LocalDate to)
            throws IOException, InterruptedException {

        List<DateRangeChunker.Chunk> chunks = DateRangeChunker.chunk(from, to, MAX_DAYS_PER_REQUEST);

        List<StatementItem> items = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            if (i > 0) {
                Thread.sleep(REQUEST_DELAY.toMillis());
            }
            DateRangeChunker.Chunk chunk = chunks.get(i);
            long chunkFrom = chunk.from().atStartOfDay(KYIV_ZONE).toEpochSecond();
            long chunkTo = chunk.to().plusDays(1).atStartOfDay(KYIV_ZONE).minusSeconds(1).toEpochSecond();
            items.addAll(fetchChunk(chunkFrom, chunkTo));
        }
        return items;
    }

    private List<StatementItem> fetchChunk(long fromEpoch, long toEpoch)
            throws IOException, InterruptedException {

        URI uri = URI.create(
                API_URL + "/" + accountId + "/" + fromEpoch + "/" + toEpoch
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("X-Token", token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new MonobankApiException(
                    "Monobank API повернув HTTP %d: %s"
                            .formatted(
                                    response.statusCode(),
                                    response.body()
                            )
            );
        }

        return objectMapper.readValue(
                response.body(),
                new TypeReference<List<StatementItem>>() {}
        );
    }
}
