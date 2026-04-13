package com.ges.boutique.utilisateur;

import lombok.Data;

@Data
public class UtilisateurDto {
    private Long id;
    private String username;
    private String nomComplet;
    private String email;
    private String telephone;
    private RoleUtilisateur role;
    private boolean actif;
}