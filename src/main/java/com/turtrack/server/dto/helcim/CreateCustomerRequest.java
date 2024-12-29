package com.turtrack.server.dto.helcim;

import lombok.Data;

@Data
public class CreateCustomerRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String street1;
    private String street2;
    private String city;
    private String province;
    private String country;
    private String postalCode;
}