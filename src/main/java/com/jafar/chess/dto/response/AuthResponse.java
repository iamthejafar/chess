package com.jafar.chess.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String userId;
    private String error;

    public static AuthResponse error(String message) {
        return AuthResponse.builder().error(message).build();
    }
}