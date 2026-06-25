package com.ges.boutique.commande;

import lombok.Data;

@Data
public class LigneCommandeRequest {
    private Long produitId;
    private Integer quantite;
    private Double prixUnitaire;
}
