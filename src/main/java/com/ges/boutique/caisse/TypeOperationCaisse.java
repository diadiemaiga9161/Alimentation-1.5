package com.ges.boutique.caisse;

public enum TypeOperationCaisse {
    OUVERTURE,
    FERMETURE,
    VENTE_COMPTANT,
    VENTE_CREDIT,
    REGLEMENT_CREDIT,
    SORTIE,
    ENTREE,
    AJUSTEMENT,
    VERIFICATION,
    DEPOT,
    RETRAIT,
    ANNULATION_VENTE,
    ANNULATION_CREDIT,
    AVANCE_CLIENT,
    VIREMENT_BANQUE,
    PAIEMENT_FOURNISSEUR,         // Remplacera SORTIE pour les paiements fournisseurs
    AVANCE_FOURNISSEUR,           // Remplacera SORTIE pour les avances fournisseurs
    PAIEMENT_EMPLOYE,             // Paiement salaire employé (hors calcul sorties normales)
    ANNULATION_PAIEMENT_EMPLOYE,
    REMBOURSEMENT_RETOUR,
    DEPENSE
}