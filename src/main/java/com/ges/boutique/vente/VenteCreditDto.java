package com.ges.boutique.vente;


import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VenteCreditDto {
    private Long id;
    private String numeroVente;
    private Long vendeurId;
    private String vendeurNom;
    private List<LigneVenteDto> lignes;
    private Double montantTotal;
    private Double montantRemiseTotal;
    private Double montantApresRemise;
    private Double remiseGlobale;
    private RemiseType typeRemiseGlobale;
    private ModePaiement modePaiement;
    private String referencePaiement;
    private LocalDateTime dateVente;

    // Champs spécifiques au crédit
    private String clientNom;
    private String clientTelephone;
    private LocalDate dateEcheance;
    private Double montantRestant;
    private Double montantVerse;
    private LocalDate dateReglement;
    private boolean creditRegle;
    private boolean creditEnRetard;
    private Integer joursDeRetard;
}