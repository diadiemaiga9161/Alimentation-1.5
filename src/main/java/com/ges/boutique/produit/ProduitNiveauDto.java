package com.ges.boutique.produit;

import lombok.Data;

@Data
public class ProduitNiveauDto {
    private Long id;
    private String nom;
    private Integer ordre;
    private Integer facteur;
    private Double prixVente;
    private Double prixAchat;
    private Integer stock;
    private Long parentId;
}
