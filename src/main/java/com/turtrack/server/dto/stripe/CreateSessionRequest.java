package com.turtrack.server.dto.stripe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {
    private String customerId;
    private String customerCode;
    private String email;
    private String firstName;
    private String lastName;
    private String priceId; // do we need this ?
    private double amount;
    private String currency;
}