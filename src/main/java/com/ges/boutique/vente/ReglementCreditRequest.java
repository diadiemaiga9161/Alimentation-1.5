package com.ges.boutique.vente;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReglementCreditRequest {
    private Long venteId;
    private Double montantRegle;
    private Long utilisateurId;
    private String modePaiement;
    private String referencePaiement;
    private LocalDate dateReglement;
}