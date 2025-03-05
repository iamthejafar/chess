package com.gbkl.Chess.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GuestUser {
    private String id;
    private String displayName;
    private String username;
    private LocalDateTime createdAt;
    private String token;

    public GuestUser(String username, String displayName) {
        this.id = "guest_" + UUID.randomUUID().toString();
        this.username = username;
        this.displayName = displayName;
        this.createdAt = LocalDateTime.now();
        this.token = UUID.randomUUID().toString();
    }
}
