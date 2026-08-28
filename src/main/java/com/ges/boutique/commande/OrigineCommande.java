package com.ges.boutique.commande;

/**
 * D'où vient la commande : prise en magasin par un vendeur (comportement historique,
 * inchangé), ou déposée par un client depuis la vitrine publique (sans connexion).
 */
public enum OrigineCommande {
    MAGASIN,
    VITRINE
}
