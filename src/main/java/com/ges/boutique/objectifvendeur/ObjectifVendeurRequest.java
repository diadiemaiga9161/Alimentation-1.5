package com.ges.boutique.objectifvendeur;

import lombok.Data;

@Data
public class ObjectifVendeurRequest {
    private Long vendeurId;
    private Integer semaine;
    private Integer annee;
    private Integer objectifNombreVentes;
    private Double bonusMontant;
    private String observation;
}
