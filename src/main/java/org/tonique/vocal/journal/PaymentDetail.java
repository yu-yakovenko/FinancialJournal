package org.tonique.vocal.journal;

import org.tonique.vocal.payment.PaymentSource;

import java.time.LocalDate;

public record PaymentDetail(
        Long id,
        PaymentSource source,
        long amountKopiykas,
        LocalDate paymentDate,
        String comment,
        String senderName
) {
}
