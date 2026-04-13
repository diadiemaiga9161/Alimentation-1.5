package com.ges.boutique.vente;

import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.caisse.OperationCaisse;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.exception.StockInsuffisantException;
import com.ges.boutique.inventaire.InventaireService;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VenteServiceImpl implements VenteService {

    private final VenteRepository venteRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final InventaireService inventaireService;
    private final LigneVenteRepository ligneVenteRepository;
    private final CaisseService caisseService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vente creerVente(VenteRequest request) {
        log.info("Création d'une nouvelle vente pour le vendeur ID: {}", request.getVendeurId());

        try {
            validerRequeteVente(request);

            Utilisateur vendeur = utilisateurRepository.findById(request.getVendeurId())
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Vendeur non trouvé avec l'ID: " + request.getVendeurId()
                    ));

            Vente vente = construireVente(request, vendeur);

            Vente savedVente = venteRepository.save(vente);

            mettreAJourStockVente(savedVente);

            if (!request.isEstCredit()) {
                enregistrerVenteEnCaisse(savedVente, request);
            }

            log.info("Vente créée avec succès - Numéro: {}, Montant: {}, Remise: {}, Produits: {}",
                    savedVente.getNumeroVente(),
                    savedVente.getMontantTotal(),
                    savedVente.getMontantRemiseTotal(),
                    savedVente.getLignes().size());

            return savedVente;

        } catch (Exception e) {
            log.error("Erreur lors de la création de la vente: {}", e.getMessage());
            throw new VenteException("Échec de la création de la vente: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vente creerVente(Vente vente) {
        log.info("Création de vente (ancienne méthode)");

        try {
            for (LigneVente ligne : vente.getLignes()) {
                Produit produit = produitRepository.findById(ligne.getProduit().getId())
                        .orElseThrow(() -> new RessourceIntrouvableException(
                                "Produit non trouvé avec l'ID: " + ligne.getProduit().getId()));

                if (produit.getQuantite() < ligne.getQuantite()) {
                    throw new StockInsuffisantException(
                            "Stock insuffisant pour le produit: " + produit.getNom() +
                                    ". Disponible: " + produit.getQuantite() + ", Demande: " + ligne.getQuantite());
                }

                ligne.setPrixUnitaire(produit.getPrixVente());
                ligne.setProduit(produit);
            }

            vente.calculerTotal();

            Vente savedVente = venteRepository.save(vente);

            for (LigneVente ligne : savedVente.getLignes()) {
                ligne.setVente(savedVente);

                inventaireService.sortieStock(
                        ligne.getProduit().getId(),
                        ligne.getQuantite(),
                        savedVente.getVendeur().getId(),
                        "Vente N°" + savedVente.getNumeroVente()
                );
            }

            return savedVente;

        } catch (Exception e) {
            log.error("Erreur lors de la création de la vente: {}", e.getMessage());
            throw new VenteException("Échec de la création de la vente: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vente annulerVente(Long venteId, Long utilisateurId, String motifAnnulation) {
        log.info("Annulation de la vente ID: {} par l'utilisateur: {}", venteId, utilisateurId);

        try {
            Vente vente = obtenirVenteParId(venteId);

            // Vérifier si la vente est déjà annulée
            if (Boolean.TRUE.equals(vente.getAnnulee())) {
                throw new IllegalStateException("Cette vente est déjà annulée");
            }

            LocalDateTime limiteAnnulation = vente.getDateVente().plusHours(24);
            if (LocalDateTime.now().isAfter(limiteAnnulation)) {
                throw new IllegalStateException("La vente ne peut plus être annulée après 24 heures (Date vente: " +
                        vente.getDateVente() + ")");
            }

            if (Boolean.TRUE.equals(vente.getEstCredit())) {
                if (Boolean.TRUE.equals(vente.getCreditRegle())) {
                    throw new IllegalStateException("Impossible d'annuler un crédit déjà réglé");
                }

                if (vente.getMontantVerse() != null && vente.getMontantVerse() > 0) {
                    throw new IllegalStateException("Impossible d'annuler un crédit avec des règlements partiels (Montant versé: " +
                            vente.getMontantVerse() + ")");
                }
            }

            // Rétablir le stock
            retablirStockAncienneVente(vente);

            // Enregistrer l'annulation en caisse
            if (!Boolean.TRUE.equals(vente.getEstCredit())) {
                caisseService.annulerVente(
                        vente,
                        utilisateurId,
                        motifAnnulation != null ? motifAnnulation : "Annulation vente N°" + vente.getNumeroVente()
                );
            } else {
                caisseService.annulerVenteCredit(
                        vente,
                        utilisateurId,
                        motifAnnulation != null ? motifAnnulation : "Annulation crédit N°" + vente.getNumeroVente()
                );
            }

            // Soft delete : marquer la vente comme annulée au lieu de la supprimer
            vente.setAnnulee(true);
            vente.setMotifAnnulation(motifAnnulation);
            vente.setDateAnnulation(LocalDateTime.now());
            vente.setUtilisateurAnnulation(utilisateurId);

            // Optionnel : mettre à jour le motif dans la vente
            if (motifAnnulation != null && !motifAnnulation.isEmpty()) {
                vente.setMotifAnnulation(motifAnnulation);
            }

            Vente venteAnnulee = venteRepository.save(vente);

            log.info("Vente annulée avec succès - ID: {}, Numéro: {}, Type: {}, Montant: {}",
                    venteId,
                    vente.getNumeroVente(),
                    Boolean.TRUE.equals(vente.getEstCredit()) ? "CRÉDIT" : "COMPTANT",
                    vente.getMontantTotal());

            return venteAnnulee;

        } catch (Exception e) {
            log.error("Erreur lors de l'annulation de la vente: {}", e.getMessage());
            throw new VenteException("Échec de l'annulation de la vente: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vente creerVenteCredit(VenteCreditRequest request) {
        log.info("Création d'une nouvelle vente à CRÉDIT pour le client: {}, Téléphone: {}",
                request.getClientNom(), request.getClientTelephone());

        try {
            validerRequeteCredit(request);

            LocalDate dateEcheance = request.getDateEcheance() != null ?
                    request.getDateEcheance() : LocalDate.now().plusDays(30);

            if (dateEcheance.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("La date d'échéance ne peut pas être dans le passé");
            }

            request.setEstCredit(true);

            Vente vente = creerVente((VenteRequest) request);

            vente.setEstCredit(true);
            vente.setClientNom(request.getClientNom());
            vente.setClientTelephone(request.getClientTelephone());
            vente.setDateEcheance(dateEcheance);
            vente.setMontantVerse(request.getMontantVerse() != null ? request.getMontantVerse() : 0.0);
            vente.setMontantRestant(vente.getMontantTotal() - vente.getMontantVerse());
            vente.setCreditRegle(false);

            Vente savedVente = venteRepository.save(vente);

            enregistrerCreditEnCaisse(savedVente, request, dateEcheance);

            log.info("Vente à crédit créée avec succès - Numéro: {}, Client: {}, Montant: {}, Échéance: {}",
                    savedVente.getNumeroVente(),
                    savedVente.getClientNom(),
                    savedVente.getMontantTotal(),
                    savedVente.getDateEcheance());

            return savedVente;

        } catch (Exception e) {
            log.error("Erreur lors de la création du crédit: {}", e.getMessage());
            throw new VenteException("Échec de la création du crédit: " + e.getMessage(), e);
        }
    }

    @Override
    public Vente obtenirVenteCreditParId(Long id) {
        log.info("Récupération du crédit ID: {}", id);

        Vente vente = venteRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Crédit non trouvé avec l'ID: " + id));

        if (!Boolean.TRUE.equals(vente.getEstCredit())) {
            throw new IllegalArgumentException("La vente avec l'ID " + id + " n'est pas un crédit");
        }

        return vente;
    }

    @Override
    public List<Vente> obtenirTousCredits() {
        log.info("Récupération de tous les crédits");
        return venteRepository.findAllCredits();
    }

    @Override
    public List<Vente> obtenirCreditsNonRegles() {
        log.info("Récupération des crédits non réglés");
        return venteRepository.findCreditsNonRegles();
    }

    @Override
    public List<Vente> obtenirCreditsEnRetard() {
        log.info("Récupération des crédits en retard");
        return venteRepository.findCreditsEnRetard();
    }

    @Override
    public List<Vente> obtenirCreditsParClient(String clientNom) {
        log.info("Récupération des crédits pour le client: {}", clientNom);
        return venteRepository.findCreditsByClientNom(clientNom);
    }

    @Override
    public List<Vente> obtenirCreditsReglesParPeriode(LocalDate dateDebut, LocalDate dateFin) {
        log.info("Récupération des crédits réglés du {} au {}", dateDebut, dateFin);
        return venteRepository.findCreditsReglesByDateRange(dateDebut, dateFin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vente modifierVenteCredit(Long venteId, VenteCreditRequest request) {
        log.info("Modification du crédit ID: {}", venteId);

        try {
            Vente venteExistante = obtenirVenteParId(venteId);

            if (!Boolean.TRUE.equals(venteExistante.getEstCredit())) {
                throw new IllegalArgumentException("La vente avec l'ID " + venteId + " n'est pas un crédit");
            }

            if (Boolean.TRUE.equals(venteExistante.getCreditRegle())) {
                throw new IllegalStateException("Impossible de modifier un crédit déjà réglé");
            }

            LocalDateTime limiteModification = venteExistante.getDateVente().plusHours(24);
            if (LocalDateTime.now().isAfter(limiteModification)) {
                throw new IllegalStateException("Le crédit ne peut plus être modifié après 24 heures");
            }

            validerRequeteCredit(request);

            Utilisateur vendeur = utilisateurRepository.findById(request.getVendeurId())
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Vendeur non trouvé avec l'ID: " + request.getVendeurId()
                    ));

            retablirStockAncienneVente(venteExistante);

            venteExistante.getLignes().clear();
            ligneVenteRepository.deleteAllByVenteId(venteId);

            mettreAJourInformationsVente(venteExistante, request, vendeur);

            ajouterNouvellesLignesVente(venteExistante, request.getLignes());

            venteExistante.calculerTotal();
            venteExistante.setMontantRestant(venteExistante.getMontantApresRemise() - venteExistante.getMontantVerse());

            Vente venteModifiee = venteRepository.save(venteExistante);

            mettreAJourStockVente(venteModifiee);

            log.info("Crédit modifié avec succès - ID: {}, Numéro: {}, Client: {}, Montant: {}",
                    venteModifiee.getId(),
                    venteModifiee.getNumeroVente(),
                    venteModifiee.getClientNom(),
                    venteModifiee.getMontantTotal());

            return venteModifiee;

        } catch (Exception e) {
            log.error("Erreur lors de la modification du crédit: {}", e.getMessage());
            throw new VenteException("Échec de la modification du crédit: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void supprimerVenteCredit(Long venteId) {
        log.info("Suppression du crédit ID: {}", venteId);

        try {
            Vente vente = obtenirVenteParId(venteId);

            if (!Boolean.TRUE.equals(vente.getEstCredit())) {
                throw new IllegalArgumentException("La vente avec l'ID " + venteId + " n'est pas un crédit");
            }

            if (Boolean.TRUE.equals(vente.getCreditRegle())) {
                throw new IllegalStateException("Impossible de supprimer un crédit déjà réglé");
            }

            LocalDateTime limiteSuppression = vente.getDateVente().plusHours(24);
            if (LocalDateTime.now().isAfter(limiteSuppression)) {
                throw new IllegalStateException("Le crédit ne peut plus être supprimé après 24 heures");
            }

            retablirStockAncienneVente(vente);

            venteRepository.delete(vente);

            log.info("Crédit supprimé avec succès - ID: {}, Numéro: {}, Client: {}",
                    venteId, vente.getNumeroVente(), vente.getClientNom());

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du crédit: {}", e.getMessage());
            throw new VenteException("Échec de la suppression du crédit: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vente enregistrerReglementCredit(Long venteId, ReglementCreditRequest request) {
        log.info("Enregistrement du règlement de crédit - Vente ID: {}, Montant: {}",
                venteId, request.getMontantRegle());

        try {
            Vente vente = obtenirVenteParId(venteId);

            if (!Boolean.TRUE.equals(vente.getEstCredit())) {
                throw new IllegalArgumentException("La vente avec l'ID " + venteId + " n'est pas un crédit");
            }

            if (Boolean.TRUE.equals(vente.getCreditRegle())) {
                throw new IllegalStateException("Ce crédit est déjà réglé");
            }

            validerMontantReglement(request, vente);

            LocalDate dateReglement = request.getDateReglement() != null ?
                    request.getDateReglement() : LocalDate.now();

            String modePaiement = request.getModePaiement() != null ?
                    request.getModePaiement() : ModePaiement.ESPECES.toString();

            enregistrerReglementEnCaisse(venteId, request, modePaiement);

            vente.enregistrerReglement(request.getMontantRegle(), dateReglement);

            Vente venteMiseAJour = venteRepository.save(vente);

            log.info("Règlement enregistré avec succès - Vente: {}, Montant réglé: {}, Restant: {}, Statut: {}",
                    venteMiseAJour.getNumeroVente(),
                    request.getMontantRegle(),
                    venteMiseAJour.getMontantRestant(),
                    venteMiseAJour.getCreditRegle() ? "RÉGLÉ" : "PARTIEL");

            return venteMiseAJour;

        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du règlement: {}", e.getMessage());
            throw new VenteException("Échec de l'enregistrement du règlement: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getStatistiquesCredits() {
        log.info("Calcul des statistiques des crédits");

        Map<String, Object> stats = new HashMap<>();

        List<Vente> tousCredits = obtenirTousCredits();
        List<Vente> creditsNonRegles = obtenirCreditsNonRegles();
        List<Vente> creditsEnRetard = obtenirCreditsEnRetard();

        double montantTotalCredits = tousCredits.stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum();

        double montantRestantTotal = creditsNonRegles.stream()
                .mapToDouble(Vente::getMontantRestant)
                .sum();

        double montantRegleTotal = tousCredits.stream()
                .filter(Vente::getCreditRegle)
                .mapToDouble(Vente::getMontantVerse)
                .sum();

        double montantEnRetard = creditsEnRetard.stream()
                .mapToDouble(Vente::getMontantRestant)
                .sum();

        Double reglementsDuJour = venteRepository.getTotalReglementsDuJour();

        stats.put("nombreTotalCredits", tousCredits.size());
        stats.put("nombreCreditsNonRegles", creditsNonRegles.size());
        stats.put("nombreCreditsEnRetard", creditsEnRetard.size());
        stats.put("nombreCreditsRegles", tousCredits.size() - creditsNonRegles.size());

        stats.put("montantTotalCredits", Math.round(montantTotalCredits * 100.0) / 100.0);
        stats.put("montantRestantTotal", Math.round(montantRestantTotal * 100.0) / 100.0);
        stats.put("montantRegleTotal", Math.round(montantRegleTotal * 100.0) / 100.0);
        stats.put("montantEnRetard", Math.round(montantEnRetard * 100.0) / 100.0);
        stats.put("reglementsDuJour", reglementsDuJour != null ? reglementsDuJour : 0.0);

        double tauxRecouvrement = montantTotalCredits > 0 ?
                (montantRegleTotal / montantTotalCredits) * 100 : 0;
        stats.put("tauxRecouvrement", Math.round(tauxRecouvrement * 100.0) / 100.0);

        Map<String, Double> topClients = new HashMap<>();
        creditsNonRegles.forEach(credit ->
                topClients.put(credit.getClientNom(),
                        topClients.getOrDefault(credit.getClientNom(), 0.0) + credit.getMontantRestant())
        );
        stats.put("topClients", topClients);

        LocalDate aujourdhui = LocalDate.now();
        Map<String, Long> echeances = new HashMap<>();
        echeances.put("moins7Jours", creditsNonRegles.stream()
                .filter(c -> c.getDateEcheance() != null &&
                        ChronoUnit.DAYS.between(aujourdhui, c.getDateEcheance()) <= 7 &&
                        ChronoUnit.DAYS.between(aujourdhui, c.getDateEcheance()) >= 0)
                .count());
        echeances.put("entre7et30Jours", creditsNonRegles.stream()
                .filter(c -> c.getDateEcheance() != null &&
                        ChronoUnit.DAYS.between(aujourdhui, c.getDateEcheance()) > 7 &&
                        ChronoUnit.DAYS.between(aujourdhui, c.getDateEcheance()) <= 30)
                .count());
        echeances.put("plus30Jours", creditsNonRegles.stream()
                .filter(c -> c.getDateEcheance() != null &&
                        ChronoUnit.DAYS.between(aujourdhui, c.getDateEcheance()) > 30)
                .count());
        stats.put("echeancesAVenir", echeances);

        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vente modifierVente(Long venteId, VenteRequest request) {
        log.info("Modification de la vente ID: {}", venteId);

        try {
            Vente venteExistante = obtenirVenteParId(venteId);

            if (Boolean.TRUE.equals(venteExistante.getEstCredit())) {
                throw new IllegalArgumentException("Utilisez modifierVenteCredit pour modifier un crédit");
            }

            LocalDateTime limiteModification = venteExistante.getDateVente().plusHours(24);
            if (LocalDateTime.now().isAfter(limiteModification)) {
                throw new IllegalStateException("La vente ne peut plus être modifiée après 24 heures");
            }

            validerRequeteVente(request);

            Utilisateur vendeur = utilisateurRepository.findById(request.getVendeurId())
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Vendeur non trouvé avec l'ID: " + request.getVendeurId()
                    ));

            retablirStockAncienneVente(venteExistante);

            venteExistante.getLignes().clear();
            ligneVenteRepository.deleteAllByVenteId(venteId);

            mettreAJourInformationsVente(venteExistante, request, vendeur);

            ajouterNouvellesLignesVente(venteExistante, request.getLignes());

            venteExistante.calculerTotal();

            Vente venteModifiee = venteRepository.save(venteExistante);

            mettreAJourStockVente(venteModifiee);

            log.info("Vente modifiée avec succès - ID: {}, Numéro: {}, Nouveau montant: {}",
                    venteModifiee.getId(),
                    venteModifiee.getNumeroVente(),
                    venteModifiee.getMontantTotal());

            return venteModifiee;

        } catch (Exception e) {
            log.error("Erreur lors de la modification de la vente: {}", e.getMessage());
            throw new VenteException("Échec de la modification de la vente: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void supprimerVente(Long venteId) {
        log.info("Suppression de la vente ID: {}", venteId);

        try {
            Vente vente = obtenirVenteParId(venteId);

            if (Boolean.TRUE.equals(vente.getEstCredit())) {
                throw new IllegalArgumentException("Utilisez supprimerVenteCredit pour supprimer un crédit");
            }

            LocalDateTime limiteSuppression = vente.getDateVente().plusHours(24);
            if (LocalDateTime.now().isAfter(limiteSuppression)) {
                throw new IllegalStateException("La vente ne peut plus être supprimée après 24 heures");
            }

            retablirStockAncienneVente(vente);

            venteRepository.delete(vente);

            log.info("Vente supprimée avec succès - ID: {}, Numéro: {}", venteId, vente.getNumeroVente());

        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la vente: {}", e.getMessage());
            throw new VenteException("Échec de la suppression de la vente: " + e.getMessage(), e);
        }
    }

    @Override
    public Vente obtenirVenteParId(Long id) {
        return venteRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Vente non trouvée avec l'ID: " + id));
    }

    @Override
    public List<Vente> obtenirToutesVentes() {
        return venteRepository.findAllNonAnnulees();
    }

    @Override
    public List<Vente> obtenirVentesParDateRange(LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);
        List<Vente> ventes = venteRepository.findByDateRange(debut, fin);
        return ventes.stream().filter(v -> !Boolean.TRUE.equals(v.getAnnulee())).toList();
    }

    @Override
    public List<Vente> obtenirVentesDuJour() {
        List<Vente> ventes = venteRepository.findTodayVentes();
        return ventes.stream().filter(v -> !Boolean.TRUE.equals(v.getAnnulee())).toList();
    }

    @Override
    public Map<String, Object> obtenirStatistiquesChiffreAffaire() {
        Map<String, Object> stats = new HashMap<>();

        Double caJournalier = venteRepository.getChiffreAffaireJournalier();
        Double caHebdomadaire = venteRepository.getChiffreAffaireHebdomadaire();
        Double caMensuel = venteRepository.getChiffreAffaireMensuel();

        stats.put("chiffreAffaireJournalier", caJournalier != null ? caJournalier : 0);
        stats.put("chiffreAffaireHebdomadaire", caHebdomadaire != null ? caHebdomadaire : 0);
        stats.put("chiffreAffaireMensuel", caMensuel != null ? caMensuel : 0);

        List<Object[]> statsPaiement = venteRepository.getChiffreAffaireParModePaiement();
        Map<String, Double> caParModePaiement = new HashMap<>();
        for (Object[] stat : statsPaiement) {
            caParModePaiement.put(stat[0].toString(), (Double) stat[1]);
        }
        stats.put("chiffreAffaireParModePaiement", caParModePaiement);

        long totalVentes = venteRepository.findAllNonAnnulees().size();
        stats.put("totalVentes", totalVentes);

        double panierMoyen = totalVentes > 0 ?
                (caJournalier != null ? caJournalier : 0) / totalVentes : 0;
        stats.put("panierMoyen", Math.round(panierMoyen * 100.0) / 100.0);

        Double totalRemises = venteRepository.findAllNonAnnulees().stream()
                .mapToDouble(Vente::getMontantRemiseTotal)
                .sum();
        stats.put("totalRemises", totalRemises);

        return stats;
    }

    @Override
    public Map<String, Object> obtenirStatistiquesJournalieres(LocalDate date) {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime debut = date.atStartOfDay();
        LocalDateTime fin = date.atTime(LocalTime.MAX);

        List<Vente> ventes = venteRepository.findByDateRange(debut, fin).stream()
                .filter(v -> !Boolean.TRUE.equals(v.getAnnulee()))
                .toList();

        Double chiffreAffaireTotal = ventes.stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum();
        Double montantRemisesTotal = ventes.stream()
                .mapToDouble(Vente::getMontantRemiseTotal)
                .sum();
        long nombreVentes = ventes.size();

        stats.put("date", date.toString());
        stats.put("chiffreAffaireTotal", chiffreAffaireTotal);
        stats.put("montantRemisesTotal", montantRemisesTotal);
        stats.put("nombreVentes", nombreVentes);
        stats.put("ventes", ventes);

        Map<String, Integer> produitsVendus = new HashMap<>();
        Map<String, Double> chiffreAffaireParProduit = new HashMap<>();
        for (Vente vente : ventes) {
            for (LigneVente ligne : vente.getLignes()) {
                String nomProduit = ligne.getProduit().getNom();
                produitsVendus.put(nomProduit,
                        produitsVendus.getOrDefault(nomProduit, 0) + ligne.getQuantite());

                chiffreAffaireParProduit.put(nomProduit,
                        chiffreAffaireParProduit.getOrDefault(nomProduit, 0.0) + ligne.getSousTotal());
            }
        }
        stats.put("produitsVendus", produitsVendus);
        stats.put("chiffreAffaireParProduit", chiffreAffaireParProduit);

        return stats;
    }

    @Override
    public Map<String, Object> obtenirStatistiquesHebdomadaires() {
        Map<String, Object> stats = new HashMap<>();

        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutSemaine = aujourdhui.minusDays(aujourdhui.getDayOfWeek().getValue() - 1);
        LocalDate finSemaine = debutSemaine.plusDays(6);

        List<Vente> ventesSemaine = obtenirVentesParDateRange(debutSemaine, finSemaine);
        Double chiffreAffaireTotal = ventesSemaine.stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum();
        Double montantRemisesTotal = ventesSemaine.stream()
                .mapToDouble(Vente::getMontantRemiseTotal)
                .sum();
        long nombreVentes = ventesSemaine.size();

        stats.put("debutSemaine", debutSemaine.toString());
        stats.put("finSemaine", finSemaine.toString());
        stats.put("chiffreAffaireTotal", chiffreAffaireTotal);
        stats.put("montantRemisesTotal", montantRemisesTotal);
        stats.put("nombreVentes", nombreVentes);
        stats.put("moyenneJournaliere", nombreVentes > 0 ? chiffreAffaireTotal / 7 : 0);
        stats.put("ventesParJour", nombreVentes / 7.0);

        Map<String, Double> caParJour = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate jour = debutSemaine.plusDays(i);
            LocalDateTime debutJour = jour.atStartOfDay();
            LocalDateTime finJour = jour.atTime(LocalTime.MAX);

            Double caJour = venteRepository.findByDateRange(debutJour, finJour).stream()
                    .filter(v -> !Boolean.TRUE.equals(v.getAnnulee()))
                    .mapToDouble(Vente::getMontantTotal)
                    .sum();
            caParJour.put(jour.getDayOfWeek().toString(), caJour);
        }
        stats.put("chiffreAffaireParJour", caParJour);

        return stats;
    }

    @Override
    public Map<String, Object> obtenirStatistiquesMensuelles() {
        Map<String, Object> stats = new HashMap<>();

        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutMois = aujourdhui.withDayOfMonth(1);
        LocalDate finMois = aujourdhui.withDayOfMonth(aujourdhui.lengthOfMonth());

        List<Vente> ventesMois = obtenirVentesParDateRange(debutMois, finMois);
        Double chiffreAffaireTotal = ventesMois.stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum();
        Double montantRemisesTotal = ventesMois.stream()
                .mapToDouble(Vente::getMontantRemiseTotal)
                .sum();
        long nombreVentes = ventesMois.size();
        int joursDansMois = aujourdhui.lengthOfMonth();

        stats.put("mois", aujourdhui.getMonth().toString());
        stats.put("annee", aujourdhui.getYear());
        stats.put("debutMois", debutMois.toString());
        stats.put("finMois", finMois.toString());
        stats.put("chiffreAffaireTotal", chiffreAffaireTotal);
        stats.put("montantRemisesTotal", montantRemisesTotal);
        stats.put("nombreVentes", nombreVentes);
        stats.put("moyenneJournaliere", nombreVentes > 0 ? chiffreAffaireTotal / joursDansMois : 0);
        stats.put("ventesParJour", (double) nombreVentes / joursDansMois);

        Map<String, Integer> topProduits = new HashMap<>();
        for (Vente vente : ventesMois) {
            for (LigneVente ligne : vente.getLignes()) {
                String nomProduit = ligne.getProduit().getNom();
                topProduits.put(nomProduit,
                        topProduits.getOrDefault(nomProduit, 0) + ligne.getQuantite());
            }
        }
        stats.put("topProduits", topProduits);

        return stats;
    }

    @Override
    public Long compterVentesParDateRange(LocalDateTime debut, LocalDateTime fin) {
        return venteRepository.countVentesByDateRange(debut, fin);
    }

    @Override
    public List<Vente> obtenirVentesParVendeur(Long vendeurId) {
        List<Vente> ventes = venteRepository.findByVendeurId(vendeurId);
        return ventes.stream().filter(v -> !Boolean.TRUE.equals(v.getAnnulee())).toList();
    }

    @Override
    public Double obtenirChiffreAffaireVendeur(Long vendeurId) {
        List<Vente> ventesVendeur = obtenirVentesParVendeur(vendeurId);
        return ventesVendeur.stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum();
    }

    @Override
    public List<Vente> obtenirVentesAvecRemise() {
        return venteRepository.findAllNonAnnulees().stream()
                .filter(vente -> vente.getMontantRemiseTotal() > 0)
                .toList();
    }

    @Override
    public Double obtenirTotalRemisesParPeriode(LocalDate debut, LocalDate fin) {
        LocalDateTime debutTime = debut.atStartOfDay();
        LocalDateTime finTime = fin.atTime(LocalTime.MAX);

        List<Vente> ventes = venteRepository.findByDateRange(debutTime, finTime).stream()
                .filter(v -> !Boolean.TRUE.equals(v.getAnnulee()))
                .toList();

        return ventes.stream()
                .mapToDouble(Vente::getMontantRemiseTotal)
                .sum();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vente annulerRemiseGlobale(Long venteId) {
        try {
            Vente vente = obtenirVenteParId(venteId);
            vente.setRemiseGlobale(0.0);
            vente.setTypeRemiseGlobale(null);
            vente.calculerTotal();
            return venteRepository.save(vente);
        } catch (Exception e) {
            log.error("Erreur lors de l'annulation de la remise globale: {}", e.getMessage());
            throw new VenteException("Échec de l'annulation de la remise: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LigneVente annulerRemiseLigne(Long ligneId) {
        try {
            LigneVente ligne = ligneVenteRepository.findById(ligneId)
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Ligne de vente non trouvée avec l'ID: " + ligneId
                    ));

            ligne.setRemisePourcentage(0.0);
            ligne.setRemiseMontant(0.0);
            ligne.calculerSousTotal();

            ligne.getVente().calculerTotal();
            venteRepository.save(ligne.getVente());

            return ligneVenteRepository.save(ligne);
        } catch (Exception e) {
            log.error("Erreur lors de l'annulation de la remise de ligne: {}", e.getMessage());
            throw new VenteException("Échec de l'annulation de la remise: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Vente appliquerRemiseGlobale(Long venteId, Double remise, RemiseType type) {
        try {
            Vente vente = obtenirVenteParId(venteId);

            if (remise == null || remise < 0) {
                throw new IllegalArgumentException("Le montant de la remise doit être positif");
            }

            if (type == RemiseType.POURCENTAGE && remise > 100) {
                throw new IllegalArgumentException("Le pourcentage de remise ne peut pas dépasser 100%");
            }

            if (type == RemiseType.MONTANT_FIXE && remise > vente.getMontantTotal()) {
                throw new IllegalArgumentException("La remise ne peut pas être supérieure au montant total");
            }

            if (type == RemiseType.POURCENTAGE) {
                vente.appliquerRemiseGlobalePourcentage(remise);
            } else {
                vente.appliquerRemiseGlobaleMontant(remise);
            }

            return venteRepository.save(vente);
        } catch (Exception e) {
            log.error("Erreur lors de l'application de la remise globale: {}", e.getMessage());
            throw new VenteException("Échec de l'application de la remise: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LigneVente appliquerRemiseLigne(Long ligneId, Double remise, RemiseType type) {
        try {
            LigneVente ligne = ligneVenteRepository.findById(ligneId)
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Ligne de vente non trouvée avec l'ID: " + ligneId
                    ));

            if (remise == null || remise < 0) {
                throw new IllegalArgumentException("Le montant de la remise doit être positif");
            }

            if (type == RemiseType.POURCENTAGE && remise > 100) {
                throw new IllegalArgumentException("Le pourcentage de remise ne peut pas dépasser 100%");
            }

            Double prixMaxRemise = ligne.getPrixUnitaire() * ligne.getQuantite();
            if (type == RemiseType.MONTANT_FIXE && remise > prixMaxRemise) {
                throw new IllegalArgumentException("La remise ne peut pas être supérieure au sous-total de la ligne (" + prixMaxRemise + ")");
            }

            if (type == RemiseType.POURCENTAGE) {
                ligne.appliquerRemisePourcentage(remise);
            } else {
                ligne.appliquerRemiseMontant(remise);
            }

            ligne.getVente().calculerTotal();
            venteRepository.save(ligne.getVente());

            return ligneVenteRepository.save(ligne);
        } catch (Exception e) {
            log.error("Erreur lors de l'application de la remise de ligne: {}", e.getMessage());
            throw new VenteException("Échec de l'application de la remise: " + e.getMessage(), e);
        }
    }

    private void validerRequeteVente(VenteRequest request) {
        if (request.getVendeurId() == null) {
            throw new IllegalArgumentException("L'ID du vendeur est requis");
        }

        if (request.getLignes() == null || request.getLignes().isEmpty()) {
            throw new IllegalArgumentException("La vente doit contenir au moins un produit");
        }

        if (request.getModePaiement() == null) {
            throw new IllegalArgumentException("Le mode de paiement est requis");
        }

        if (request.getModePaiement() != ModePaiement.ESPECES &&
                (request.getReferencePaiement() == null || request.getReferencePaiement().trim().isEmpty())) {
            throw new IllegalArgumentException("La référence de paiement est requise pour le mode: " + request.getModePaiement());
        }
    }

    private void validerRequeteCredit(VenteCreditRequest request) {
        if (request.getClientNom() == null || request.getClientNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du client est requis pour une vente à crédit");
        }
    }

    private void validerMontantReglement(ReglementCreditRequest request, Vente vente) {
        if (request.getMontantRegle() == null || request.getMontantRegle() <= 0) {
            throw new IllegalArgumentException("Le montant du règlement doit être supérieur à 0");
        }

        if (request.getMontantRegle() > vente.getMontantRestant()) {
            throw new IllegalArgumentException("Le montant réglé ne peut pas dépasser le montant restant (" +
                    vente.getMontantRestant() + ")");
        }
    }

    private Vente construireVente(VenteRequest request, Utilisateur vendeur) {
        Vente vente = new Vente();
        vente.setVendeur(vendeur);
        vente.setModePaiement(request.getModePaiement());
        vente.setReferencePaiement(request.getReferencePaiement());

        if (request.getRemiseGlobale() != null && request.getTypeRemiseGlobale() != null) {
            if (request.getTypeRemiseGlobale() == RemiseType.POURCENTAGE) {
                vente.appliquerRemiseGlobalePourcentage(request.getRemiseGlobale());
            } else if (request.getTypeRemiseGlobale() == RemiseType.MONTANT_FIXE) {
                vente.appliquerRemiseGlobaleMontant(request.getRemiseGlobale());
            }
        }

        for (LigneVenteRequest ligneRequest : request.getLignes()) {
            validerLigneVente(ligneRequest);
            LigneVente ligne = creerLigneVente(ligneRequest);
            vente.ajouterLigne(ligne);
        }

        vente.calculerTotal();

        return vente;
    }

    private void validerLigneVente(LigneVenteRequest ligneRequest) {
        if (ligneRequest.getProduitId() == null) {
            throw new IllegalArgumentException("L'ID du produit est requis");
        }

        if (ligneRequest.getQuantite() == null || ligneRequest.getQuantite() <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à 0 pour le produit ID: " + ligneRequest.getProduitId());
        }
    }

    private LigneVente creerLigneVente(LigneVenteRequest ligneRequest) {
        Produit produit = produitRepository.findById(ligneRequest.getProduitId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Produit non trouvé avec l'ID: " + ligneRequest.getProduitId()
                ));

        if (produit.getQuantite() < ligneRequest.getQuantite()) {
            throw new StockInsuffisantException(
                    "Stock insuffisant pour le produit: " + produit.getNom() +
                            ". Disponible: " + produit.getQuantite() + ", Demande: " + ligneRequest.getQuantite());
        }

        LigneVente ligne = new LigneVente();
        ligne.setProduit(produit);
        ligne.setQuantite(ligneRequest.getQuantite());
        ligne.setPrixUnitaire(produit.getPrixVente());

        if (ligneRequest.getRemisePourcentage() != null && ligneRequest.getRemisePourcentage() > 0) {
            ligne.appliquerRemisePourcentage(ligneRequest.getRemisePourcentage());
        } else if (ligneRequest.getRemiseMontant() != null && ligneRequest.getRemiseMontant() > 0) {
            ligne.appliquerRemiseMontant(ligneRequest.getRemiseMontant());
        } else {
            ligne.setSousTotal(produit.getPrixVente() * ligneRequest.getQuantite());
        }

        return ligne;
    }

    private void mettreAJourStockVente(Vente vente) {
        for (LigneVente ligne : vente.getLignes()) {
            try {
                inventaireService.sortieStock(
                        ligne.getProduit().getId(),
                        ligne.getQuantite(),
                        vente.getVendeur().getId(),
                        "Vente N°" + vente.getNumeroVente()
                );
            } catch (Exception e) {
                log.error("Erreur lors de la mise à jour du stock pour le produit {}: {}",
                        ligne.getProduit().getId(), e.getMessage());
                throw new VenteException("Erreur lors de la mise à jour du stock: " + e.getMessage(), e);
            }
        }
    }

    private void retablirStockAncienneVente(Vente vente) {
        for (LigneVente ligneExistante : vente.getLignes()) {
            try {
                inventaireService.entreeStock(
                        ligneExistante.getProduit().getId(),
                        ligneExistante.getQuantite(),
                        vente.getVendeur().getId(),
                        "Annulation modification vente N°" + vente.getNumeroVente()
                );
            } catch (Exception e) {
                log.error("Erreur lors du rétablissement du stock pour le produit {}: {}",
                        ligneExistante.getProduit().getId(), e.getMessage());
                throw new VenteException("Erreur lors du rétablissement du stock: " + e.getMessage(), e);
            }
        }
    }

    private void mettreAJourInformationsVente(Vente vente, VenteRequest request, Utilisateur vendeur) {
        vente.setVendeur(vendeur);
        vente.setModePaiement(request.getModePaiement());
        vente.setReferencePaiement(request.getReferencePaiement());

        vente.setRemiseGlobale(0.0);
        vente.setTypeRemiseGlobale(null);

        if (request.getRemiseGlobale() != null && request.getTypeRemiseGlobale() != null) {
            if (request.getTypeRemiseGlobale() == RemiseType.POURCENTAGE) {
                vente.appliquerRemiseGlobalePourcentage(request.getRemiseGlobale());
            } else if (request.getTypeRemiseGlobale() == RemiseType.MONTANT_FIXE) {
                vente.appliquerRemiseGlobaleMontant(request.getRemiseGlobale());
            }
        }
    }

    private void ajouterNouvellesLignesVente(Vente vente, List<LigneVenteRequest> lignesRequest) {
        for (LigneVenteRequest ligneRequest : lignesRequest) {
            validerLigneVente(ligneRequest);

            Produit produit = produitRepository.findById(ligneRequest.getProduitId())
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Produit non trouvé avec l'ID: " + ligneRequest.getProduitId()
                    ));

            if (produit.getQuantite() < ligneRequest.getQuantite()) {
                throw new StockInsuffisantException(
                        "Stock insuffisant pour le produit: " + produit.getNom() +
                                ". Disponible: " + produit.getQuantite() + ", Demande: " + ligneRequest.getQuantite());
            }

            LigneVente nouvelleLigne = new LigneVente();
            nouvelleLigne.setProduit(produit);
            nouvelleLigne.setQuantite(ligneRequest.getQuantite());
            nouvelleLigne.setPrixUnitaire(produit.getPrixVente());

            if (ligneRequest.getRemisePourcentage() != null && ligneRequest.getRemisePourcentage() > 0) {
                nouvelleLigne.appliquerRemisePourcentage(ligneRequest.getRemisePourcentage());
            } else if (ligneRequest.getRemiseMontant() != null && ligneRequest.getRemiseMontant() > 0) {
                nouvelleLigne.appliquerRemiseMontant(ligneRequest.getRemiseMontant());
            } else {
                nouvelleLigne.setSousTotal(produit.getPrixVente() * ligneRequest.getQuantite());
            }

            vente.ajouterLigne(nouvelleLigne);
        }
    }

    private void enregistrerVenteEnCaisse(Vente vente, VenteRequest request) {
        try {
            caisseService.enregistrerVente(
                    vente,
                    request.getVendeurId(),
                    request.getModePaiement().toString(),
                    request.getReferencePaiement()
            );
            log.info("Vente enregistrée en caisse - Montant: {}", vente.getMontantTotal());
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement en caisse: {}", e.getMessage());
            throw new VenteException("Échec de l'enregistrement en caisse: " + e.getMessage(), e);
        }
    }

    private void enregistrerCreditEnCaisse(Vente vente, VenteCreditRequest request, LocalDate dateEcheance) {
        try {
            OperationCaisse operationCredit = caisseService.enregistrerVenteCredit(
                    vente,
                    request.getVendeurId(),
                    request.getClientNom(),
                    request.getClientTelephone(),
                    dateEcheance
            );
            log.info("Vente à crédit enregistrée - ID Opération Caisse: {}, Échéance: {}",
                    operationCredit.getId(), dateEcheance);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du crédit en caisse: {}", e.getMessage());
            throw new VenteException("Échec de l'enregistrement du crédit en caisse: " + e.getMessage(), e);
        }
    }

    private void enregistrerReglementEnCaisse(Long venteId, ReglementCreditRequest request, String modePaiement) {
        try {
            OperationCaisse reglementOperation = caisseService.reglementCredit(
                    venteId,
                    request.getMontantRegle(),
                    request.getUtilisateurId(),
                    modePaiement,
                    request.getReferencePaiement()
            );
            log.info("Règlement enregistré en caisse - ID Opération: {}", reglementOperation.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du règlement en caisse: {}", e.getMessage());
            throw new VenteException("Erreur lors de l'enregistrement du règlement: " + e.getMessage(), e);
        }
    }
}