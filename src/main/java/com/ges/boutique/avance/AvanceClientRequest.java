package com.ges.boutique.avance;

import lombok.Data;

@Data
public class AvanceClientRequest {
    private String clientNom;
    private String clientTelephone;
    private Double montant;
    private String motif;
    private Long utilisateurId;
    private String modePaiement;
    private String referencePaiement;
}
