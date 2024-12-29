package com.turtrack.server.dto.helcim;

import lombok.Data;

@Data
public class FinalizeSubscriptionRequest {
    private String customerCode;
    private String planCode;
    private String transactionId; // from HelcimPay.js response
    private String cardToken;     // from HelcimPay.js response
}
