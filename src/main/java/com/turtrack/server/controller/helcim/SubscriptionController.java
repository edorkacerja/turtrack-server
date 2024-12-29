package com.turtrack.server.controller.helcim;

import com.turtrack.server.dto.helcim.CreateSubscriptionRequest;
import com.turtrack.server.dto.helcim.FinalizeSubscriptionRequest;
import com.turtrack.server.service.helcim.HelcimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscriptions")
@Slf4j
public class SubscriptionController {

    private final HelcimService helcimService;

    /**
     * Get subscription details for the current authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserSubscription( String customerCode) {
        try {
//            // Extract the user's email from the authenticated principal
//            String userEmail = userDetails.getUsername();

            // Fetch the subscription details using the user's email
            Map<String, Object> subscriptionDetails = helcimService.getSubscriptionsByCustomerCode(customerCode);

            return ResponseEntity.ok(subscriptionDetails);
        } catch (Exception e) {
            log.error("Error retrieving subscription details for user", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve subscription details"));
        }
    }

    /**
     * Step 1: Initialize subscription process
     * - Create a customer if none is provided
     * - Initialize a checkout session to capture payment details
     */
    @PostMapping("/create")
    public ResponseEntity<?> createSubscription(@RequestBody CreateSubscriptionRequest request) {
        try {
            String customerCode = request.getCustomerCode();

            // If no customerCode, create a new customer
            if (customerCode == null || customerCode.isBlank()) {
                customerCode = helcimService.createCustomer(
                        request.getFirstName(),
                        request.getLastName(),
                        request.getEmail()
                );
            }

            // Initialize checkout session to get checkoutToken
            Map<String, String> tokens = helcimService.initializeCheckoutSession(0.01, "USD");
            // Note: Even if amount is 0.00 here, this session is used to store card details securely.
            // Once we have the card, we can attach it to the subscription.

            return ResponseEntity.ok(Map.of(
                    "checkoutToken", tokens.get("checkoutToken"),
                    "customerCode", customerCode,
                    "planCode", request.getPlanCode()
            ));

        } catch (IOException e) {
            log.error("Error creating subscription", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to initialize subscription"));
        }
    }

    /**
     * Step 2: Finalize subscription after payment card is captured
     * - The frontend sends transactionId and cardToken from HelcimPay.js response
     * - Subscribe the customer to the given plan
     */
    @PostMapping("/finalize")
    public ResponseEntity<?> finalizeSubscription(@RequestBody FinalizeSubscriptionRequest request) {
        try {
            // Optionally, attach cardToken to customer if needed.
            // Helcim automatically vaults card details when using helcim-pay if the session is set up correctly.
            // If needed, you can set a default payment method for the customer before subscription.

            String subscriptionCode = helcimService.subscribeCustomerToPlan(request.getCustomerCode(), request.getPlanCode());

            // Return subscription details for frontend display
            Map<String, Object> subscriptionDetails = helcimService.getSubscriptionDetails(subscriptionCode);
            return ResponseEntity.ok(subscriptionDetails);
        } catch (IOException e) {
            log.error("Error finalizing subscription", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to finalize subscription"));
        }
    }

    /**
     * Cancel a subscription
     */
    @PostMapping("/{subscriptionCode}/cancel")
    public ResponseEntity<?> cancelSubscription(@PathVariable String subscriptionCode) {
        try {
            helcimService.cancelSubscription(subscriptionCode);
            return ResponseEntity.ok(Map.of("message", "Subscription cancelled successfully"));
        } catch (Exception e) {
            log.error("Error cancelling subscription", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to cancel subscription"));
        }
    }

    /**
     * Get subscription details by subscriptionCode
     */
    @GetMapping("/{subscriptionCode}")
    public ResponseEntity<?> getSubscriptionByCode(@PathVariable String subscriptionCode) {
        try {
            Map<String, Object> details = helcimService.getSubscriptionDetails(subscriptionCode);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            log.error("Error retrieving subscription details", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve subscription details"));
        }
    }
}
