package com.ges.boutique.vente;

import lombok.Data;

@Data
public class LigneVenteRequest {
    private Long produitId;
    private Integer quantite;
    private Double remisePourcentage;  // Nouveau champ
    private Double remiseMontant;      // Nouveau champ
}