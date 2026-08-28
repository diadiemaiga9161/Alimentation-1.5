package com.ges.boutique.commande;

import com.ges.boutique.vitrine.VitrineCommandeRequest;

import java.util.List;

public interface CommandeService {
    Commande creer(CommandeRequest request);
    Commande modifier(Long id, CommandeRequest request);
    Commande valider(Long id, Long currentUserId);
    void supprimer(Long id);
    Commande findById(Long id);
    List<Commande> findAll();
    List<Commande> findByStatut(StatutCommande statut);
    Commande payerCredit(Long id, Double montant);
    List<Commande> payerCreditsGroupes(List<Long> ids, Double montantTotal);
    Commande annuler(Long id, Long utilisateurId);

    /** Commande publique déposée depuis la vitrine (sans connexion) — cf. VitrineController. */
    Commande creerDepuisVitrine(VitrineCommandeRequest request);

    /** Commandes vitrine pas encore traitées — pour le badge/popup "commandes en attente". */
    List<Commande> trouverVitrineEnAttente();
}
