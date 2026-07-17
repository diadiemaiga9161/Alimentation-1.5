package com.ges.boutique.transfert;

import lombok.Data;

@Data
public class PaiementTransfertRequest {
    private Double montant;
    private String modePaiement;
    private String notes;
}
