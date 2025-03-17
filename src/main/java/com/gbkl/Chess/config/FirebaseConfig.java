package com.gbkl.Chess.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
public class FirebaseConfig {

    private final Environment environment;

    public FirebaseConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void initializeFirebase() throws IOException {
        String firebaseConfig = environment.getProperty("FIREBASE_CONFIG");

        if (Objects.isNull(firebaseConfig)) {
            System.out.println("FIREBASE_CONFIG not found, using default credentials file...");
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ClassPathResource("firebase-credentials.json").getInputStream()))
                    .build();

            // Ensure Firebase is initialized only once
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized using the default file.");
            }
        } else {
            System.out.println("FIREBASE_CONFIG retrieved");

            if (!firebaseConfig.trim().startsWith("{") || !firebaseConfig.trim().endsWith("}")) {
                throw new IllegalStateException("FIREBASE_CONFIG is not a valid JSON string");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ByteArrayInputStream(firebaseConfig.getBytes(StandardCharsets.UTF_8))))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized successfully with FIREBASE_CONFIG.");
            }
        }
    }
}