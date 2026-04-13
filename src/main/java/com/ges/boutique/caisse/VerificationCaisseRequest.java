package com.ges.boutique.caisse;

import lombok.Data;

@Data
public class VerificationCaisseRequest {
    private Double soldeReelSaisi;
    private Long utilisateurId;
    private String observations;
}