package com.ges.boutique.produit;

import lombok.Data;

/**
 * Requête de création/modification d'une unité de vente. L'admin fournit soit
 * facteurBase directement, soit (uniteReferenceId + facteurRelatif) pour laisser le
 * serveur calculer le facteur total — ex: uniteReferenceId = id de "Cartouche" (déjà
 * enregistrée à 10 Pièces) + facteurRelatif = 5 => facteurBase calculé = 50.
 * uniteReferenceId = null => facteurRelatif est relatif à l'unité de base elle-même.
 */
@Data
public class UniteVenteRequest {
    private String nom;
    private Double prixVente;
    private Double prixAchat;
    private Integer ordre;

    private Integer facteurBase;
    private Long uniteReferenceId;
    private Integer facteurRelatif;
}
