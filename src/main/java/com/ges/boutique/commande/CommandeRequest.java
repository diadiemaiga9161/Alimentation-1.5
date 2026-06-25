package com.ges.boutique.commande;

import com.ges.boutique.vente.ModePaiement;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CommandeRequest {
    private Long vendeurId;
    private Long clientId;
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;
    private List<LigneCommandeRequest> lignes;
    private ModePaiement modePaiement;
    private String referencePaiement;
    private Boolean estCredit = false;
    private Double montantVerse = 0.0;
    private LocalDate dateEcheance;
    private String notes;
}
