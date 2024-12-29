package com.turtrack.server.dto.helcim;

import lombok.Data;

@Data
public class SubscriptionRequest {

    private CustomerDTO customer;
    private String customerCode;
    private String firstName;
    private String lastName;
    private String email;
    private String paymentToken; // Received from the client-side tokenization
    private String planCode;
    private String startDate; // Format: YYYY-MM-DD
    private String frequency;
    private String amount;

}