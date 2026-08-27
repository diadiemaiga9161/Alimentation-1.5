package com.ges.boutique.transfert;

public enum StatutTransfert {
    CREE,
    EN_ATTENTE_CONFIRMATION,
    EN_ATTENTE,
    CONFIRME,
    ACCEPTE,
    REJETE,
    COMPLETE,
    ANNULE,
    /** La notification vers la boutique de destination a échoué (URL injoignable, clé invalide, etc.) —
     *  le stock a bien été retiré côté source, mais la destination n'a jamais reçu le transfert. */
    ECHEC_NOTIFICATION
}
