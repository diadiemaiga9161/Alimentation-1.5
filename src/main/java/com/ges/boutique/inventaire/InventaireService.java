package com.ges.boutique.inventaire;

import com.ges.boutique.produit.Produit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InventaireService {

    void entreeStock(Long produitId, Integer quantite, Long utilisateurId, String motif);
    void sortieStock(Long produitId, Integer quantite, Long utilisateurId, String motif);
    void ajusterStock(Long produitId, Integer nouvelleQuantite, Long utilisateurId, String motif);

    List<MouvementStock> obtenirHistoriqueProduit(Long produitId);
    List<MouvementStock> obtenirMouvementsParDate(LocalDateTime debut, LocalDateTime fin);
    List<Produit> obtenirProduitsStockFaible();

    Map<String, Object> obtenirStatistiquesInventaire();
}