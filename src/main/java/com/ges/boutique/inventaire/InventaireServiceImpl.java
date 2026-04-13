package com.ges.boutique.inventaire;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.exception.StockInsuffisantException;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventaireServiceImpl implements InventaireService {

    private final MouvementStockRepository mouvementStockRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional
    public void entreeStock(Long produitId, Integer quantite, Long utilisateurId, String motif) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        int ancienneQuantite = produit.getQuantite();
        int nouvelleQuantite = ancienneQuantite + quantite;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        enregistrerMouvement(produit, quantite, TypeMouvement.ENTREE,
                ancienneQuantite, nouvelleQuantite, utilisateurId, motif);
    }

    @Override
    @Transactional
    public void sortieStock(Long produitId, Integer quantite, Long utilisateurId, String motif) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        if (produit.getQuantite() < quantite) {
            throw new StockInsuffisantException("Stock insuffisant. Disponible: " + produit.getQuantite() + ", Demande: " + quantite);
        }

        int ancienneQuantite = produit.getQuantite();
        int nouvelleQuantite = ancienneQuantite - quantite;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        enregistrerMouvement(produit, quantite, TypeMouvement.SORTIE,
                ancienneQuantite, nouvelleQuantite, utilisateurId, motif);
    }

    @Override
    @Transactional
    public void ajusterStock(Long produitId, Integer nouvelleQuantite, Long utilisateurId, String motif) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        int ancienneQuantite = produit.getQuantite();
        int difference = nouvelleQuantite - ancienneQuantite;
        TypeMouvement type = difference > 0 ? TypeMouvement.ENTREE : TypeMouvement.SORTIE;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        enregistrerMouvement(produit, Math.abs(difference), type,
                ancienneQuantite, nouvelleQuantite, utilisateurId, motif);
    }

    private void enregistrerMouvement(Produit produit, Integer quantite, TypeMouvement type,
                                      Integer quantiteAvant, Integer quantiteApres,
                                      Long utilisateurId, String motif) {
        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setQuantite(quantite);
        mouvement.setTypeMouvement(type);
        mouvement.setQuantiteAvant(quantiteAvant);
        mouvement.setQuantiteApres(quantiteApres);
        mouvement.setMotif(motif);

        if (utilisateurId != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                    .orElse(null);
            mouvement.setUtilisateur(utilisateur);
        }

        mouvementStockRepository.save(mouvement);
    }

    @Override
    public List<MouvementStock> obtenirHistoriqueProduit(Long produitId) {
        return mouvementStockRepository.findByProduitId(produitId);
    }

    @Override
    public List<MouvementStock> obtenirMouvementsParDate(LocalDateTime debut, LocalDateTime fin) {
        return mouvementStockRepository.findByDateMouvementBetween(debut, fin);
    }

    @Override
    public List<Produit> obtenirProduitsStockFaible() {
        return produitRepository.trouverProduitsStockFaible();
    }

    @Override
    public Map<String, Object> obtenirStatistiquesInventaire() {
        Map<String, Object> stats = new HashMap<>();

        List<Produit> produitsStockFaible = obtenirProduitsStockFaible();
        List<Produit> produitsRupture = produitRepository.trouverProduitsEnRupture();
        Double valeurTotale = produitRepository.getValeurTotaleStock();

        long totalEntrees = mouvementStockRepository.findByTypeMouvement(TypeMouvement.ENTREE).stream()
                .mapToLong(MouvementStock::getQuantite)
                .sum();

        long totalSorties = mouvementStockRepository.findByTypeMouvement(TypeMouvement.SORTIE).stream()
                .mapToLong(MouvementStock::getQuantite)
                .sum();

        stats.put("produitsStockFaible", produitsStockFaible.size());
        stats.put("produitsRupture", produitsRupture.size());
        stats.put("valeurTotaleStock", valeurTotale != null ? valeurTotale : 0);
        stats.put("totalEntrees", totalEntrees);
        stats.put("totalSorties", totalSorties);
        stats.put("variationNet", totalEntrees - totalSorties);

        return stats;
    }
}