package com.ges.boutique.vente;

import lombok.Data;

@Data
public class VendeurDto {
    private Long id;
    private String username;
    private String nomComplet;
    private String email;
    private String telephone;
    private String role;
    private boolean actif;
}