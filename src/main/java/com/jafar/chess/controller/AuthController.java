package com.jafar.chess.controller;

import com.jafar.chess.dto.request.GoogleAuthRequest;
import com.jafar.chess.dto.response.AuthResponse;
import com.jafar.chess.service.GoogleAuthService;
import com.jafar.chess.service.JwtService;
import com.jafar.chess.model.User;
import com.jafar.chess.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleAuthService googleAuthService;
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(
            @RequestBody GoogleAuthRequest request) {
        try {
            if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
                log.warn("Google auth request missing idToken");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponse.error("Missing idToken"));
            }

            var payload = googleAuthService.verifyToken(request.getIdToken());

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            String googleId = payload.getSubject();

            log.info("Authenticating user with email: {}", email);

            // Find or create user
            User user = userService.findOrCreateGoogleUser(googleId, email, name, picture);

            // Generate JWT token
            String jwt = jwtService.generateToken(user.getId(), email);

            // Return response
            AuthResponse response = AuthResponse.builder()
                    .token(jwt)
                    .userId(user.getId())
                    .build();

            log.info("User authenticated successfully: {}", user.getId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid Google auth request", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.error("Invalid idToken"));
        } catch (Exception e) {
            log.error("Error during Google authentication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Authentication failed"));
        }
    }

    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> authenticateAsGuest() {
        try {
            // Create guest user
            User user = userService.createGuestUser();

            // Generate JWT token for guest
            String jwt = jwtService.generateToken(user.getId(), null);

            // Return response
            AuthResponse response = AuthResponse.builder()
                    .token(jwt)
                    .userId(user.getId())
                    .build();

            log.info("Guest user created successfully: {}", user.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during guest authentication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Guest authentication failed"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Invalid token format"));
            }

            String token = authHeader.substring(7);
            String userId = jwtService.validateTokenAndGetUserId(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Invalid or expired token"));
            }

            User user = userService.getUser(userId);

            // Generate new token
            String newJwt = jwtService.generateToken(user.getId(), user.getEmail());

            AuthResponse response = AuthResponse.builder()
                    .token(newJwt)
                    .userId(user.getId())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token refresh failed"));
        }
    }
}
