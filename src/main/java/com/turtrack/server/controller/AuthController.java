package com.turtrack.server.controller;

import com.turtrack.server.dto.manager.UserDTO;
import com.turtrack.server.model.turtrack.User;
import com.turtrack.server.service.turtrack.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookies;

    @PostMapping("/register")
    public ResponseEntity<UserDTO.AuthResponse> register(
            @Valid @RequestBody UserDTO.RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        UserDTO.AuthResponse authResponse = userService.register(request);

        // Set JWT and refresh token cookies
        setAuthCookies(httpRequest, response, authResponse.getToken(), authResponse.getRefreshToken());

        // Remove tokens from response body for security
        authResponse.setToken(null);
        authResponse.setRefreshToken(null);

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO.AuthResponse> login(
            @Valid @RequestBody UserDTO.LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        UserDTO.AuthResponse authResponse = userService.authenticate(request);

        // Set JWT and refresh token cookies
        setAuthCookies(httpRequest, response, authResponse.getToken(), authResponse.getRefreshToken());

        // Remove tokens from response body for security
        authResponse.setToken(null);
        authResponse.setRefreshToken(null);

        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/refresh")
    public ResponseEntity<UserDTO.AuthResponse> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        UserDTO.AuthResponse authResponse = userService.refreshAccessToken(refreshToken);

        // Set new JWT cookie
        setAuthCookies(httpRequest, response, authResponse.getToken(), null);

        // Remove tokens from response body for security
        authResponse.setToken(null);
        authResponse.setRefreshToken(null);

        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO.AuthResponse> getCurrentUser(Authentication authentication) {
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
                User user = userService.findUserByEmail(email);
                return ResponseEntity.ok( UserDTO.AuthResponse.builder()
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .profilePicture(user.getImageUrl())
                        .subscriptionStatus(user.getSubscriptionStatus())
                        .build());
            }
        }
        return ResponseEntity.status(401).build();
    }

    private void setAuthCookies(HttpServletRequest httpRequest, HttpServletResponse response, String token, String refreshToken) {

        String domain = httpRequest.getServerName();

        // Set JWT cookie
        if (token != null) {
            Cookie jwtCookie = new Cookie("token", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(secureCookies); // Set to true in production (requires HTTPS)
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(15 * 60); // 15 minutes
            response.addCookie(jwtCookie);
        }

        // Set Refresh Token cookie
        if (refreshToken != null) {
            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(secureCookies);
            refreshCookie.setPath("/auth/refresh"); // Only sent to the refresh endpoint
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            response.addCookie(refreshCookie);
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Invalidate the JWT cookie
        Cookie jwtCookie = new Cookie("token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // Deletes the cookie
        response.addCookie(jwtCookie);

        // Invalidate the Refresh Token cookie
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/auth/refresh");
        refreshCookie.setMaxAge(0); // Deletes the cookie
        response.addCookie(refreshCookie);

        return ResponseEntity.noContent().build();
    }
}
