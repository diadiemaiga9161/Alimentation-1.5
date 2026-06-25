package com.ges.boutique.commande;

import java.util.List;

public interface CommandeService {
    Commande creer(CommandeRequest request);
    Commande modifier(Long id, CommandeRequest request);
    Commande valider(Long id);
    void supprimer(Long id);
    Commande findById(Long id);
    List<Commande> findAll();
    List<Commande> findByStatut(StatutCommande statut);
}
