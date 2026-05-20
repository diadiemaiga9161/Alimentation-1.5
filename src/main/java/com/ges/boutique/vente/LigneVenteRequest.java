package com.ges.boutique.vente;

import lombok.Data;

@Data
public class LigneVenteRequest {
    private Long produitId;
    private Integer quantite;
    private Double prixUnitaire;        // Prix modifiable à la volée
    private Double remisePourcentage;
    private Double remiseMontant;
}