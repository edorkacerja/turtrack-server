package com.turtrack.server.controller.helcim;

import com.turtrack.server.dto.helcim.CreateCustomerRequest;
import com.turtrack.server.service.helcim.HelcimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final HelcimService helcimService;

    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        try {
            String response = helcimService.testConnection();
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to connect to Helcim API: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCustomer(@RequestBody CreateCustomerRequest request) {
        try {
            String customerCode = helcimService.createCustomer(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getStreet1(),
                    request.getStreet2(),
                    request.getCity(),
                    request.getProvince(),
                    request.getCountry(),
                    request.getPostalCode()
            );
            return ResponseEntity.ok(Map.of(
                    "customerCode", customerCode,
                    "message", "Customer created successfully"
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}