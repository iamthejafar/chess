package com.gbkl.Chess.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
@NoArgsConstructor // Required for Firestore deserialization
public class User {
    @DocumentId
    private String uid;
    private String email;
    private String username;
    private String displayName;
    private Timestamp createdAt;

    public User(String uid, String email, String username, String displayName) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.displayName = displayName != null ? displayName : username;
        this.createdAt = Timestamp.ofTimeSecondsAndNanos(
            LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), 0
        );
    }

    public LocalDateTime getCreatedAtDateTime() {
        return createdAt != null ? 
            LocalDateTime.ofEpochSecond(createdAt.getSeconds(), 0, ZoneOffset.UTC) : 
            null;
    }
}
