package com.ges.boutique.commande;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findAllByOrderByDateCommandeDesc();
    List<Commande> findByStatutOrderByDateCommandeDesc(StatutCommande statut);
    List<Commande> findByClient_IdOrderByDateCommandeDesc(Long clientId);
    List<Commande> findByOrigineAndStatutOrderByDateCommandeDesc(OrigineCommande origine, StatutCommande statut);
}
