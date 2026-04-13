package com.ges.boutique.caisse;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationCaisseDto {
    private Long id;
    private String type;
    private String typeLibelle;
    private Double montant;
    private Double soldeAvant;
    private Double soldeApres;
    private String motif;
    private Long utilisateurId;
    private String utilisateurNom;
    private Long venteId;
    private String numeroVente;
    private String modePaiement;
    private String modePaiementLibelle;
    private String referencePaiement;
    private String clientNom;
    private String clientTelephone;
    private LocalDateTime dateOperation;
    private boolean estReglee;
    private LocalDateTime dateEcheance;
    private Double montantVerse;
    private Double montantRestant;
    private Long venteCreditId;
    private String numeroCredit;
    private String periode;
    private boolean enRetard;
    private Long joursRetard;
}
