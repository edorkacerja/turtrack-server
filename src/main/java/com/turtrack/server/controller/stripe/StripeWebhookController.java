package com.turtrack.server.controller.stripe;

import com.turtrack.server.config.StripeConfig;
import com.turtrack.server.model.turtrack.User;
import com.turtrack.server.repository.turtrack.UserRepository;
import com.stripe.model.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

import static com.stripe.net.ApiResource.GSON;

@RestController
@RequestMapping("/webhook")
@AllArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeConfig stripeConfig;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<String> handleStripeEvent(HttpServletRequest request,
                                                    @RequestHeader("Stripe-Signature") String sigHeader) {
        String payload;
        Event event;

        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            payload = sb.toString();

            event = GSON.fromJson(payload, Event.class);

        } catch (IOException e) {
            System.err.println("Error reading request body: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

        if (stripeObject == null) {
            log.error("Failed to deserialize stripe object");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to deserialize stripe object");
        }

        try {
            // Handle the event
            switch (event.getType()) {
                // Subscription lifecycle events
                case "customer.subscription.created":
                    handleSubscriptionCreated((Subscription) stripeObject);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated((Subscription) stripeObject);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted((Subscription) stripeObject);
                    break;
                case "customer.subscription.trial_will_end":
                    handleSubscriptionTrialEnding((Subscription) stripeObject);
                    break;

                // Payment lifecycle events
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded((Invoice) stripeObject);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed((Invoice) stripeObject);
                    break;

                // Customer lifecycle events
                case "customer.created":
                    handleCustomerCreated((Customer) stripeObject);
                    break;
                case "customer.deleted":
                    handleCustomerDeleted((Customer) stripeObject);
                    break;

                default:
                    log.info("Unhandled event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing Stripe webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook event");
        }

        return ResponseEntity.ok().body("Webhook processed successfully");
    }

    private void handleSubscriptionCreated(Subscription subscription) {
        updateUserSubscriptionStatus(subscription.getCustomer(), User.SubscriptionStatus.ACTIVE);
        log.info("Subscription created: {}", subscription.getId());
    }

    private void handleSubscriptionUpdated(Subscription subscription) {
        String status = subscription.getStatus();
        User.SubscriptionStatus newStatus;

        switch (status) {
            case "active":
                newStatus = User.SubscriptionStatus.ACTIVE;
                break;
            case "canceled":
                newStatus = User.SubscriptionStatus.CANCELED;
                break;
            case "unpaid":
            case "past_due":
                newStatus = User.SubscriptionStatus.EXPIRED;
                break;
            default:
                newStatus = User.SubscriptionStatus.NONE;
        }

        updateUserSubscriptionStatus(subscription.getCustomer(), newStatus);
        log.info("Subscription updated: {} - Status: {}", subscription.getId(), status);
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        updateUserSubscriptionStatus(subscription.getCustomer(), User.SubscriptionStatus.EXPIRED);
        log.info("Subscription deleted: {}", subscription.getId());
    }

    private void handleSubscriptionTrialEnding(Subscription subscription) {
        // Notify user about trial ending
        log.info("Trial ending soon for subscription: {}", subscription.getId());
    }

    private void handleInvoicePaymentSucceeded(Invoice invoice) {
        if (invoice.getSubscription() != null) {
            updateUserSubscriptionStatus(invoice.getCustomer(), User.SubscriptionStatus.ACTIVE);
        }
        log.info("Payment succeeded for invoice: {}", invoice.getId());
    }

    private void handleInvoicePaymentFailed(Invoice invoice) {
        if (invoice.getSubscription() != null) {
            updateUserSubscriptionStatus(invoice.getCustomer(), User.SubscriptionStatus.EXPIRED);
            // You might want to notify the user about the failed payment
        }
        log.info("Payment failed for invoice: {}", invoice.getId());
    }

    private void handleCustomerCreated(Customer customer) {
        // This might be redundant if you're already handling this in your service
        log.info("Customer created: {}", customer.getId());
    }

    private void handleCustomerDeleted(Customer customer) {
        Optional<User> userOpt = userRepository.findByStripeCustomerId(customer.getId());
        userOpt.ifPresent(user -> {
            user.setStripeCustomerId(null);
            user.setSubscriptionStatus(User.SubscriptionStatus.NONE);
            userRepository.save(user);
        });
        log.info("Customer deleted: {}", customer.getId());
    }

    private void updateUserSubscriptionStatus(String customerId, User.SubscriptionStatus status) {
        Optional<User> userOpt = userRepository.findByStripeCustomerId(customerId);
        userOpt.ifPresent(user -> {
            user.setSubscriptionStatus(status);
            userRepository.save(user);
            log.info("Updated subscription status for user {} to {}", user.getEmail(), status);
        });
    }
}