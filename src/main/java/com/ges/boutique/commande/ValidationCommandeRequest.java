package com.ges.boutique.commande;

import lombok.Data;

/**
 * Infos de livraison optionnelles saisies par le personnel au moment de valider une
 * commande (surtout utile pour celles venues de la vitrine, à livrer) — voir
 * Commande.adresseLivraison/fraisLivraison/chauffeurNom/chauffeurTelephone.
 */
@Data
public class ValidationCommandeRequest {
    private Double fraisLivraison;
    private String chauffeurNom;
    private String chauffeurTelephone;
}
