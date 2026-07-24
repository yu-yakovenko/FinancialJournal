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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class MonobankClient {

    private static final String API_URL =
            "https://api.monobank.ua/personal/statement";

    public static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");

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

        URI uri = URI.create(
                API_URL + "/" + accountId + "/" + from + "/" + to
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
