package com.turtrack.server.controller.stripe;

import com.stripe.model.Price;
import com.turtrack.server.service.stripe.StripeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prices")
@AllArgsConstructor
@Slf4j
public class PriceController {

    private final StripeService stripeService;

    @GetMapping("/current-price")
    public ResponseEntity<Price> getCurrentPrice(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email;
            // Handle different types of Authentication
            if (authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                email = oauth2User.getAttribute("email");
            } else if (authentication.getPrincipal() instanceof UserDetails) {
                email = ((UserDetails) authentication.getPrincipal()).getUsername();
            } else {
                email = authentication.getName();
            }

            if (email != null) {
                Price price = stripeService.getCurrentPrice(email);
                return ResponseEntity.ok(price);
            }
        }
        return ResponseEntity.status(401).build();
    }
}