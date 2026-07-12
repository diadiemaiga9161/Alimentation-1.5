package com.ges.boutique.produit;

import lombok.Data;

@Data
public class ProduitNiveauRequest {
    private String nom;
    private Integer ordre;    // optionnel — calculé depuis parentId si absent
    private Long parentId;    // null = niveau racine (le plus grand)
    private Integer facteur;  // combien de CE niveau dans 1 unité du parent
    private Double prixAchat;
    private Double prixVente;
    private Integer stock;
}
