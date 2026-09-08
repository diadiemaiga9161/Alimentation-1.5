package com.ges.boutique.fidelite;

import com.ges.boutique.client.Client;

import java.util.List;

public interface FideliteService {

    /**
     * Crédite les points gagnés sur une vente (montant / montantParPoint, arrondi au
     * point inférieur). Ne fait rien si le programme est désactivé pour la boutique, si
     * le client est null (vente "client divers") ou si le montant ne rapporte aucun
     * point entier — appelé systématiquement depuis VenteServiceImpl, jamais exposé en
     * endpoint direct.
     */
    void gagnerPoints(Client client, Long venteId, double montantVente);

    /**
     * Débite des points du solde du client pour les convertir en réduction (déjà
     * appliquée côté appelant via une remise classique — ceci ne fait que la
     * comptabilité des points, aucun montant n'est recalculé ici).
     * Lève IllegalArgumentException si le solde est insuffisant.
     */
    void utiliserPoints(Long clientId, int points, Long venteId, String motif);

    /** Ajustement manuel par l'admin (points positifs ou négatifs), hors vente. */
    void ajusterManuel(Long clientId, int delta, String motif);

    /** Annule tous les mouvements (gagnés ou utilisés) liés à une vente annulée — restaure le solde correspondant. */
    void annulerMouvementsPourVente(Long venteId);

    FideliteSoldeDto obtenirSolde(Long clientId);

    List<MouvementFidelite> obtenirHistorique(Long clientId);

    FideliteParametresDto obtenirParametres();

    void definirParametres(FideliteParametresDto parametres);
}
