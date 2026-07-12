package com.ges.boutique.bonus;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BonusFournisseurRequest {
    private Long fournisseurId;
    private TypeBonus type;
    private Double montant;
    private Long produitId;
    private Double quantiteProduit;
    private LocalDate date;
    private String description;
}
