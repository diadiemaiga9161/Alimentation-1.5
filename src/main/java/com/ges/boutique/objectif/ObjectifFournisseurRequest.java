package com.ges.boutique.objectif;

import lombok.Data;

@Data
public class ObjectifFournisseurRequest {
    private Long fournisseurId;
    private Long produitId;
    private Integer mois;
    private Integer annee;
    private Double objectifQuantite;
    private Double bonusParUnite;
    private Double quantiteAtteinte;
    private Double quantiteBonusRecue;
    private String observation;
}
