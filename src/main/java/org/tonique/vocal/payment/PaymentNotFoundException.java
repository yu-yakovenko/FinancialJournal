package org.tonique.vocal.payment;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long id) {
        super("Платіж не знайдено: id=" + id);
    }
}
