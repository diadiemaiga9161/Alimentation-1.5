package com.ges.boutique.objectif;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class StatsObjectifDto {
    private int mois;
    private int annee;
    private long totalObjectifs;
    private long objectifsAtteints;
    private long objectifsNonAtteints;
    private double totalBonusCalcule;
    private double totalQuantiteBonusRecue;
}
