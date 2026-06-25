package com.ges.boutique.inventaire;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.exception.StockInsuffisantException;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import com.ges.boutique.config.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventaireServiceImpl implements InventaireService {

    private final MouvementStockRepository mouvementStockRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;

    // ========== MÉTHODES EXISTANTES ==========

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public void entreeStock(Long produitId, Integer quantite, Long utilisateurId, String motif) {
        entreeStock(produitId, quantite, utilisateurId, motif, null);
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public void entreeStock(Long produitId, Integer quantite, Long utilisateurId, String motif, LocalDateTime dateMouvement) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        int ancienneQuantite = produit.getQuantite();
        int nouvelleQuantite = ancienneQuantite + quantite;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setQuantite(quantite);
        mouvement.setTypeMouvement(TypeMouvement.ENTREE);
        mouvement.setQuantiteAvant(ancienneQuantite);
        mouvement.setQuantiteApres(nouvelleQuantite);
        mouvement.setMotif(motif);
        mouvement.setDateMouvement(dateMouvement != null ? dateMouvement : LocalDateTime.now());

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(mouvement::setUtilisateur);
        }
        mouvementStockRepository.save(mouvement);
        notificationService.notifierMiseAJourStock(produit.getId(), produit.getNom(), nouvelleQuantite);
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
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
                ancienneQuantite, nouvelleQuantite, utilisateurId, motif, null, null, null);
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public void retourStock(Long produitId, Integer quantite, Long utilisateurId, String motif) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        int ancienneQuantite = produit.getQuantite();
        int nouvelleQuantite = ancienneQuantite + quantite;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        enregistrerMouvement(produit, quantite, TypeMouvement.RETOUR,
                ancienneQuantite, nouvelleQuantite, utilisateurId, motif, null, null, null);
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public void ajusterStock(Long produitId, Integer nouvelleQuantite, Long utilisateurId, String motif) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        int ancienneQuantite = produit.getQuantite();
        int difference = nouvelleQuantite - ancienneQuantite;
        TypeMouvement type = difference > 0 ? TypeMouvement.ENTREE : TypeMouvement.SORTIE;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        enregistrerMouvement(produit, Math.abs(difference), type,
                ancienneQuantite, nouvelleQuantite, utilisateurId, motif, null, null, null);
    }

    // ========== NOUVELLES MÉTHODES AVEC TRACABILITÉ ==========

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public void entreeStockAchat(Long produitId, Integer quantite, Long utilisateurId, Long achatId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        int ancienneQuantite = produit.getQuantite();
        int nouvelleQuantite = ancienneQuantite + quantite;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        String motif = "Achat fournisseur #" + achatId;
        enregistrerMouvement(produit, quantite, TypeMouvement.ENTREE,
                ancienneQuantite, nouvelleQuantite, utilisateurId, motif, achatId, "ACHAT", achatId);

        log.info("📦 Stock ENTRÉE: +{} x {} (Achat #{}) - Stock: {} → {}",
                quantite, produit.getNom(), achatId, ancienneQuantite, nouvelleQuantite);
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public void sortieStockAnnulationAchat(Long produitId, Integer quantite, Long utilisateurId, Long achatId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        int stockActuel = produit.getQuantite();
        int qteEffective = Math.min(quantite, stockActuel);

        if (qteEffective <= 0) {
            log.warn("Stock déjà à 0 pour {} — aucune sortie créée", produit.getNom());
            return;
        }

        int ancienneQuantite = produit.getQuantite();
        int nouvelleQuantite = ancienneQuantite - qteEffective;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        String motif = "Annulation achat fournisseur #" + achatId;
        enregistrerMouvement(produit, qteEffective, TypeMouvement.SORTIE,
                ancienneQuantite, nouvelleQuantite, utilisateurId, motif, achatId, "ANNULATION_ACHAT", achatId);

        log.info("📦 Stock SORTIE: -{} x {} (Annulation Achat #{}) - Stock: {} → {}",
                qteEffective, produit.getNom(), achatId, ancienneQuantite, nouvelleQuantite);
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public void sortieStockVente(Long produitId, Integer quantite, Long utilisateurId, Long venteId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        if (produit.getQuantite() < quantite) {
            throw new StockInsuffisantException("Stock insuffisant. Disponible: " + produit.getQuantite() + ", Demande: " + quantite);
        }

        int ancienneQuantite = produit.getQuantite();
        int nouvelleQuantite = ancienneQuantite - quantite;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        String motif = "Vente #" + venteId;
        enregistrerMouvement(produit, quantite, TypeMouvement.SORTIE,
                ancienneQuantite, nouvelleQuantite, utilisateurId, motif, null, "VENTE", venteId);

        log.info("📦 Stock SORTIE: -{} x {} (Vente #{}) - Stock: {} → {}",
                quantite, produit.getNom(), venteId, ancienneQuantite, nouvelleQuantite);
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public void entreeStockRetourVente(Long produitId, Integer quantite, Long utilisateurId, Long retourId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        int ancienneQuantite = produit.getQuantite();
        int nouvelleQuantite = ancienneQuantite + quantite;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        String motif = "Retour vente #" + retourId;
        enregistrerMouvement(produit, quantite, TypeMouvement.RETOUR,
                ancienneQuantite, nouvelleQuantite, utilisateurId, motif, null, "RETOUR_VENTE", retourId);

        log.info("📦 Stock RETOUR: +{} x {} (Retour Vente #{}) - Stock: {} → {}",
                quantite, produit.getNom(), retourId, ancienneQuantite, nouvelleQuantite);
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public void entreeStockBonusFournisseur(Long produitId, Integer quantite, Long objectifId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

        int ancienneQuantite = produit.getQuantite();
        int nouvelleQuantite = ancienneQuantite + quantite;

        produit.setQuantite(nouvelleQuantite);
        produitRepository.save(produit);

        String motif = "Bonus fournisseur - Objectif #" + objectifId;
        enregistrerMouvement(produit, quantite, TypeMouvement.BONUS_FOURNISSEUR,
                ancienneQuantite, nouvelleQuantite, null, motif, null, "OBJECTIF_FOURNISSEUR", objectifId);

        log.info("🎁 Stock BONUS_FOURNISSEUR: +{} x {} (Objectif #{}) - Stock: {} → {}",
                quantite, produit.getNom(), objectifId, ancienneQuantite, nouvelleQuantite);
    }

    // ========== MÉTHODE PRIVÉE AMÉLIORÉE ==========

    private void enregistrerMouvement(Produit produit, Integer quantite, TypeMouvement type,
                                      Integer quantiteAvant, Integer quantiteApres,
                                      Long utilisateurId, String motif, Long achatId,
                                      String referenceType, Long referenceId) {
        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setQuantite(quantite);
        mouvement.setTypeMouvement(type);
        mouvement.setQuantiteAvant(quantiteAvant);
        mouvement.setQuantiteApres(quantiteApres);
        mouvement.setMotif(motif);
        mouvement.setAchatId(achatId);
        mouvement.setReferenceType(referenceType);
        mouvement.setReferenceId(referenceId);

        if (utilisateurId != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId).orElse(null);
            mouvement.setUtilisateur(utilisateur);
        }

        mouvementStockRepository.save(mouvement);
        notificationService.notifierMiseAJourStock(produit.getId(), produit.getNom(), quantiteApres);
    }

    // ========== MÉTHODES DE CONSULTATION ==========

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

    @Override
    public List<MouvementStock> obtenirMouvementsParAchat(Long achatId) {
        return mouvementStockRepository.findByAchatId(achatId);
    }

    @Override
    public List<MouvementStock> obtenirTousMouvements() {
        return mouvementStockRepository.findAllByOrderByDateMouvementDesc();
    }
}