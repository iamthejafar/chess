package com.gbkl.Chess.service;

import com.gbkl.Chess.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FirestoreService {
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_USERNAMES = "usernames";

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public void saveUser(User user) {
        try {
            DocumentReference docRef = getFirestore().collection(COLLECTION_USERS).document(user.getUid());
            Map<String, Object> data = new HashMap<>();
            data.put("email", user.getEmail());
            data.put("username", user.getUsername());
            data.put("displayName", user.getDisplayName());
            data.put("createdAt", user.getCreatedAt());
            
            // Save user data
            ApiFuture<WriteResult> userResult = docRef.set(data);
            
            // Save username mapping
            DocumentReference usernameRef = getFirestore().collection(COLLECTION_USERNAMES).document(user.getUsername());
            Map<String, Object> usernameData = new HashMap<>();
            usernameData.put("uid", user.getUid());
            ApiFuture<WriteResult> usernameResult = usernameRef.set(usernameData);
            
            // Wait for both operations to complete
            userResult.get();
            usernameResult.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving user to Firestore", e);
        }
    }

    public User getUser(String uid) {
        try {
            DocumentReference docRef = getFirestore().collection(COLLECTION_USERS).document(uid);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                return document.toObject(User.class);
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error getting user from Firestore", e);
        }
    }

    public boolean isUsernameTaken(String username) {
        try {
            DocumentReference docRef = getFirestore().collection(COLLECTION_USERNAMES).document(username);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            return document.exists();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error checking username in Firestore", e);
        }
    }

    public void deleteUser(String uid, String username) {
        try {
            // Delete user document
            ApiFuture<WriteResult> userResult = getFirestore().collection(COLLECTION_USERS).document(uid).delete();
            
            // Delete username mapping
            ApiFuture<WriteResult> usernameResult = getFirestore().collection(COLLECTION_USERNAMES).document(username).delete();
            
            // Wait for both operations to complete
            userResult.get();
            usernameResult.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting user from Firestore", e);
        }
    }
}
