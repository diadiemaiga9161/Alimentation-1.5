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
    private VendeurDto vendeur; // Nouveau champ
    private List<LigneVenteDto> lignes;
    private Double montantTotal;
    private Double montantRemiseTotal;
    private Double montantApresRemise;
    private Double remiseGlobale;
    private RemiseType typeRemiseGlobale;
    private ModePaiement modePaiement;
    private String referencePaiement;
    private LocalDateTime dateVente;
}