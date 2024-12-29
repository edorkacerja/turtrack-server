package com.turtrack.server.controller.helcim;

import com.turtrack.server.service.helcim.HelcimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/checkout")
@Slf4j
public class CheckoutController {

    private final HelcimService helcimService;

//    @PostMapping("/initialize")
//    public ResponseEntity<Map<String, String>> initializeCheckout(@RequestBody CreateSessionRequest request) {
//        try {
//            double amount = request.getAmount();
//            String currency = request.getCurrency();
//            String customerCode = request.getCustomerCode();
//
//            // Check if customerId is provided; if not, create a new customer
//            if (customerCode == null || customerCode.isBlank()) {
//                customerCode = helcimService.createCustomer(
//                        request.getFirstName(),
//                        request.getLastName(),
//                        request.getEmail()
//                );
//            }
//
//            // Call HelcimService to initialize the checkout session
//            Map<String, String> tokens = helcimService.initializeCheckoutSessionWithCustomer(amount, currency, customerCode);
//
//            // Return the checkout token and secret token
//            return ResponseEntity.ok(tokens);
//        } catch (IOException e) {
//            log.error("Error initializing Helcim checkout session", e);
//            return ResponseEntity.status(500).body(Map.of("error", "Failed to initialize checkout session"));
//        }
//    }

}
