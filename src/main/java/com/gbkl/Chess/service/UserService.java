package com.gbkl.Chess.service;

import com.gbkl.Chess.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UsernameService usernameService;
    private final FirestoreService firestoreService;

    public UserService(UsernameService usernameService, FirestoreService firestoreService) {
        this.usernameService = usernameService;
        this.firestoreService = firestoreService;
    }

    public User createUser(String uid, String email, String username, String displayName) {
        // Check if username is taken in Firestore
        if (username == null || username.trim().isEmpty()) {
            username = usernameService.generateUniqueUsername();
            while (firestoreService.isUsernameTaken(username)) {
                username = usernameService.generateUniqueUsername();
            }
        } else if (firestoreService.isUsernameTaken(username)) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = new User(uid, email, username, displayName);
        firestoreService.saveUser(user);
        return user;
    }

    public User getUser(String uid) {
        return firestoreService.getUser(uid);
    }

    public void removeUser(String uid) {
        User user = firestoreService.getUser(uid);
        if (user != null) {
            firestoreService.deleteUser(uid, user.getUsername());
        }
    }

    public boolean isUsernameTaken(String username) {
        return firestoreService.isUsernameTaken(username);
    }
}
