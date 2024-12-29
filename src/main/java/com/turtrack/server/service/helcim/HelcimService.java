package com.turtrack.server.service.helcim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turtrack.server.config.HelcimConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelcimService {

    private final HelcimConfig helcimConfig;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();


    public String testConnection() throws IOException {
        if (!StringUtils.hasText(helcimConfig.getApiToken())) {
            throw new IllegalStateException("Helcim API token is not configured");
        }

        Request request = new Request.Builder()
                .url(helcimConfig.getBaseUrl() + "/v2/connection-test")
                .get()
                .addHeader("accept", "application/json")
                .addHeader("api-token", helcimConfig.getApiToken())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Helcim API error: " + response.code() + " - " + response.message());
            }

            return response.body().string();
        }
    }

    public String createCustomer(String firstName, String lastName, String email) throws IOException {
        return createCustomer(firstName, lastName, email, null, null, null, null, null, null, null);
    }

    public String createCustomer(String firstName, String lastName, String email, String phone,
                                 String street1, String street2, String city,
                                 String province, String country, String postalCode) throws IOException {
        if (!StringUtils.hasText(helcimConfig.getApiToken())) {
            throw new IllegalStateException("Helcim API token is not configured");
        }

        // Build billing address object
//        Map<String, String> billingAddress = new HashMap<>();
//        billingAddress.put("name", firstName + " " + lastName);
//        billingAddress.put("street1", street1);
//        if (StringUtils.hasText(street2)) {
//            billingAddress.put("street2", street2);
//        }
//        billingAddress.put("city", city);
//        billingAddress.put("province", province);
//        billingAddress.put("country", country);
//        billingAddress.put("postalCode", postalCode);
//        billingAddress.put("email", email);
//        if (StringUtils.hasText(phone)) {
//            billingAddress.put("phone", phone);
//        }

        // Build main request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contactName", firstName + " " + lastName);
        if (StringUtils.hasText(phone)) {
            requestBody.put("cellPhone", phone);
        }
//        requestBody.put("billingAddress", billingAddress);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(helcimConfig.getBaseUrl() + "/v2/customers")
                .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
                .addHeader("api-token", helcimConfig.getApiToken())
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                throw new IOException("Helcim API error: " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (jsonNode.has("errors")) {
                throw new IOException("Helcim API error: " + jsonNode.get("errors").asText());
            }

            return jsonNode.path("customerCode").asText();
        }
    }


//    public String createCustomer(String firstName, String lastName, String email) throws IOException {
//        if (!StringUtils.hasText(helcimConfig.getApiToken())) {
//            throw new IllegalStateException("Helcim API token is not configured");
//        }
//
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("contactName", firstName + " " + lastName);
//        requestBody.put("billingAddress", Map.of("email", email));
//
//        String jsonBody = objectMapper.writeValueAsString(requestBody);
//
//        Request request = new Request.Builder()
//                .url(helcimConfig.getBaseUrl() + "/v2/customers")
//                .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
//                .addHeader("api-token", helcimConfig.getApiToken())
//                .addHeader("accept", "application/json")
//                .addHeader("Content-Type", "application/json")
//                .build();
//
//        try (Response response = httpClient.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                String errorBody = response.body().string();
//                throw new IOException("Helcim API error: " + response.code() + " - " + errorBody);
//            }
//
//            String responseBody = response.body().string();
//            JsonNode jsonNode = objectMapper.readTree(responseBody);
//
//            if (jsonNode.has("errors")) {
//                throw new IOException("Helcim API error: " + jsonNode.get("errors").asText());
//            }
//
//            return jsonNode.path("customerCode").asText();
//        }
//    }

    public Map<String, String> initializeCheckoutSession(double amount, String currency) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("paymentType", "purchase");
        requestBody.put("amount", amount);
        requestBody.put("currency", currency);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(helcimConfig.getBaseUrl() + "/v2/helcim-pay/initialize")
                .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
                .addHeader("api-token", helcimConfig.getApiToken())
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Helcim API error: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("checkoutToken", jsonNode.path("checkoutToken").asText());
            tokens.put("secretToken", jsonNode.path("secretToken").asText());

            return tokens;
        }
    }

    public String subscribeCustomerToPlan(String customerCode, String planCode) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerCode", customerCode);
        requestBody.put("planCode", planCode);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(helcimConfig.getBaseUrl() + "/v2/subscriptions")
                .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
                .addHeader("api-token", helcimConfig.getApiToken())
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Helcim API error: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.path("subscriptionCode").asText();
        }
    }

    public void cancelSubscription(String subscriptionCode) throws IOException {
        Request request = new Request.Builder()
                .url(helcimConfig.getBaseUrl() + "/v2/subscriptions/" + subscriptionCode + "/cancel")
                .post(RequestBody.create(null, new byte[0]))
                .addHeader("api-token", helcimConfig.getApiToken())
                .addHeader("accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Helcim API error: " + response.code() + " - " + response.message());
            }
        }
    }

    public Map<String, Object> getSubscriptionDetails(String subscriptionCode) throws IOException {
        Request request = new Request.Builder()
                .url(helcimConfig.getBaseUrl() + "/v2/subscriptions/" + subscriptionCode)
                .get()
                .addHeader("api-token", helcimConfig.getApiToken())
                .addHeader("accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Helcim API error: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, Map.class);
        }
    }

    public Map<String, Object> getSubscriptionByEmail(String email) throws IOException {
        // Implement the logic to fetch subscription details based on the user's email
        // This might involve calling Helcim's API or querying your database

        return null;
    }

    public Map<String, Object> getSubscriptionsByCustomerCode(String customerCode) throws IOException {
        HttpUrl url = HttpUrl.parse(helcimConfig.getBaseUrl() + "/v2/subscriptions")
                .newBuilder()
                .addQueryParameter("customerCode", customerCode)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("api-token", helcimConfig.getApiToken())
                .addHeader("accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, Map.class);
        }
    }
}
