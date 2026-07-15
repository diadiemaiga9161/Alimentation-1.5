package com.ges.boutique.rapport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrevisionStockDTO {
    private Long produitId;
    private String produitNom;
    private int stockActuel;
    private double velociteJournaliere;
    private int joursAvantRupture;
    private int quantiteRecommandee;
    private LocalDate datePrevueRupture;
    private String urgence; // CRITIQUE, ATTENTION, OK
}
