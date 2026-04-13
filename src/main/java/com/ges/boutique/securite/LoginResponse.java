package com.ges.boutique.securite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private String nomComplet;
    private String email;
    private String telephone;
    private Long id; // Ajout de l'ID
}