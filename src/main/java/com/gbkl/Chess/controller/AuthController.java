package com.gbkl.Chess.controller;

import com.gbkl.Chess.dto.AuthRequest;
import com.gbkl.Chess.dto.GuestUserRequest;
import com.gbkl.Chess.model.GuestUser;
import com.gbkl.Chess.model.User;
import com.gbkl.Chess.service.AuthService;
import com.gbkl.Chess.service.GuestUserService;
import com.gbkl.Chess.service.UserService;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GuestUserService guestUserService;
    private final UserService userService;

    public AuthController(AuthService authService, GuestUserService guestUserService, UserService userService) {
        this.authService = authService;
        this.guestUserService = guestUserService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest request) {
        try {
            // Check if username is taken (if provided)
            if (request.getUsername() != null && !request.getUsername().trim().isEmpty() 
                && userService.isUsernameTaken(request.getUsername())) {
                return ResponseEntity.badRequest().body("Username already taken");
            }

            // Create Firebase user
            UserRecord userRecord = authService.createUser(request.getEmail(), request.getPassword());
            String customToken = authService.createCustomToken(userRecord.getUid());
            
            // Create local user with username
            User user = userService.createUser(
                userRecord.getUid(),
                request.getEmail(),
                request.getUsername(),
                request.getDisplayName()
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("token", customToken);
            response.put("uid", userRecord.getUid());
            response.put("type", "registered");
            response.put("username", user.getUsername());
            response.put("displayName", user.getDisplayName());
            response.put("email", user.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/guest")
    public ResponseEntity<?> createGuestUser(@RequestBody(required = false) GuestUserRequest request) {
        String displayName = request != null ? request.getName() : null;
        GuestUser guestUser = guestUserService.createGuestUser(displayName);
        
        Map<String, String> response = new HashMap<>();
        response.put("token", guestUser.getToken());
        response.put("uid", guestUser.getId());
        response.put("type", "guest");
        response.put("username", guestUser.getUsername());
        response.put("displayName", guestUser.getDisplayName());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String token) {
        try {
            // Remove "Bearer " prefix if present
            token = token.startsWith("Bearer ") ? token.substring(7) : token;

            // First try to verify as a guest token
            GuestUser guestUser = guestUserService.validateGuestToken(token);
            if (guestUser != null) {
                Map<String, String> response = new HashMap<>();
                response.put("uid", guestUser.getId());
                response.put("username", guestUser.getUsername());
                response.put("displayName", guestUser.getDisplayName());
                response.put("type", "guest");
                return ResponseEntity.ok(response);
            }

            // If not a guest token, try Firebase token
            UserRecord firebaseUser = authService.verifyIdToken(token);
            User user = userService.getUser(firebaseUser.getUid());
            
            Map<String, String> response = new HashMap<>();
            response.put("uid", user.getUid());
            response.put("email", user.getEmail());
            response.put("type", "registered");
            response.put("username", user.getUsername());
            response.put("displayName", user.getDisplayName());
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
