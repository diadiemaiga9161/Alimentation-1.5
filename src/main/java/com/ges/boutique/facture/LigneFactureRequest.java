package com.ges.boutique.facture;

import lombok.Data;

@Data
public class LigneFactureRequest {
    private Long produitId;
    private String designation;
    private String description;
    private Integer quantite;
    private Double prixUnitaire;
    private Double remisePourcentage;
    private Double remiseMontant;
}