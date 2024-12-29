package com.turtrack.server.dto.stripe;

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