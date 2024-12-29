package com.turtrack.server.dto.stripe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSessionResponse {
    private String clientSecret;
    private String connectedAccountId;
}