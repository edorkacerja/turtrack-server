package com.turtrack.server.dto.stripe;


import com.turtrack.server.model.turtrack.TurtrackSubscription;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateSubscriptionResponse {
    private TurtrackSubscription turtrackSubscription;
    private String clientSecret;
}
