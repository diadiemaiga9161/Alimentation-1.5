package com.ges.boutique.permission;

/**
 * Permissions accordables au rôle VENDEUR par l'ADMIN normal de la boutique (décision
 * opérationnelle propre à chaque boutique — sans rapport avec le super admin ni le
 * système de fonctionnalités avancées, voir com.ges.boutique.feature). Chaque clé
 * élargit l'accès du vendeur à une consultation précise, jamais à une action d'écriture.
 */
public enum CleVendeur {
    INVENTAIRE_LECTURE("Consultation de l'inventaire");

    private final String libelle;

    CleVendeur(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
