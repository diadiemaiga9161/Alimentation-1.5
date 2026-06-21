package com.ges.boutique.vente;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneRetourVenteResponse {
    private Long id;
    private Long produitId;
    private String produitNom;
    private String produitCodeBarre;
    private Integer quantiteRetournee;
    private Double prixUnitaire;
    private Double sousTotal;
    private Long ligneVenteId;
}