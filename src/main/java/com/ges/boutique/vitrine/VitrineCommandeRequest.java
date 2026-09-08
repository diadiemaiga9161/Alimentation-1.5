package com.ges.boutique.vitrine;

import lombok.Data;

import java.util.List;

/**
 * Requête publique (sans authentification) envoyée par un client depuis la vitrine
 * pour passer commande. Volontairement minimale — pas d'ID vendeur, pas de mode de
 * paiement (pas de paiement en ligne pour l'instant, cf. VitrineController).
 */
@Data
public class VitrineCommandeRequest {
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;
    private String adresseLivraison;
    private String notes;
    private List<Ligne> lignes;

    @Data
    public static class Ligne {
        private Long produitId;
        private Integer quantite;
    }
}
