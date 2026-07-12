package com.ges.boutique.caisse;

import lombok.Data;

@Data
public class TransfertCaisseBanqueRequest {
    private Long compteId;
    private Double montant;
    private String motif;
    private Long utilisateurId;
    private String reference;
}