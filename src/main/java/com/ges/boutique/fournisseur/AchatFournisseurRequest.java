package com.ges.boutique.fournisseur;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AchatFournisseurRequest {
    private Long fournisseurId;      // optionnel, si null on crée un nouveau fournisseur
    private FournisseurRequest nouveauFournisseur; // utilisé si fournisseurId null
    private List<LigneAchatRequest> lignes;
    private Double montantPaye;      // ce qui est payé immédiatement
    private String commentaire;
    private Long utilisateurId;
}