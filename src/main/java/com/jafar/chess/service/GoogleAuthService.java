package com.jafar.chess.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class);
    @Value("${google.oauth.client-id}")
    private String clientId;

    public GoogleIdToken.Payload verifyToken(String idTokenString) throws Exception {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new IllegalArgumentException("ID token is required.");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("google.oauth.client-id is not configured");
        }

        // Clean the token - remove Bearer prefix and all whitespace
        String token = idTokenString.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }

        // Remove any whitespace, newlines, or carriage returns
        token = token.replaceAll("\\s+", "");

        // Validate JWT structure - must have exactly 2 dots
        long dotCount = token.chars().filter(ch -> ch == '.').count();
        if (dotCount != 2) {
            logger.error("Invalid JWT structure. Expected 2 dots, found {}. Token length: {}",
                    dotCount, token.length());
            throw new IllegalArgumentException(
                    String.format("Invalid JWT structure. Token must have exactly 3 parts separated by dots. Found %d dots.", dotCount)
            );
        }

        // Log token structure for debugging (first/last 20 chars only)
        if (logger.isDebugEnabled()) {
            String tokenPreview = token.length() > 40
                    ? token.substring(0, 20) + "..." + token.substring(token.length() - 20)
                    : token;
            logger.debug("Token structure validated. Preview: {}", tokenPreview);
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken = verifier.verify(token);

        if (idToken != null) {
            return idToken.getPayload();
        } else {
            throw new RuntimeException("Invalid ID token - verification failed.");
        }
    }
}