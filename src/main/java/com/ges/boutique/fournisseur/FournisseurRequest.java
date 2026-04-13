package com.ges.boutique.fournisseur;

import lombok.Data;

@Data
public class FournisseurRequest {
    private String nom;
    private String code;
    private String adresse;
    private String telephone;
    private String email;
    private String siteWeb;
    private String contactNom;
    private String contactTelephone;
    private String contactEmail;
    private String description;
    private String typeProduits;
    private String conditionsPaiement;
    private Integer delaiLivraison;
    private Integer note;
    private boolean actif;
}