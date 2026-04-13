package com.ges.boutique.caisse;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CaisseRequest {
    private Double montant;
    private String motif;
    private Long utilisateurId;
    private String modePaiement;
    private String referencePaiement;
    private String clientNom;
    private String clientTelephone;
    private LocalDateTime dateEcheance;
}