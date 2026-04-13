package com.ges.boutique.vente;

import lombok.Data;

@Data
public class LigneVenteDto {
    private Long produitId;
    private String produitNom;
    private Integer quantite;
    private Double prixUnitaire;
    private Double remisePourcentage;
    private Double remiseMontant;
    private Double prixApresRemise;
    private Double sousTotal;
    private Double montantRemise;
}