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

    private Boolean estCredit = false;

    // Client
    private Long clientId;
    private Boolean clientDivers = false;
    private Boolean creerClient = false;
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;
    private String clientEmail;
    private String clientAdresse;

    // Pour les crédits
    private LocalDate dateEcheance;
    private Double montantVerse;
    private Double montantAvanceUtilise = 0.0;

    // Idempotence - renseigné par le contrôleur depuis le header X-Client-Request-ID
    private String clientRequestId;
}