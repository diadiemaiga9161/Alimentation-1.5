package com.ges.boutique.compte;

import lombok.Data;

@Data
public class OperationCompteRequest {
    private Long compteId;
    private TypeOperationCompte type;
    private Double montant;
    private String motif;
    private String reference;
    private Long utilisateurId;
}
