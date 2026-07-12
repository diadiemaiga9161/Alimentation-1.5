package com.ges.boutique.depense;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DepenseRequest {
    private String nom;
    private String motif;
    private LocalDate date;
    private Double montant;
    private String typeDepense;
}
