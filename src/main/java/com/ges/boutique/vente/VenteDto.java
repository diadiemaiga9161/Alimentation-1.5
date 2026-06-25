package com.ges.boutique.vente;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VenteDto {
    private Long id;
    private String numeroVente;
    private Long vendeurId;
    private String vendeurNom;
    private VendeurDto vendeur;
    private Long clientId;
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;
    private Boolean clientDivers;
    private List<LigneVenteDto> lignes;
    private Double montantTotal;
    private Double montantRemiseTotal;
    private Double montantApresRemise;
    private Double remiseGlobale;
    private RemiseType typeRemiseGlobale;
    private ModePaiement modePaiement;
    private String referencePaiement;
    private LocalDateTime dateVente;
    private Boolean estCredit;
    private Double montantVerse;
    private Double montantRestant;
    private Boolean creditRegle;

    // Champs pour l'annulation et le retour
    private Boolean annulee;
    private String motifAnnulation;
    private LocalDateTime dateAnnulation;
    private Boolean estRetourne;
}