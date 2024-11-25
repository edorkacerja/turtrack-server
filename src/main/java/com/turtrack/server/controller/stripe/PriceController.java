package com.turtrack.server.controller.stripe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.turtrack.server.config.security.StripeConfig;
import com.turtrack.server.dto.turtrack.ProductDTO;
import com.turtrack.server.service.stripe.StripeService;
import com.turtrack.server.service.turtrack.ProductService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/prices")
@AllArgsConstructor
@Slf4j
public class PriceController {

    private final StripeService stripeService;

    @GetMapping("/current-price")
    public ResponseEntity<Price> getCurrentPrice(Principal principal) {
        Price price = stripeService.getCurrentPrice(principal.getName());
        return ResponseEntity.ok(price);
    }


}
