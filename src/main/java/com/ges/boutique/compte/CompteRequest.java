package com.ges.boutique.compte;

import lombok.Data;

@Data
public class CompteRequest {
    private String nomBanque;
    private String numeroCompte;
    private String agence;
    private String titulaire;
    private Double soldeInitial;
    private String description;
}
