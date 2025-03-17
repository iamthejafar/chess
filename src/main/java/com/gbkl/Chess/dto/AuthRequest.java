package com.gbkl.Chess.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
    private String username;
    private String displayName;
}
