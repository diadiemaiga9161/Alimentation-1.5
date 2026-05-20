package com.ges.boutique.fournisseur;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AchatFournisseurRequest {
    private Long fournisseurId;
    private FournisseurRequest nouveauFournisseur;
    private List<LigneAchatRequest> lignes;
    private Double montantPaye;            // paiement immédiat en espèces
    private Double montantAvanceUtilise;   // montant déduit de l'avance fournisseur (déjà payé)
    private String commentaire;
    private Long utilisateurId;
}