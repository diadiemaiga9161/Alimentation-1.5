package com.ges.boutique.vente;

import com.ges.boutique.avance.AvanceClientService;
import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.client.Client;
import com.ges.boutique.client.ClientRepository;
import com.ges.boutique.config.NotificationService;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.exception.StockInsuffisantException;
import com.ges.boutique.inventaire.InventaireService;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VenteServiceImpl implements VenteService {

    private static final String CLIENT_DIVERS_NOM = "Client divers";

    private final VenteRepository venteRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final InventaireService inventaireService;
    private final LigneVenteRepository ligneVenteRepository;
    private final CaisseService caisseService;
    private final ClientRepository clientRepository;
    private final AvanceClientService avanceClientService;
    private final NotificationService notificationService;

    // ==================== CRÉATION VENTES ====================

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public Vente creerVente(VenteRequest request) {
        log.info("Création d'une nouvelle vente COMPTANT pour le vendeur ID: {}", request.getVendeurId());

        Vente savedVente = preparerVenteBase(request);

        caisseService.enregistrerVente(savedVente, request.getVendeurId(),
                request.getModePaiement().toString(), request.getReferencePaiement());

        log.info("Vente comptant créée - Numéro: {}, Montant: {}", savedVente.getNumeroVente(), savedVente.getMontantTotal());

        // Notification temps réel
        notificationService.notifierNouvelleVente(Map.of(
                "id", savedVente.getId(),
                "numeroVente", savedVente.getNumeroVente(),
                "montantTotal", savedVente.getMontantTotal(),
                "type", "COMPTANT"
        ));
        notificationService.notifierMiseAJourDashboard();

        return savedVente;
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public Vente creerVenteCredit(VenteCreditRequest request) {
        log.info("=== CRÉATION D'UN CRÉDIT UNIQUEMENT ===");
        log.info("Client: {}", request.getClientNom());

        request.setEstCredit(true);

        if (request.getClientId() == null && (request.getClientNom() == null || request.getClientNom().trim().isEmpty())) {
            throw new IllegalArgumentException("Le nom du client est requis pour un crédit");
        }

        if (request.getDateEcheance() == null) {
            request.setDateEcheance(LocalDate.now().plusDays(30));
        }

        if (request.getDateEcheance().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La date d'échéance ne peut pas être dans le passé");
        }

        if (request.getClientId() == null) {
            request.setCreerClient(true);
            request.setClientDivers(false);
        }

        validerRequeteVente(request);

        Utilisateur vendeur = utilisateurRepository.findById(request.getVendeurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Vendeur non trouvé: " + request.getVendeurId()));

        Vente vente = new Vente();
        vente.setVendeur(vendeur);
        vente.setModePaiement(request.getModePaiement());
        vente.setReferencePaiement(request.getReferencePaiement());
        vente.setEstCredit(true);
        vente.setMontantVerse(0.0);
        vente.setMontantRestant(0.0);
        vente.setCreditRegle(false);

        gererClientVente(vente, request);

        for (LigneVenteRequest ligneRequest : request.getLignes()) {
            LigneVente ligne = creerLigneVente(ligneRequest);
            vente.ajouterLigne(ligne);
        }

        vente.calculerTotal();

        if (request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0 && request.getTypeRemiseGlobale() != null) {
            if (request.getTypeRemiseGlobale() == RemiseType.POURCENTAGE) {
                vente.appliquerRemiseGlobalePourcentage(request.getRemiseGlobale());
            } else {
                vente.appliquerRemiseGlobaleMontant(request.getRemiseGlobale());
            }
        }

        vente.setDateEcheance(request.getDateEcheance());

        double avanceUtilisee = request.getMontantAvanceUtilise() != null ? request.getMontantAvanceUtilise() : 0.0;
        double montantVerseTotal = (request.getMontantVerse() != null ? request.getMontantVerse() : 0.0) + avanceUtilisee;
        vente.setMontantAvanceUtilise(avanceUtilisee);
        vente.setMontantVerse(montantVerseTotal);
        vente.setMontantRestant(vente.getMontantTotal() - montantVerseTotal);
        vente.setCreditRegle(vente.getMontantRestant() <= 0);

        if (vente.getCreditRegle()) {
            vente.setDateReglement(LocalDate.now());
        }

        if (request.getClientRequestId() != null && !request.getClientRequestId().isBlank()) {
            vente.setClientRequestId(request.getClientRequestId());
        }

        Vente savedVente = venteRepository.save(vente);
        mettreAJourStockVente(savedVente);
        caisseService.enregistrerVenteCredit(savedVente, request.getVendeurId(),
                request.getClientNom(), request.getClientTelephone(), request.getDateEcheance());

        if (avanceUtilisee > 0) {
            avanceClientService.utiliserAvance(request.getClientNom(), avanceUtilisee);
        }

        log.info("✅ CRÉDIT créé - Numéro: {}, Client: {}, Montant: {}, estCredit: {}",
                savedVente.getNumeroVente(), savedVente.getClientNom(),
                savedVente.getMontantTotal(), savedVente.getEstCredit());

        // Notification temps réel
        notificationService.notifierNouvelleVente(Map.of(
                "id", savedVente.getId(),
                "numeroVente", savedVente.getNumeroVente(),
                "montantTotal", savedVente.getMontantTotal(),
                "clientNom", savedVente.getClientNom() != null ? savedVente.getClientNom() : "",
                "type", "CREDIT"
        ));
        notificationService.notifierMiseAJourDashboard();

        return savedVente;
    }

    // ==================== LECTURE VENTES ====================

    @Override
    public Vente obtenirVenteParId(Long id) {
        return venteRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Vente non trouvée: " + id));
    }

    @Override
    public Vente obtenirVenteCreditParId(Long id) {
        Vente vente = obtenirVenteParId(id);
        if (!Boolean.TRUE.equals(vente.getEstCredit())) {
            throw new IllegalArgumentException("Cette vente n'est pas un crédit");
        }
        return vente;
    }

    @Override
    public List<Vente> obtenirToutesVentes() {
        return venteRepository.findAllNonAnnulees();
    }

    @Override
    public List<Vente> obtenirTousCredits() {
        return venteRepository.findAllCredits();
    }

    @Override
    public List<Vente> obtenirCreditsNonRegles() {
        return venteRepository.findCreditsNonRegles();
    }

    @Override
    public List<Vente> obtenirCreditsEnRetard() {
        return venteRepository.findCreditsEnRetard();
    }

    @Override
    public List<Vente> obtenirCreditsParClient(String clientNom) {
        return venteRepository.findCreditsByClientNom(clientNom);
    }

    @Override
    public List<Vente> obtenirVentesParVendeur(Long vendeurId) {
        return venteRepository.findByVendeurId(vendeurId).stream()
                .filter(v -> !Boolean.TRUE.equals(v.getAnnulee()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Vente> obtenirVentesParDateRange(LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);
        return venteRepository.findByDateRange(debut, fin).stream()
                .filter(v -> !Boolean.TRUE.equals(v.getAnnulee()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Vente> obtenirVentesDuJour() {
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return venteRepository.findTodayVentes(debut, fin).stream()
                .filter(v -> !Boolean.TRUE.equals(v.getAnnulee()))
                .collect(Collectors.toList());
    }

    // ==================== MODIFICATION VENTES ====================

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public Vente modifierVente(Long venteId, VenteRequest request) {
        log.info("Modification de la vente ID: {}", venteId);

        Vente venteExistante = obtenirVenteParId(venteId);

        if (Boolean.TRUE.equals(venteExistante.getEstCredit())) {
            throw new IllegalArgumentException("Utilisez modifierVenteCredit pour modifier un crédit");
        }

        retablirStockAncienneVente(venteExistante);
        venteExistante.getLignes().clear();

        Utilisateur vendeur = utilisateurRepository.findById(request.getVendeurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Vendeur non trouvé: " + request.getVendeurId()));

        venteExistante.setVendeur(vendeur);
        venteExistante.setModePaiement(request.getModePaiement());
        venteExistante.setReferencePaiement(request.getReferencePaiement());
        gererClientVente(venteExistante, request);

        for (LigneVenteRequest ligneRequest : request.getLignes()) {
            LigneVente ligne = creerLigneVente(ligneRequest);
            venteExistante.ajouterLigne(ligne);
        }

        venteExistante.setRemiseGlobale(0.0);
        venteExistante.setTypeRemiseGlobale(null);

        if (request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0 && request.getTypeRemiseGlobale() != null) {
            if (request.getTypeRemiseGlobale() == RemiseType.POURCENTAGE) {
                venteExistante.appliquerRemiseGlobalePourcentage(request.getRemiseGlobale());
            } else {
                venteExistante.appliquerRemiseGlobaleMontant(request.getRemiseGlobale());
            }
        } else {
            venteExistante.calculerTotal();
        }

        Vente venteModifiee = venteRepository.save(venteExistante);
        mettreAJourStockVente(venteModifiee);

        return venteModifiee;
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public Vente modifierVenteCredit(Long venteId, VenteCreditRequest request) {
        log.info("Modification du crédit ID: {}", venteId);

        Vente venteExistante = obtenirVenteParId(venteId);

        if (!Boolean.TRUE.equals(venteExistante.getEstCredit())) {
            throw new IllegalArgumentException("Cette vente n'est pas un crédit");
        }

        if (Boolean.TRUE.equals(venteExistante.getCreditRegle())) {
            throw new IllegalStateException("Impossible de modifier un crédit déjà réglé");
        }

        retablirStockAncienneVente(venteExistante);
        venteExistante.getLignes().clear();

        Utilisateur vendeur = utilisateurRepository.findById(request.getVendeurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Vendeur non trouvé: " + request.getVendeurId()));

        venteExistante.setVendeur(vendeur);
        venteExistante.setModePaiement(request.getModePaiement());
        venteExistante.setReferencePaiement(request.getReferencePaiement());

        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId()).orElse(null);
            if (client != null) {
                venteExistante.setClient(client);
                venteExistante.setClientNom(client.getNom());
                venteExistante.setClientPrenom(client.getPrenom());
                venteExistante.setClientTelephone(client.getNumeroTelephone());
                venteExistante.setClientDivers(false);
            }
        } else {
            venteExistante.setClient(null);
            venteExistante.setClientNom(request.getClientNom());
            venteExistante.setClientPrenom(request.getClientPrenom());
            venteExistante.setClientTelephone(request.getClientTelephone());
            venteExistante.setClientDivers(false);
        }

        for (LigneVenteRequest ligneRequest : request.getLignes()) {
            LigneVente ligne = creerLigneVente(ligneRequest);
            venteExistante.ajouterLigne(ligne);
        }

        venteExistante.setRemiseGlobale(0.0);
        venteExistante.setTypeRemiseGlobale(null);

        if (request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0 && request.getTypeRemiseGlobale() != null) {
            if (request.getTypeRemiseGlobale() == RemiseType.POURCENTAGE) {
                venteExistante.appliquerRemiseGlobalePourcentage(request.getRemiseGlobale());
            } else {
                venteExistante.appliquerRemiseGlobaleMontant(request.getRemiseGlobale());
            }
        } else {
            venteExistante.calculerTotal();
        }

        venteExistante.setDateEcheance(request.getDateEcheance());
        venteExistante.setMontantVerse(request.getMontantVerse() != null ? request.getMontantVerse() : 0.0);
        venteExistante.setMontantRestant(venteExistante.getMontantTotal() - venteExistante.getMontantVerse());
        venteExistante.setCreditRegle(venteExistante.getMontantRestant() <= 0);

        if (venteExistante.getCreditRegle()) {
            venteExistante.setDateReglement(LocalDate.now());
        }

        Vente venteModifiee = venteRepository.save(venteExistante);
        mettreAJourStockVente(venteModifiee);

        return venteModifiee;
    }

    // ==================== SUPPRESSION ET ANNULATION ====================

    @Override
    @Transactional
    public void supprimerVente(Long venteId) {
        log.info("Suppression de la vente ID: {}", venteId);

        Vente vente = obtenirVenteParId(venteId);

        if (Boolean.TRUE.equals(vente.getEstCredit())) {
            throw new IllegalArgumentException("Utilisez supprimerVenteCredit pour supprimer un crédit");
        }

        retablirStockAncienneVente(vente);
        caisseService.annulerVente(vente, null, "Suppression vente");

        vente.setAnnulee(true);
        vente.setMotifAnnulation("Suppression vente");
        vente.setDateAnnulation(LocalDateTime.now());
        venteRepository.save(vente);
    }

    @Override
    @Transactional
    public void supprimerVenteCredit(Long venteId) {
        log.info("Suppression du crédit ID: {}", venteId);

        Vente vente = obtenirVenteParId(venteId);

        if (!Boolean.TRUE.equals(vente.getEstCredit())) {
            throw new IllegalArgumentException("Cette vente n'est pas un crédit");
        }

        if (Boolean.TRUE.equals(vente.getCreditRegle())) {
            throw new IllegalStateException("Impossible de supprimer un crédit déjà réglé");
        }

        retablirStockAncienneVente(vente);
        caisseService.annulerVenteCredit(vente, null, "Suppression crédit");

        vente.setAnnulee(true);
        vente.setMotifAnnulation("Suppression crédit");
        vente.setDateAnnulation(LocalDateTime.now());
        venteRepository.save(vente);
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public Vente annulerVente(Long venteId, Long utilisateurId, String motif) {
        log.info("Annulation de la vente ID: {} par utilisateur: {}", venteId, utilisateurId);

        Vente vente = obtenirVenteParId(venteId);

        if (Boolean.TRUE.equals(vente.getAnnulee())) {
            throw new IllegalStateException("Cette vente est déjà annulée");
        }

        if (Boolean.TRUE.equals(vente.getEstCredit())) {
            if (Boolean.TRUE.equals(vente.getCreditRegle())) {
                throw new IllegalStateException("Impossible d'annuler un crédit déjà réglé");
            }
            if (vente.getMontantVerse() != null && vente.getMontantVerse() > 0) {
                throw new IllegalStateException("Impossible d'annuler un crédit avec des règlements partiels");
            }
        }

        retablirStockAncienneVente(vente);

        if (!Boolean.TRUE.equals(vente.getEstCredit())) {
            caisseService.annulerVente(vente, utilisateurId, motif != null ? motif : "Annulation vente");
        } else {
            caisseService.annulerVenteCredit(vente, utilisateurId, motif != null ? motif : "Annulation crédit");
        }

        vente.setAnnulee(true);
        vente.setMotifAnnulation(motif);
        vente.setDateAnnulation(LocalDateTime.now());
        vente.setUtilisateurAnnulation(utilisateurId);

        Vente saved = venteRepository.save(vente);
        notificationService.notifierVenteAnnulee(Map.of("venteId", venteId, "montant", vente.getMontantTotal()));
        notificationService.notifierMiseAJourDashboard();
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public Vente annulerVenteCredit(Long venteId, Long utilisateurId, String motif) {
        return annulerVente(venteId, utilisateurId, motif);
    }

    // ==================== RÈGLEMENTS CRÉDIT ====================

    @Override
    @Transactional
    public Vente enregistrerReglementCredit(Long venteId, ReglementCreditRequest request) {
        log.info("Enregistrement règlement crédit - Vente ID: {}, Montant: {}", venteId, request.getMontantRegle());

        Vente vente = obtenirVenteParId(venteId);

        if (!Boolean.TRUE.equals(vente.getEstCredit())) {
            throw new IllegalArgumentException("Cette vente n'est pas un crédit");
        }

        if (Boolean.TRUE.equals(vente.getCreditRegle())) {
            throw new IllegalStateException("Ce crédit est déjà réglé");
        }

        if (request.getMontantRegle() == null || request.getMontantRegle() <= 0) {
            throw new IllegalArgumentException("Le montant du règlement doit être supérieur à 0");
        }

        if (request.getMontantRegle() > vente.getMontantRestant()) {
            throw new IllegalArgumentException("Le montant réglé ne peut pas dépasser le montant restant (" + vente.getMontantRestant() + ")");
        }

        LocalDate dateReglement = request.getDateReglement() != null ? request.getDateReglement() : LocalDate.now();
        String modePaiement = request.getModePaiement() != null ? request.getModePaiement() : "ESPECES";

        caisseService.reglementCredit(venteId, request.getMontantRegle(), request.getUtilisateurId(), modePaiement, request.getReferencePaiement());

        vente.enregistrerReglement(request.getMontantRegle(), dateReglement);
        return venteRepository.save(vente);
    }

    // ==================== REMISES ====================

    @Override
    @Transactional
    public Vente appliquerRemiseGlobale(Long venteId, Double remise, RemiseType type) {
        Vente vente = obtenirVenteParId(venteId);

        if (remise == null || remise < 0) {
            throw new IllegalArgumentException("Le montant de la remise doit être positif");
        }

        if (type == RemiseType.POURCENTAGE && remise > 100) {
            throw new IllegalArgumentException("Le pourcentage de remise ne peut pas dépasser 100%");
        }

        if (type == RemiseType.POURCENTAGE) {
            vente.appliquerRemiseGlobalePourcentage(remise);
        } else {
            vente.appliquerRemiseGlobaleMontant(remise);
        }

        return venteRepository.save(vente);
    }

    @Override
    @Transactional
    public LigneVente appliquerRemiseLigne(Long ligneId, Double remise, RemiseType type) {
        LigneVente ligne = ligneVenteRepository.findById(ligneId)
                .orElseThrow(() -> new RessourceIntrouvableException("Ligne non trouvée: " + ligneId));

        if (remise == null || remise < 0) {
            throw new IllegalArgumentException("Le montant de la remise doit être positif");
        }

        if (type == RemiseType.POURCENTAGE && remise > 100) {
            throw new IllegalArgumentException("Le pourcentage de remise ne peut pas dépasser 100%");
        }

        Double prixMax = ligne.getPrixUnitaire() * ligne.getQuantite();
        if (type == RemiseType.MONTANT_FIXE && remise > prixMax) {
            throw new IllegalArgumentException("La remise ne peut pas dépasser le sous-total (" + prixMax + ")");
        }

        if (type == RemiseType.POURCENTAGE) {
            ligne.appliquerRemisePourcentage(remise);
        } else {
            ligne.appliquerRemiseMontant(remise);
        }

        ligne.getVente().calculerTotal();
        venteRepository.save(ligne.getVente());

        return ligneVenteRepository.save(ligne);
    }

    @Override
    @Transactional
    public Vente annulerRemiseGlobale(Long venteId) {
        Vente vente = obtenirVenteParId(venteId);
        vente.setRemiseGlobale(0.0);
        vente.setTypeRemiseGlobale(null);
        vente.calculerTotal();
        return venteRepository.save(vente);
    }

    @Override
    @Transactional
    public LigneVente annulerRemiseLigne(Long ligneId) {
        LigneVente ligne = ligneVenteRepository.findById(ligneId)
                .orElseThrow(() -> new RessourceIntrouvableException("Ligne non trouvée: " + ligneId));

        ligne.setRemisePourcentage(0.0);
        ligne.setRemiseMontant(0.0);
        ligne.calculerSousTotal();

        ligne.getVente().calculerTotal();
        venteRepository.save(ligne.getVente());

        return ligneVenteRepository.save(ligne);
    }

    // ==================== STATISTIQUES ====================

    @Override
    public Map<String, Object> obtenirStatistiquesChiffreAffaire() {
        LocalDate today = LocalDate.now();
        LocalDateTime debutJour = today.atStartOfDay();
        LocalDateTime finJour = today.atTime(LocalTime.MAX);
        LocalDateTime debutSemaine = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime debutMois = today.withDayOfMonth(1).atStartOfDay();

        Map<String, Object> stats = new HashMap<>();
        Double caJour = venteRepository.getChiffreAffaireJournalier(debutJour, finJour);
        stats.put("chiffreAffaireJournalier", caJour != null ? caJour : 0);
        Double caHebdo = venteRepository.getChiffreAffaireHebdomadaire(debutSemaine, finJour);
        stats.put("chiffreAffaireHebdomadaire", caHebdo != null ? caHebdo : 0);
        Double caMensuel = venteRepository.getChiffreAffaireMensuel(debutMois, finJour);
        stats.put("chiffreAffaireMensuel", caMensuel != null ? caMensuel : 0);
        stats.put("totalCreditsNonRegles", venteRepository.getTotalCreditsNonRegles() != null ? venteRepository.getTotalCreditsNonRegles() : 0);
        Double reglementsJour = venteRepository.getTotalReglementsDuJour(today, today);
        stats.put("reglementsDuJour", reglementsJour != null ? reglementsJour : 0);
        return stats;
    }

    @Override
    public Map<String, Object> obtenirStatistiquesJournalieres(LocalDate date) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime debut = date.atStartOfDay();
        LocalDateTime fin = date.atTime(LocalTime.MAX);

        List<Vente> ventes = venteRepository.findByDateRange(debut, fin).stream()
                .filter(v -> !Boolean.TRUE.equals(v.getAnnulee()))
                .collect(Collectors.toList());

        double totalCA = ventes.stream().mapToDouble(Vente::getMontantTotal).sum();
        double totalBenefice = ventes.stream().mapToDouble(Vente::getBeneficeTotal).sum();

        stats.put("date", date);
        stats.put("nombreVentes", ventes.size());
        stats.put("chiffreAffaire", totalCA);
        stats.put("beneficeTotal", totalBenefice);
        stats.put("margeMoyenne", ventes.size() > 0 ? (totalBenefice / totalCA) * 100 : 0);

        return stats;
    }

    @Override
    public Map<String, Object> obtenirStatistiquesHebdomadaires() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutSemaine = aujourdhui.minusDays(aujourdhui.getDayOfWeek().getValue() - 1);
        LocalDate finSemaine = debutSemaine.plusDays(6);

        Map<String, Object> stats = new HashMap<>();
        stats.put("debutSemaine", debutSemaine);
        stats.put("finSemaine", finSemaine);
        stats.put("statistiquesParJour", new HashMap<>());

        for (int i = 0; i < 7; i++) {
            LocalDate jour = debutSemaine.plusDays(i);
            stats.put("jour" + (i + 1), obtenirStatistiquesJournalieres(jour));
        }

        return stats;
    }

    @Override
    public Map<String, Object> obtenirStatistiquesMensuelles() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutMois = aujourdhui.withDayOfMonth(1);
        LocalDate finMois = aujourdhui.withDayOfMonth(aujourdhui.lengthOfMonth());

        List<Vente> ventes = obtenirVentesParDateRange(debutMois, finMois);

        Map<String, Object> stats = new HashMap<>();
        stats.put("mois", aujourdhui.getMonth().toString());
        stats.put("annee", aujourdhui.getYear());
        stats.put("nombreVentes", ventes.size());
        stats.put("chiffreAffaire", ventes.stream().mapToDouble(Vente::getMontantTotal).sum());
        stats.put("beneficeTotal", ventes.stream().mapToDouble(Vente::getBeneficeTotal).sum());

        return stats;
    }

    @Override
    public Map<String, Object> getStatistiquesCredits() {
        Map<String, Object> stats = new HashMap<>();
        List<Vente> creditsNonRegles = obtenirCreditsNonRegles();
        List<Vente> creditsEnRetard = obtenirCreditsEnRetard();

        stats.put("nombreCreditsNonRegles", creditsNonRegles.size());
        stats.put("montantTotalCreditsNonRegles", creditsNonRegles.stream().mapToDouble(Vente::getMontantRestant).sum());
        stats.put("nombreCreditsEnRetard", creditsEnRetard.size());
        stats.put("montantTotalCreditsEnRetard", creditsEnRetard.stream().mapToDouble(Vente::getMontantRestant).sum());

        return stats;
    }

    @Override
    public Long compterVentesParDateRange(LocalDateTime debut, LocalDateTime fin) {
        return (long) venteRepository.findByDateRange(debut, fin).stream()
                .filter(v -> !Boolean.TRUE.equals(v.getAnnulee()))
                .count();
    }

    @Override
    public Double obtenirChiffreAffaireVendeur(Long vendeurId) {
        return obtenirVentesParVendeur(vendeurId).stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum();
    }

    @Override
    public List<Map<String, Object>> obtenirTopClients() {
        Map<String, Double> topClients = new HashMap<>();
        for (Vente vente : obtenirToutesVentes()) {
            String clientNom = vente.getClientNom() != null ? vente.getClientNom() : "Client divers";
            topClients.merge(clientNom, vente.getMontantTotal(), Double::sum);
        }

        return topClients.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of("client", e.getKey(), "montantTotal", e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> obtenirTopProduitsParQuantite() {
        Map<String, Integer> topProduits = new HashMap<>();
        for (Vente vente : obtenirToutesVentes()) {
            for (LigneVente ligne : vente.getLignes()) {
                String produitNom = ligne.getProduitNom();
                topProduits.merge(produitNom, ligne.getQuantite(), Integer::sum);
            }
        }

        return topProduits.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of("produit", e.getKey(), "quantite", e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> obtenirTopProduitsParChiffreAffaire() {
        Map<String, Double> topProduits = new HashMap<>();
        for (Vente vente : obtenirToutesVentes()) {
            for (LigneVente ligne : vente.getLignes()) {
                String produitNom = ligne.getProduitNom();
                topProduits.merge(produitNom, ligne.getSousTotal(), Double::sum);
            }
        }

        return topProduits.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of("produit", e.getKey(), "chiffreAffaire", e.getValue()))
                .collect(Collectors.toList());
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private Vente preparerVenteBase(VenteRequest request) {
        validerRequeteVente(request);

        Utilisateur vendeur = utilisateurRepository.findById(request.getVendeurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Vendeur non trouvé: " + request.getVendeurId()));

        Vente vente = new Vente();
        vente.setVendeur(vendeur);
        vente.setModePaiement(request.getModePaiement());
        vente.setReferencePaiement(request.getReferencePaiement());
        vente.setEstCredit(request.getEstCredit() != null && request.getEstCredit());
        vente.setMontantVerse(0.0);
        vente.setMontantRestant(0.0);
        vente.setCreditRegle(false);

        gererClientVente(vente, request);

        for (LigneVenteRequest ligneRequest : request.getLignes()) {
            LigneVente ligne = creerLigneVente(ligneRequest);
            vente.ajouterLigne(ligne);
        }

        vente.calculerTotal();

        if (request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0 && request.getTypeRemiseGlobale() != null) {
            if (request.getTypeRemiseGlobale() == RemiseType.POURCENTAGE) {
                vente.appliquerRemiseGlobalePourcentage(request.getRemiseGlobale());
            } else {
                vente.appliquerRemiseGlobaleMontant(request.getRemiseGlobale());
            }
        }

        if (request.getClientRequestId() != null && !request.getClientRequestId().isBlank()) {
            vente.setClientRequestId(request.getClientRequestId());
        }

        Vente savedVente = venteRepository.save(vente);
        mettreAJourStockVente(savedVente);
        return savedVente;
    }

    private void validerRequeteVente(VenteRequest request) {
        if (request == null) throw new IllegalArgumentException("La requête est requise");
        if (request.getVendeurId() == null) throw new IllegalArgumentException("Le vendeur est requis");
        if (request.getLignes() == null || request.getLignes().isEmpty()) {
            throw new IllegalArgumentException("La vente doit contenir au moins un produit");
        }
        if (request.getModePaiement() == null) throw new IllegalArgumentException("Le mode de paiement est requis");

        if (request.getModePaiement() != ModePaiement.ESPECES &&
                (request.getReferencePaiement() == null || request.getReferencePaiement().trim().isEmpty())) {
            throw new IllegalArgumentException("La référence de paiement est requise");
        }

        Map<Long, Integer> quantitesParProduit = new HashMap<>();
        for (LigneVenteRequest ligne : request.getLignes()) {
            if (ligne.getProduitId() == null) throw new IllegalArgumentException("L'ID produit est requis");
            if (ligne.getQuantite() == null || ligne.getQuantite() <= 0) {
                throw new IllegalArgumentException("La quantité doit être positive");
            }
            int facteur = ligne.getNiveauFacteur() != null && ligne.getNiveauFacteur() > 1 ? ligne.getNiveauFacteur() : 1;
            quantitesParProduit.merge(ligne.getProduitId(), ligne.getQuantite() * facteur, Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : quantitesParProduit.entrySet()) {
            Produit produit = produitRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé: " + entry.getKey()));
            if (produit.getQuantite() < entry.getValue()) {
                throw new StockInsuffisantException("Stock insuffisant pour " + produit.getNom() +
                        ". Disponible: " + produit.getQuantite() + ", Demandé: " + entry.getValue());
            }
        }
    }

    private LigneVente creerLigneVente(LigneVenteRequest ligneRequest) {
        Produit produit = produitRepository.findById(ligneRequest.getProduitId())
                .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé: " + ligneRequest.getProduitId()));

        LigneVente ligne = new LigneVente();
        ligne.setProduit(produit);
        ligne.setQuantite(ligneRequest.getQuantite());
        // Utilise le prix achat du niveau (conditionnement) si fourni, sinon prix achat du produit
        Double prixAchatEffectif = (ligneRequest.getPrixAchat() != null && ligneRequest.getPrixAchat() > 0)
                ? ligneRequest.getPrixAchat()
                : produit.getPrixAchat();
        ligne.setPrixAchat(prixAchatEffectif);
        // Facteur pour déduction stock en unité de base (pièces). Ex: 200 si 1 Carton = 200 Pièces
        ligne.setNiveauFacteur(ligneRequest.getNiveauFacteur() != null && ligneRequest.getNiveauFacteur() > 1
                ? ligneRequest.getNiveauFacteur() : 1);

        Double prixVente = ligneRequest.getPrixUnitaire() != null ? ligneRequest.getPrixUnitaire() : produit.getPrixVente();
        ligne.setPrixUnitaire(prixVente);
        ligne.setPrixOriginalProduit(produit.getPrixVente());

        if (ligneRequest.getRemisePourcentage() != null && ligneRequest.getRemisePourcentage() > 0) {
            ligne.appliquerRemisePourcentage(ligneRequest.getRemisePourcentage());
        } else if (ligneRequest.getRemiseMontant() != null && ligneRequest.getRemiseMontant() > 0) {
            ligne.appliquerRemiseMontant(ligneRequest.getRemiseMontant());
        } else {
            ligne.calculerSousTotal();
        }

        return ligne;
    }

    private void gererClientVente(Vente vente, VenteRequest request) {
        vente.setClient(null);
        vente.setClientDivers(false);

        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId()).orElse(null);
            if (client != null) {
                vente.setClient(client);
                vente.setClientNom(client.getNom());
                vente.setClientPrenom(client.getPrenom());
                vente.setClientTelephone(client.getNumeroTelephone());
                return;
            }
        }

        if (request.getClientTelephone() != null && !request.getClientTelephone().trim().isEmpty()) {
            Client client = clientRepository.findByNumeroTelephone(request.getClientTelephone()).orElse(null);
            if (client != null) {
                vente.setClient(client);
                vente.setClientNom(client.getNom());
                vente.setClientPrenom(client.getPrenom());
                vente.setClientTelephone(client.getNumeroTelephone());
                return;
            }
            if (Boolean.TRUE.equals(request.getCreerClient())) {
                client = new Client();
                client.setNom(request.getClientNom() != null ? request.getClientNom() : "Client");
                client.setPrenom(request.getClientPrenom() != null ? request.getClientPrenom() : "");
                client.setNumeroTelephone(request.getClientTelephone());
                client = clientRepository.save(client);
                vente.setClient(client);
                vente.setClientNom(client.getNom());
                vente.setClientPrenom(client.getPrenom());
                vente.setClientTelephone(client.getNumeroTelephone());
                return;
            }
        }

        vente.setClientDivers(true);
        vente.setClientNom(request.getClientNom() != null ? request.getClientNom() : CLIENT_DIVERS_NOM);
        vente.setClientPrenom(request.getClientPrenom());
        vente.setClientTelephone(request.getClientTelephone());
    }

    private void mettreAJourStockVente(Vente vente) {
        for (LigneVente ligne : vente.getLignes()) {
            Long produitId = ligne.getProduit().getId();
            int facteur = ligne.getNiveauFacteur() != null ? ligne.getNiveauFacteur() : 1;
            inventaireService.sortieStock(produitId, ligne.getQuantite() * facteur,
                    vente.getVendeur().getId(), "Vente N°" + vente.getNumeroVente());
            produitRepository.findById(produitId).ifPresent(p ->
                    notificationService.notifierMiseAJourStock(p.getId(), p.getNom(), p.getQuantite()));
        }
    }

    // ==================== MODIFICATION LIGNES AVEC AJUSTEMENT CAISSE ====================

    @Transactional
    @CacheEvict(value = "produits", allEntries = true)
    public Map<String, Object> modifierLignesVente(Long venteId, ModificationLignesRequest request) {
        log.info("=== MODIFICATION LIGNES VENTE ID: {} ===", venteId);

        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RessourceIntrouvableException("Vente introuvable: " + venteId));

        if (Boolean.TRUE.equals(vente.getAnnulee())) {
            throw new IllegalStateException("Impossible de modifier une vente annulée");
        }

        if (Boolean.TRUE.equals(vente.getEstCredit()) && Boolean.TRUE.equals(vente.getCreditRegle())) {
            throw new IllegalStateException("Impossible de modifier un crédit entièrement réglé");
        }

        if (request.getLignes() == null || request.getLignes().isEmpty()) {
            throw new IllegalArgumentException("La liste des nouvelles lignes ne peut pas être vide");
        }

        double ancienTotal = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;

        // 1. Remettre l'ancien stock
        retablirStockAncienneVente(vente);
        vente.getLignes().clear();
        venteRepository.save(vente);

        // 2. Construire les nouvelles lignes
        for (LigneVenteRequest ligneReq : request.getLignes()) {
            LigneVente ligne = creerLigneVente(ligneReq);
            vente.ajouterLigne(ligne);
        }

        vente.calculerTotal();
        double nouveauTotal = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
        double difference = nouveauTotal - ancienTotal;

        // 3. Ajuster la caisse selon la différence
        String motif = (request.getMotif() != null ? request.getMotif() : "Modification vente N°" + vente.getNumeroVente());

        if (Math.abs(difference) > 0.01) {
            if (difference > 0) {
                // Client paye la différence → ENTRÉE caisse
                caisseService.entreeCaisse(difference,
                        "Complément modification vente N°" + vente.getNumeroVente() + " - " + motif,
                        request.getUtilisateurId(), "ESPECES", null);
                log.info("Entrée caisse: +{} F (client paye la différence)", difference);
            } else {
                // Remboursement client → SORTIE caisse
                caisseService.sortieCaisse(Math.abs(difference),
                        "Remboursement modification vente N°" + vente.getNumeroVente() + " - " + motif,
                        request.getUtilisateurId());
                log.info("Sortie caisse: -{} F (remboursement client)", Math.abs(difference));
            }
        }

        // 4. Mettre à jour montantVerse si crédit
        if (Boolean.TRUE.equals(vente.getEstCredit())) {
            double montantVerse = vente.getMontantVerse() != null ? vente.getMontantVerse() : 0.0;
            vente.setMontantRestant(nouveauTotal - montantVerse);
            if (vente.getMontantRestant() <= 0) {
                vente.setCreditRegle(true);
                vente.setDateReglement(LocalDate.now());
            }
        }

        Vente venteSauvee = venteRepository.save(vente);
        mettreAJourStockVente(venteSauvee);

        log.info("Vente modifiée: ancien total={}, nouveau total={}, différence={}", ancienTotal, nouveauTotal, difference);

        Map<String, Object> result = new HashMap<>();
        result.put("ancienTotal", ancienTotal);
        result.put("nouveauTotal", nouveauTotal);
        result.put("difference", difference);
        result.put("venteId", venteId);
        result.put("numeroVente", vente.getNumeroVente());
        return result;
    }

    private void retablirStockAncienneVente(Vente vente) {
        for (LigneVente ligne : vente.getLignes()) {
            int facteur = ligne.getNiveauFacteur() != null ? ligne.getNiveauFacteur() : 1;
            inventaireService.entreeStock(ligne.getProduit().getId(), ligne.getQuantite() * facteur,
                    vente.getVendeur().getId(), "Annulation vente N°" + vente.getNumeroVente());
        }
    }
}