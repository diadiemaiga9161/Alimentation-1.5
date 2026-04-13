package com.ges.boutique.utilisateur;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}