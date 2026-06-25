package com.ges.boutique.fournisseur;

import lombok.Data;

@Data
public class PaiementFournisseurRequest {
    private Long fournisseurId;
    private Double montant;
    private ModePaiementFournisseur modePaiement;
    private String reference;
    private String observation;
    private Long utilisateurId;
    private Long compteId;
    private Long achatCibleId;  // NOUVEAU: pour payer un achat spécifique
}