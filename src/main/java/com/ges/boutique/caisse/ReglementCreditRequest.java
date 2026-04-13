package com.ges.boutique.caisse;

import lombok.Data;

@Data
public class ReglementCreditRequest {
    private Long venteCreditId;
    private Double montantRegle;
    private Long utilisateurId;
    private String modePaiement;
    private String referencePaiement;
    private String dateReglement;
}