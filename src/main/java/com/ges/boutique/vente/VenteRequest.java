package com.ges.boutique.vente;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class VenteRequest {
    private Long vendeurId;
    private List<LigneVenteRequest> lignes;
    private ModePaiement modePaiement;
    private String referencePaiement;
    private Double remiseGlobale;
    private RemiseType typeRemiseGlobale;

    // NOUVEAUX CHAMPS POUR LES CRÉDITS
    private boolean estCredit = false;
    private String clientNom;
    private String clientTelephone;
    private LocalDate dateEcheance;
    private Double montantVerse;
    private Long creditId; // Pour modification de crédit
}