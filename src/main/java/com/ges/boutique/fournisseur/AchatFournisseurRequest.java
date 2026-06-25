package com.ges.boutique.fournisseur;

import lombok.Data;
import java.util.List;

@Data
public class AchatFournisseurRequest {
    private Long fournisseurId;
    private FournisseurRequest nouveauFournisseur;
    private List<LigneAchatRequest> lignes;
    private Double montantPaye;
    private Double montantAvanceUtilise;
    private String commentaire;
    private Long utilisateurId;
    private String modePaiementImmediat;
    private Long compteIdPaiement;
}