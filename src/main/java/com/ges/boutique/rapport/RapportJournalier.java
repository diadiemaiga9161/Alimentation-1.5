package com.ges.boutique.rapport;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class RapportJournalier {
    private LocalDate date;
    private Double chiffreAffaireTotal;
    private Integer nombreVentes;
    private Map<String, Double> chiffreAffaireParModePaiement;
    private Integer produitsStockFaible;
    private Double valeurStockTotale;
}