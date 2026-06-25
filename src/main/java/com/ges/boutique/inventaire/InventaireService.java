package com.ges.boutique.inventaire;

import com.ges.boutique.produit.Produit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InventaireService {

    void entreeStock(Long produitId, Integer quantite, Long utilisateurId, String motif);
    void entreeStock(Long produitId, Integer quantite, Long utilisateurId, String motif, LocalDateTime dateMouvement);
    void sortieStock(Long produitId, Integer quantite, Long utilisateurId, String motif);
    void ajusterStock(Long produitId, Integer nouvelleQuantite, Long utilisateurId, String motif);
    void retourStock(Long produitId, Integer quantite, Long utilisateurId, String motif);

    // ========== NOUVELLES MÉTHODES AVEC TRACABILITÉ ==========
    void entreeStockAchat(Long produitId, Integer quantite, Long utilisateurId, Long achatId);
    void sortieStockAnnulationAchat(Long produitId, Integer quantite, Long utilisateurId, Long achatId);
    void sortieStockVente(Long produitId, Integer quantite, Long utilisateurId, Long venteId);
    void entreeStockRetourVente(Long produitId, Integer quantite, Long utilisateurId, Long retourId);
    // ========================================================

    void entreeStockBonusFournisseur(Long produitId, Integer quantite, Long objectifId);

    List<MouvementStock> obtenirHistoriqueProduit(Long produitId);
    List<MouvementStock> obtenirMouvementsParDate(LocalDateTime debut, LocalDateTime fin);
    List<Produit> obtenirProduitsStockFaible();
    Map<String, Object> obtenirStatistiquesInventaire();
    List<MouvementStock> obtenirMouvementsParAchat(Long achatId);
    List<MouvementStock> obtenirTousMouvements();
}