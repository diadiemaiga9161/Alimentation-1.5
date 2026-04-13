package com.ges.boutique.rapport;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class RapportHebdomadaire {
    private LocalDate debutSemaine;
    private LocalDate finSemaine;
    private Double chiffreAffaireTotal;
    private Integer nombreVentes;
    private Double moyenneJournaliere;
    private Double croissanceParRapportSemainePrecedente;
    private Map<String, Object> meilleursProduits;
}