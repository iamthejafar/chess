package com.jafar.chess.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    private String id;

    private String name;

    private String username;

    private String picture;

    @Column(unique = true)
    private String googleId;

    @Column(unique = true)
    private String email;

    @Builder.Default
    private boolean isGuest = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastLoginAt;

    @Builder.Default
    private Integer rating = 1200;

    @Builder.Default
    private Integer gamesPlayed = 0;

    @Builder.Default
    private Integer gamesWon = 0;

    @Builder.Default
    private Integer gamesLost = 0;

    @Builder.Default
    private Integer gamesDraw = 0;
}
