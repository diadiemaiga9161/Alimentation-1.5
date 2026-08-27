package com.ges.boutique.journalaudit;

/**
 * Liste FERMÉE des actions sensibles tracées dans le journal d'audit.
 * N'ajouter une valeur ici que si un nouveau point d'instrumentation est
 * explicitement demandé — ne pas étendre cette liste "au cas où".
 */
public enum TypeActionAudit {
    SUPPRESSION_VENTE,
    MODIFICATION_PRIX_PRODUIT,
    ANNULATION_TRANSFERT,
    SUPPRESSION_TRANSFERT,
    SUPPRESSION_CLIENT,
    SUPPRESSION_FOURNISSEUR,
    MODIFICATION_ROLE_UTILISATEUR,
    SUPPRESSION_CREDIT
}
