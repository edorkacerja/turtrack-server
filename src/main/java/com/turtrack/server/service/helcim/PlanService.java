package com.turtrack.server.service.helcim;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turtrack.server.config.HelcimConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final HelcimConfig helcimConfig;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetch all available plans from Helcim API
     */
    public List<Map<String, Object>> getAvailablePlans() throws IOException {
        Request request = new Request.Builder()
                .url(helcimConfig.getBaseUrl() + "/v2/payment-plans")
                .get()
                .addHeader("accept", "application/json")
                .addHeader("api-token", helcimConfig.getApiToken())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Helcim API error: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // Parse the plans into a list of maps
            List<Map<String, Object>> plans = new ArrayList<>();
            jsonNode.path("data").forEach(plan -> {
                plans.add(objectMapper.convertValue(plan, Map.class));
            });

            return plans;
        }
    }
}
