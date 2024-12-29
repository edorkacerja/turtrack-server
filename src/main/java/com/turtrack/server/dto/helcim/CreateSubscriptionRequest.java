package com.turtrack.server.dto.helcim;

import lombok.Data;

@Data
public class CreateSubscriptionRequest {
    private String customerCode; // optional if new customer
    private String firstName;
    private String lastName;
    private String email;
    private String planCode; // the subscription plan to subscribe to
}
