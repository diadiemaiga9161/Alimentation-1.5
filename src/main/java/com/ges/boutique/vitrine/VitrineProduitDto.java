package com.ges.boutique.vitrine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO public et minimal pour la vitrine (mini-site vitrine automatique).
 * ATTENTION SÉCURITÉ : ce DTO est exposé SANS authentification.
 * N'y ajouter QUE des champs destinés à être visibles par n'importe quel visiteur
 * (jamais prixAchat, fournisseur, quantité exacte, seuilAlerte, codeBarre, lotNumber...).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VitrineProduitDto {

    private Long id;
    private String nom;
    private String categorieNom;
    private Double prixVente;
    private boolean disponible;

    private boolean enPromotion;
    private String promotionTitre;
    private String promotionReduction;
}
