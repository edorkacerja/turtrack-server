package com.turtrack.server.dto.payment;

import lombok.Data;

@Data
public class CreatePayment {
    private CreatePaymentItem[] items;

    @Data
    public static class CreatePaymentItem {
        private String id;
        private Long amount;
    }
}