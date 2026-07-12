package com.ges.boutique.promo;

import lombok.Data;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

@Data
public class PromotionRequest {
    private String titre;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String typeReduction; // POURCENTAGE ou MONTANT_FIXE
    private Double valeurReduction;
    private Boolean active;
    private Boolean globale = false;
    private List<Long> produitIds = new ArrayList<>();
}
