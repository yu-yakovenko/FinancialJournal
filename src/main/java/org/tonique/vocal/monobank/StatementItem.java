package org.tonique.vocal.monobank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatementItem(
        String id, long time, String description, int mcc, int originalMcc, boolean hold, long amount,
        long operationAmount, int currencyCode, long commissionRate, long cashbackAmount, long balance,
        String comment, String receiptId, String invoiceId, String counterEdrpou, String counterIban,
        String counterName
) {
}
