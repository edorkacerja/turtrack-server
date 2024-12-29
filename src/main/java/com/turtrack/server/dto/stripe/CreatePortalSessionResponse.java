package com.turtrack.server.dto.stripe;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreatePortalSessionResponse {
    private String url;
}