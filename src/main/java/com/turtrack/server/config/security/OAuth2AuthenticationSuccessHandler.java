package com.turtrack.server.config.security;

import com.turtrack.server.model.turtrack.User;
import com.turtrack.server.repository.turtrack.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider; // Inject JwtTokenProvider
    @Value("${app.oauth2.redirectUri}")
    private String clientRedirectUri;
    @Value("${app.cookie.secure:true}")
    private boolean secureCookies;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // Check if the user exists, if not, create a new user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .firstName(oAuth2User.getAttribute("given_name"))
                    .lastName(oAuth2User.getAttribute("family_name"))
                    .provider(User.AuthProvider.GOOGLE)
                    .subscriptionStatus(User.SubscriptionStatus.NONE)
                    .isActive(true)
                    .build();
            return userRepository.save(newUser);
        });

        // Generate JWT and Refresh Tokens
        String token = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // Set tokens as secure, HTTP-only cookies
        setAuthCookies(response, token, refreshToken);

        // Redirect to frontend without tokens in URL
        String redirectUrl = UriComponentsBuilder.fromUriString(clientRedirectUri)
                .queryParam("success", "true")
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }

    private void setAuthCookies(HttpServletResponse response, String token, String refreshToken) {
        if (token != null) {
            String jwtCookie = String.format(
                    "token=%s; Max-Age=%d; Path=/; Secure; HttpOnly; SameSite=None",
                    token, 15 * 60
            );
            response.addHeader("Set-Cookie", jwtCookie);
        }

        if (refreshToken != null) {
            String refreshCookie = String.format(
                    "refreshToken=%s; Max-Age=%d; Path=/auth/refresh; Secure; HttpOnly; SameSite=None",
                    refreshToken, 7 * 24 * 60 * 60
            );
            response.addHeader("Set-Cookie", refreshCookie);
        }
    }
}
