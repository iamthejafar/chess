package com.gbkl.Chess.service;

import com.gbkl.Chess.model.GuestUser;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GuestUserService {
    private final Map<String, GuestUser> guestUsers = new ConcurrentHashMap<>();
    private final UsernameService usernameService;

    public GuestUserService(UsernameService usernameService) {
        this.usernameService = usernameService;
    }

    public GuestUser createGuestUser(String displayName) {
        String username = usernameService.generateUniqueUsername();
        GuestUser guestUser = new GuestUser(username, displayName != null ? displayName : username);
        guestUsers.put(guestUser.getId(), guestUser);
        return guestUser;
    }

    public GuestUser getGuestUser(String id) {
        return guestUsers.get(id);
    }

    public GuestUser validateGuestToken(String token) {
        return guestUsers.values().stream()
                .filter(user -> user.getToken().equals(token))
                .findFirst()
                .orElse(null);
    }

    public void removeGuestUser(String id) {
        GuestUser user = guestUsers.get(id);
        if (user != null) {
            usernameService.releaseUsername(user.getUsername());
            guestUsers.remove(id);
        }
    }
}
