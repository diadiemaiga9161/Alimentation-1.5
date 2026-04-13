package com.ges.boutique.rapport;

import lombok.Data;

import java.util.Map;

@Data
public class RapportMensuel {
    private String mois;
    private Integer annee;
    private Double chiffreAffaireTotal;
    private Integer nombreVentes;
    private Double moyenneJournaliere;
    private Double valeurStockTotale;
    private Map<String, Object> tendances;
}