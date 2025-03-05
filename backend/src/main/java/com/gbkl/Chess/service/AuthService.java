package com.gbkl.Chess.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public UserRecord createUser(String email, String password) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setEmailVerified(false);

        return FirebaseAuth.getInstance().createUser(request);
    }

    public String createCustomToken(String uid) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().createCustomToken(uid);
    }

    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().getUserByEmail(email);
    }

    public void deleteUser(String uid) throws FirebaseAuthException {
        FirebaseAuth.getInstance().deleteUser(uid);
    }

    public UserRecord verifyIdToken(String idToken) throws FirebaseAuthException {
        String uid = FirebaseAuth.getInstance().verifyIdToken(idToken).getUid();
        return FirebaseAuth.getInstance().getUser(uid);
    }
}
