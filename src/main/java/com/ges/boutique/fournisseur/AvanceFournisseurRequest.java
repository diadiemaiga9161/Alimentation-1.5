package com.ges.boutique.fournisseur;

import lombok.Data;

@Data
public class AvanceFournisseurRequest {
    private Long fournisseurId;
    private Double montant;
    private String motif;
    private String sourceFinancement; // CAISSE ou BANQUE
    private Long compteId; // requis si sourceFinancement = BANQUE
    private Long utilisateurId;
}
