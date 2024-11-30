package com.turtrack.server.controller.stripe;

import com.turtrack.server.config.security.StripeConfig;
import com.turtrack.server.dto.payment.CreateSessionRequest;
import com.turtrack.server.dto.payment.CreateSessionResponse;
import com.turtrack.server.dto.payment.CreatePortalSessionRequest;
import com.turtrack.server.dto.payment.CreatePortalSessionResponse;
import com.turtrack.server.dto.turtrack.ProductDTO;
import com.turtrack.server.service.stripe.StripeService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stripe.model.Price;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/checkout")
@Slf4j


public class CheckoutController {

    private final StripeConfig stripeConfig;
    private final StripeService stripeService;


    @PostMapping("/create-portal-session")
    public ResponseEntity<CreatePortalSessionResponse> createPortalSession(
            @RequestBody CreatePortalSessionRequest request) {
        try {
            String portalUrl = stripeService.createPortalSession(
                    request.getEmail(),
                    request.getReturnUrl()
//                    request.getSelectedPriceId(),
//                    request.getCurrentPriceId()
            );
            return ResponseEntity.ok(new CreatePortalSessionResponse(portalUrl));
        } catch (StripeException e) {
            log.error("Error creating portal session", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/create-checkout-session")
    public CreateSessionResponse createCheckoutSession(@RequestBody CreateSessionRequest createSessionRequest) throws StripeException {
        // Set the Stripe API key
        Stripe.apiKey = stripeConfig.getSecretKey();

//        if customer doesn't exist, how do i create it with the right name and email?

//        if(createSessionRequest.getCustomerId() == null) {
//
//
//        }


//        SessionCreateParams.CustomerUpdate customerUpdate = ;
        SessionCreateParams params =
                SessionCreateParams.builder()
//                        .setCustomer(createSessionRequest.getCustomerId()) // if null, will create a new customer in stripe.
                        .setCustomerEmail(createSessionRequest.getEmail())
//                        .setSuccessUrl("http://localhost:5173/payment/success")
                        .setReturnUrl("http://localhost:5173/payment/success")
                        .setCurrency(createSessionRequest.getCurrency())
//                        .addAllLineItem() // TODO: Try this. how does it work with many line items.
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(createSessionRequest.getPriceId())
                                        .setQuantity(1L)
                                        .build()
                        )
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setUiMode(SessionCreateParams.UiMode.EMBEDDED)
                        .build();

        // Create the session
        Session session = Session.create(params);

//        TODO: DO I need a createSession Response? aka. do i need the client reference id? When they want to update subs?
        // Return the session URL
        return CreateSessionResponse.builder()
                .clientSecret(session.getClientSecret())
                .connectedAccountId(session.getClientReferenceId())
                .build();
    }


}