package com.ges.boutique.inventaire;

import lombok.Data;

@Data
public class InventaireDto {
    private Long produitId;
    private String produitNom;
    private Integer quantiteActuelle;
    private Integer seuilAlerte;
    private boolean stockFaible;
    private Double valeurStock;
}