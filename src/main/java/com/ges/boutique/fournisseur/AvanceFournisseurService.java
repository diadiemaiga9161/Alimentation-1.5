package com.ges.boutique.fournisseur;

import java.util.List;

public interface AvanceFournisseurService {
    AvanceFournisseur enregistrerAvance(AvanceFournisseurRequest request);
    Double getSoldeDisponible(Long fournisseurId);
    List<AvanceFournisseur> getHistoriqueParFournisseur(Long fournisseurId);
    List<AvanceFournisseur> getToutesLesAvances();
    void utiliserAvance(Long fournisseurId, Double montantAUtiliser);
    void annulerUtilisationAvance(Long fournisseurId, Double montantAAnnuler);

    /**
     * Crédite un trop-perçu (excédent de paiement fournisseur non applicable à une dette
     * existante) directement en avance disponible, SANS débiter la caisse/banque — contrairement
     * à enregistrerAvance(), l'argent est déjà sorti de la caisse au moment du paiement fournisseur
     * d'origine. Utilisé uniquement quand un paiement ciblé (achatCibleId) dépasse la dette totale
     * du fournisseur (tous ses achats non payés confondus).
     */
    AvanceFournisseur crediterExcedentPaiement(Long fournisseurId, Double montant, String motif, Long utilisateurId);
}