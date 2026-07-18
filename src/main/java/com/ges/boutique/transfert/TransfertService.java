package com.ges.boutique.transfert;

import com.ges.boutique.boutique.Boutique;
import com.ges.boutique.boutique.BoutiqueRepository;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.inventaire.MouvementStock;
import com.ges.boutique.inventaire.MouvementStockRepository;
import com.ges.boutique.inventaire.TypeMouvement;
import com.ges.boutique.notification.NotificationPersistanceService;
import com.ges.boutique.websocket.StockWebSocketService;
import com.ges.boutique.produit.Categorie;
import com.ges.boutique.produit.CategorieRepository;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransfertService {

    private final TransfertRepository transfertRepository;
    private final BoutiquePartenaireRepository partenaireRepository;
    private final ProduitRepository produitRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final NotificationPersistanceService notifService;
    private final RestTemplate restTemplate;
    private final MouvementStockRepository mouvementStockRepository;
    private final CategorieRepository categorieRepository;
    private final PaiementTransfertRepository paiementTransfertRepository;
    private final StockWebSocketService stockWebSocketService;

    private static final Long BOUTIQUE_ID = 1L;

    @Value("${transfert.service.key:}")
    private String transfertServiceKey;

    // ==================== PARTENAIRES ====================

    public List<BoutiquePartenaire> getPartenaires() {
        return partenaireRepository.findAll();
    }

    @Transactional
    public BoutiquePartenaire ajouterPartenaire(BoutiquePartenaire p) {
        return partenaireRepository.save(p);
    }

    @Transactional
    public BoutiquePartenaire modifierPartenaire(Long id, BoutiquePartenaire p) {
        BoutiquePartenaire existing = partenaireRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Partenaire introuvable: " + id));
        existing.setNom(p.getNom());
        existing.setUrl(p.getUrl());
        existing.setDescription(p.getDescription());
        existing.setActif(p.isActif());
        return partenaireRepository.save(existing);
    }

    @Transactional
    public void supprimerPartenaire(Long id) {
        partenaireRepository.deleteById(id);
    }

    @SuppressWarnings("unchecked")
    public List<Object> getProduitsBoutique(Long partenaireId) {
        BoutiquePartenaire partenaire = partenaireRepository.findById(partenaireId)
                .orElseThrow(() -> new RessourceIntrouvableException("Partenaire introuvable: " + partenaireId));
        try {
            String url = partenaire.getUrl().stripTrailing() + "/api/produits";
            HttpHeaders headers = new HttpHeaders();
            if (!transfertServiceKey.isBlank()) {
                headers.set("X-Service-Key", transfertServiceKey);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List> resp = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            return resp.getBody() != null ? resp.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Impossible de récupérer les produits de {}: {}", partenaire.getNom(), e.getMessage());
            return List.of();
        }
    }

    // ==================== TRANSFERTS ====================

    public List<TransfertStock> getTout() {
        return transfertRepository.findAllByOrderByDateCreationDesc();
    }

    public TransfertStock getById(Long id) {
        return transfertRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Transfert introuvable: " + id));
    }

    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public TransfertStock creer(TransfertRequest req, String currentUser) {
        BoutiquePartenaire dest = partenaireRepository.findById(req.getBoutiqueDestId())
                .orElseThrow(() -> new RessourceIntrouvableException("Boutique destination introuvable"));

        Boutique sourceBoutique = boutiqueRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Boutique source introuvable"));

        TransfertStock t = new TransfertStock();
        t.setBoutiqueSourceNom(sourceBoutique.getNom());
        t.setBoutiqueSourceUrl("");
        t.setBoutiqueDestNom(dest.getNom());
        t.setBoutiqueDestUrl(dest.getUrl());
        t.setStatut(StatutTransfert.CREE);
        t.setTypePaiement(TypePaiementTransfert.valueOf(
                req.getTypePaiement() != null ? req.getTypePaiement() : "SANS_PAIEMENT"));
        t.setNotes(req.getNotes());
        t.setCreePar(currentUser);

        for (TransfertRequest.LigneRequest lr : req.getLignes()) {
            LigneTransfert l = new LigneTransfert();
            l.setTransfert(t);
            l.setProduitId(lr.getProduitId());
            l.setProduitNom(lr.getProduitNom());
            l.setQuantite(lr.getQuantite());
            l.setPrixUnitaire(lr.getPrixUnitaire());
            t.getLignes().add(l);
        }

        t.getHistorique().add(HistoriqueTransfert.creer(t, "CREATION",
                "Transfert créé par " + currentUser, currentUser));

        TransfertStock saved = transfertRepository.save(t);

        // Déduire le stock côté source
        deduireStock(saved);

        // Notifier la boutique destination
        notifierDestination(saved, dest.getUrl());

        // Notification interne
        notifService.creer("TRANSFERT_ENVOYE", "Transfert envoyé",
                "Transfert " + saved.getNumeroTransfert() + " → " + dest.getNom(), "/pages/transferts");

        // Notification WebSocket temps réel
        stockWebSocketService.diffuserTransfert(BOUTIQUE_ID, Map.of(
                "type", "TRANSFERT_ENVOYE",
                "numeroTransfert", saved.getNumeroTransfert(),
                "destination", dest.getNom(),
                "timestamp", System.currentTimeMillis()
        ));

        return saved;
    }

    @Transactional
    public TransfertStock modifier(Long id, TransfertRequest req, String currentUser) {
        TransfertStock t = getById(id);
        if (t.getStatut() == StatutTransfert.CONFIRME) {
            throw new IllegalStateException("Un transfert confirmé ne peut plus être modifié");
        }
        if (t.getStatut() == StatutTransfert.ANNULE) {
            throw new IllegalStateException("Un transfert annulé ne peut plus être modifié");
        }

        // Remettre le stock avant modification
        restaurerStock(t);

        t.setNotes(req.getNotes());
        if (req.getTypePaiement() != null) {
            t.setTypePaiement(TypePaiementTransfert.valueOf(req.getTypePaiement()));
        }

        t.getLignes().clear();
        for (TransfertRequest.LigneRequest lr : req.getLignes()) {
            LigneTransfert l = new LigneTransfert();
            l.setTransfert(t);
            l.setProduitId(lr.getProduitId());
            l.setProduitNom(lr.getProduitNom());
            l.setQuantite(lr.getQuantite());
            l.setPrixUnitaire(lr.getPrixUnitaire());
            t.getLignes().add(l);
        }

        t.setStatut(StatutTransfert.EN_ATTENTE_CONFIRMATION);
        t.getHistorique().add(HistoriqueTransfert.creer(t, "MODIFICATION",
                "Modifié par " + currentUser, currentUser));

        TransfertStock saved = transfertRepository.save(t);
        deduireStock(saved);

        notifService.creer("TRANSFERT_MODIFIE", "Transfert modifié",
                "Transfert " + saved.getNumeroTransfert() + " a été modifié", "/pages/transferts");

        return saved;
    }

    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public TransfertStock confirmer(Long id, String currentUser) {
        TransfertStock t = getById(id);
        if (t.getStatut() == StatutTransfert.CONFIRME) {
            throw new IllegalStateException("Déjà confirmé");
        }
        if (t.getStatut() == StatutTransfert.ANNULE) {
            throw new IllegalStateException("Annulé, impossible de confirmer");
        }

        t.setStatut(StatutTransfert.CONFIRME);
        t.setDateConfirmation(LocalDateTime.now());
        t.setConfirmeParUser(currentUser);
        t.getHistorique().add(HistoriqueTransfert.creer(t, "CONFIRMATION",
                "Confirmé définitivement par " + currentUser, currentUser));

        TransfertStock saved = transfertRepository.save(t);

        notifService.creer("TRANSFERT_CONFIRME", "Transfert confirmé",
                "Transfert " + saved.getNumeroTransfert() + " confirmé par " + currentUser, "/pages/transferts");

        return saved;
    }

    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public TransfertStock annuler(Long id, String motif, String currentUser) {
        TransfertStock t = getById(id);
        if (t.getStatut() == StatutTransfert.CONFIRME) {
            throw new IllegalStateException("Un transfert confirmé ne peut pas être annulé");
        }

        restaurerStock(t);
        t.setStatut(StatutTransfert.ANNULE);
        t.getHistorique().add(HistoriqueTransfert.creer(t, "ANNULATION",
                "Annulé par " + currentUser + (motif != null ? " — " + motif : ""), currentUser));

        return transfertRepository.save(t);
    }

    // Endpoint appelé par une autre boutique pour recevoir un transfert
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public TransfertStock recevoir(Map<String, Object> payload, String receivedKey) {
        if (!transfertServiceKey.isBlank() && !transfertServiceKey.equals(receivedKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Clé de service invalide");
        }
        TransfertStock t = new TransfertStock();
        t.setBoutiqueSourceNom((String) payload.getOrDefault("boutiqueSourceNom", "Inconnue"));
        t.setBoutiqueSourceUrl((String) payload.getOrDefault("boutiqueSourceUrl", ""));
        t.setBoutiqueDestNom((String) payload.getOrDefault("boutiqueDestNom", "Cette boutique"));
        t.setBoutiqueDestUrl("");
        t.setStatut(StatutTransfert.EN_ATTENTE_CONFIRMATION);
        t.setTypePaiement(TypePaiementTransfert.valueOf(
                (String) payload.getOrDefault("typePaiement", "SANS_PAIEMENT")));
        t.setNotes((String) payload.get("notes"));
        t.setCreePar((String) payload.getOrDefault("creePar", "Boutique distante"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lignesData = (List<Map<String, Object>>) payload.get("lignes");
        if (lignesData != null) {
            for (Map<String, Object> ld : lignesData) {
                LigneTransfert l = new LigneTransfert();
                l.setTransfert(t);
                l.setProduitId(((Number) ld.get("produitId")).longValue());
                l.setProduitNom((String) ld.get("produitNom"));
                l.setQuantite(((Number) ld.get("quantite")).intValue());
                Object pu = ld.get("prixUnitaire");
                l.setPrixUnitaire(pu != null ? ((Number) pu).doubleValue() : null);
                t.getLignes().add(l);
            }
        }

        t.getHistorique().add(HistoriqueTransfert.creer(t, "RECEPTION",
                "Reçu de " + t.getBoutiqueSourceNom(), "SYSTÈME"));

        TransfertStock saved = transfertRepository.save(t);

        notifService.creer("TRANSFERT_RECU", "Transfert reçu",
                "Transfert reçu de " + saved.getBoutiqueSourceNom() +
                " — " + saved.getNumeroTransfert(), "/pages/transferts");

        // Notification WebSocket temps réel
        stockWebSocketService.diffuserTransfert(BOUTIQUE_ID, Map.of(
                "type", "TRANSFERT_RECU",
                "numeroTransfert", saved.getNumeroTransfert(),
                "source", saved.getBoutiqueSourceNom(),
                "timestamp", System.currentTimeMillis()
        ));

        return saved;
    }

    // ==================== RECUS / ENVOYES ====================

    private String getNomBoutiqueCourante() {
        return boutiqueRepository.findAll().stream()
            .findFirst()
            .map(b -> b.getNom())
            .orElse("Cette boutique");
    }

    public List<TransfertStock> getRecus() {
        String nomBoutique = getNomBoutiqueCourante();
        return transfertRepository.findByBoutiqueDestNomOrderByDateCreationDesc(nomBoutique);
    }

    public List<TransfertStock> getEnvoyes() {
        String nomBoutique = getNomBoutiqueCourante();
        return transfertRepository.findByBoutiqueSourceNomOrderByDateCreationDesc(nomBoutique);
    }

    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public TransfertStock accepter(Long id, String currentUser) {
        TransfertStock t = getById(id);
        if (t.getStatut() != StatutTransfert.EN_ATTENTE_CONFIRMATION && t.getStatut() != StatutTransfert.EN_ATTENTE) {
            throw new IllegalStateException("Ce transfert ne peut pas être accepté dans son état actuel");
        }

        for (LigneTransfert l : t.getLignes()) {
            Optional<Produit> optProduit = produitRepository.findByNomIgnoreCase(l.getProduitNom()).stream().findFirst();

            Produit produit;
            int quantiteAvant;

            if (optProduit.isPresent()) {
                produit = optProduit.get();
                quantiteAvant = produit.getQuantite();
                produit.setQuantite(produit.getQuantite() + l.getQuantite());
                produitRepository.save(produit);
            } else {
                Categorie categorieDefaut = categorieRepository.findAll().stream().findFirst().orElse(null);
                if (categorieDefaut == null) {
                    log.warn("Impossible de créer le produit '{}' : aucune catégorie disponible", l.getProduitNom());
                    continue;
                }
                produit = new Produit();
                produit.setNom(l.getProduitNom());
                produit.setQuantite(l.getQuantite());
                produit.setPrixAchat(l.getPrixUnitaire() != null ? l.getPrixUnitaire() : 0.0);
                produit.setPrixVente(l.getPrixUnitaire() != null ? l.getPrixUnitaire() : 0.0);
                produit.setSeuilAlerte(5);
                produit.setDescription("Créé automatiquement via transfert " + t.getNumeroTransfert());
                produit.setDateCreation(LocalDate.now());
                produit.setCategorie(categorieDefaut);
                produitRepository.save(produit);
                quantiteAvant = 0;
            }

            MouvementStock m = new MouvementStock();
            m.setProduit(produit);
            m.setQuantite(l.getQuantite());
            m.setTypeMouvement(TypeMouvement.ENTREE);
            m.setQuantiteAvant(quantiteAvant);
            m.setQuantiteApres(produit.getQuantite());
            m.setMotif("Reçu de " + t.getBoutiqueSourceNom());
            m.setReferenceType("TRANSFERT");
            m.setReferenceId(t.getId());
            mouvementStockRepository.save(m);
        }

        t.setStatut(StatutTransfert.ACCEPTE);
        t.setDateConfirmation(LocalDateTime.now());
        t.setConfirmeParUser(currentUser);
        t.getHistorique().add(HistoriqueTransfert.creer(t, "ACCEPTATION",
            "Accepté par " + currentUser, currentUser));
        TransfertStock saved = transfertRepository.save(t);
        notifService.creer("TRANSFERT_ACCEPTE", "Transfert accepté",
            "Transfert " + saved.getNumeroTransfert() + " accepté", "/pages/transferts");
        return saved;
    }

    @Transactional
    public TransfertStock rejeter(Long id, String motif, String currentUser) {
        TransfertStock t = getById(id);
        if (t.getStatut() != StatutTransfert.EN_ATTENTE_CONFIRMATION && t.getStatut() != StatutTransfert.EN_ATTENTE) {
            throw new IllegalStateException("Ce transfert ne peut pas être rejeté dans son état actuel");
        }
        t.setStatut(StatutTransfert.REJETE);
        t.setMotifRejet(motif);
        t.getHistorique().add(HistoriqueTransfert.creer(t, "REJET",
            "Rejeté par " + currentUser + (motif != null ? " — " + motif : ""), currentUser));
        return transfertRepository.save(t);
    }

    // ==================== PAIEMENTS ====================

    public List<PaiementTransfert> getPaiementsTransfert(Long transfertId) {
        return paiementTransfertRepository.findByTransfertIdOrderByDatePaiementDesc(transfertId);
    }

    @Transactional
    public PaiementTransfert ajouterPaiement(Long transfertId, PaiementTransfertRequest req, String currentUser) {
        TransfertStock t = getById(transfertId);
        PaiementTransfert p = new PaiementTransfert();
        p.setTransfert(t);
        p.setMontant(req.getMontant());
        p.setModePaiement(req.getModePaiement());
        p.setNotes(req.getNotes());
        p.setEnregistrePar(currentUser);
        return paiementTransfertRepository.save(p);
    }

    // ==================== STOCK ====================

    private void deduireStock(TransfertStock t) {
        for (LigneTransfert l : t.getLignes()) {
            Optional<Produit> opt = produitRepository.findById(l.getProduitId());
            if (opt.isPresent()) {
                Produit p = opt.get();
                int ancienneQuantite = p.getQuantite();
                int nouvelleQte = Math.max(0, ancienneQuantite - l.getQuantite());
                p.setQuantite(nouvelleQte);
                produitRepository.save(p);

                MouvementStock m = new MouvementStock();
                m.setProduit(p);
                m.setQuantite(l.getQuantite());
                m.setTypeMouvement(TypeMouvement.SORTIE);
                m.setQuantiteAvant(ancienneQuantite);
                m.setQuantiteApres(p.getQuantite());
                m.setMotif("Envoyé à " + t.getBoutiqueDestNom());
                m.setReferenceType("TRANSFERT");
                m.setReferenceId(t.getId());
                mouvementStockRepository.save(m);
            }
        }
    }

    private void restaurerStock(TransfertStock t) {
        for (LigneTransfert l : t.getLignes()) {
            produitRepository.findById(l.getProduitId()).ifPresent(p -> {
                p.setQuantite(p.getQuantite() + l.getQuantite());
                produitRepository.save(p);
            });
        }
    }

    // ==================== NOTIFICATION DISTANTE ====================

    private void notifierDestination(TransfertStock t, String destUrl) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("boutiqueSourceNom", t.getBoutiqueSourceNom());
            payload.put("boutiqueSourceUrl", t.getBoutiqueSourceUrl());
            payload.put("boutiqueDestNom", t.getBoutiqueDestNom());
            payload.put("typePaiement", t.getTypePaiement().name());
            payload.put("notes", t.getNotes());
            payload.put("creePar", t.getCreePar());
            payload.put("lignes", t.getLignes().stream().map(l -> {
                Map<String, Object> m = new HashMap<>();
                m.put("produitId", l.getProduitId());
                m.put("produitNom", l.getProduitNom());
                m.put("quantite", l.getQuantite());
                m.put("prixUnitaire", l.getPrixUnitaire());
                return m;
            }).toList());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!transfertServiceKey.isBlank()) {
                headers.set("X-Service-Key", transfertServiceKey);
            }
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            restTemplate.postForObject(destUrl.stripTrailing() + "/api/transferts/recevoir", entity, Object.class);
        } catch (Exception e) {
            log.warn("Impossible de notifier la boutique destination {}: {}", destUrl, e.getMessage());
        }
    }
}
