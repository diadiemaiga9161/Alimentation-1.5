package com.ges.boutique.produit;

import lombok.Data;

@Data
public class ProduitNiveauRequest {
    private String nom;
    private Integer ordre;
    private Integer facteur;
    private Double prixAchat;
    private Double prixVente;
}
