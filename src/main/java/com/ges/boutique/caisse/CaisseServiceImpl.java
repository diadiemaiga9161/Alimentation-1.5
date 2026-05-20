package com.ges.boutique.caisse;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.exception.SoldeInsuffisantException;
import com.ges.boutique.client.Client;
import com.ges.boutique.client.ClientRepository;
import com.ges.boutique.facture.Facture;
import com.ges.boutique.facture.FactureRepository;
import com.ges.boutique.facture.LigneFacture;
import com.ges.boutique.facture.LigneFactureRepository;
import com.ges.boutique.facture.FactureRequest;
import com.ges.boutique.facture.LigneFactureRequest;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import com.ges.boutique.vente.Vente;
import com.ges.boutique.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaisseServiceImpl implements CaisseService {

    private final CaisseRepository caisseRepository;
    private final OperationCaisseRepository operationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final VenteRepository venteRepository;
    private final FactureRepository factureRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;

    // ==================== GESTION DES CAISSES ====================

    @Override
    @Transactional
    public Caisse creerCaisse(String numeroCaisse) {
        Caisse caisse = new Caisse();
        if (numeroCaisse != null && !numeroCaisse.trim().isEmpty()) {
            if (caisseRepository.existsByNumeroCaisse(numeroCaisse)) {
                throw new RuntimeException("Une caisse avec ce numéro existe déjà: " + numeroCaisse);
            }
            caisse.setNumeroCaisse(numeroCaisse);
        }
        caisse.setSoldeActuel(0.0);
        caisse.setSoldeInitial(0.0);
        caisse.setSoldeSysteme(0.0);
        caisse.setSoldeReel(0.0);
        caisse.setEcart(0.0);
        caisse.setTotalEntrees(0.0);
        caisse.setTotalSorties(0.0);
        caisse.setEstOuverte(false);
        caisse.setNombreOperations(0);
        caisse.setVerifiee(false);
        return caisseRepository.save(caisse);
    }

    @Override
    @Transactional
    public Caisse ouvrirCaisse() {
        Optional<Caisse> caisseOuverte = caisseRepository.findCaisseOuverte();
        if (caisseOuverte.isPresent()) {
            log.info("Caisse déjà ouverte: {}", caisseOuverte.get().getNumeroCaisse());
            return caisseOuverte.get();
        }

        try {
            Optional<Caisse> derniereCaisse = caisseRepository.findFirstByOrderByIdDesc();
            Caisse caisse;

            if (derniereCaisse.isPresent() && !derniereCaisse.get().isEstOuverte()) {
                caisse = derniereCaisse.get();
                caisse.setEstOuverte(true);
                caisse.setDateOuverture(LocalDateTime.now());
                caisse.setDerniereOperation(LocalDateTime.now());
                caisse.setVerifiee(false);
                caisse.setSoldeSysteme(caisse.getSoldeActuel());
                caisse.setSoldeReel(caisse.getSoldeActuel());
                log.info("Réouverture de la caisse existante: {}", caisse.getNumeroCaisse());
            } else {
                caisse = new Caisse();
                caisse.setSoldeActuel(0.0);
                caisse.setSoldeInitial(0.0);
                caisse.setSoldeSysteme(0.0);
                caisse.setSoldeReel(0.0);
                caisse.setEcart(0.0);
                caisse.setTotalEntrees(0.0);
                caisse.setTotalSorties(0.0);
                caisse.setEstOuverte(true);
                caisse.setDateOuverture(LocalDateTime.now());
                caisse.setDerniereOperation(LocalDateTime.now());
                caisse.setNombreOperations(0);
                caisse.setVerifiee(false);
                log.info("Création d'une nouvelle caisse");
            }

            Caisse savedCaisse = caisseRepository.save(caisse);

            OperationCaisse operation = new OperationCaisse();
            operation.setCaisse(savedCaisse);
            operation.setType(TypeOperationCaisse.OUVERTURE);
            operation.setMontant(savedCaisse.getSoldeActuel());
            operation.setSoldeAvant(savedCaisse.getSoldeActuel());
            operation.setSoldeApres(savedCaisse.getSoldeActuel());
            operation.setMotif("Ouverture de caisse - " + savedCaisse.getNumeroCaisse());
            operation.setDateOperation(LocalDateTime.now());
            operation.setEstReglee(true);
            operationRepository.save(operation);

            return savedCaisse;

        } catch (Exception e) {
            log.error("Erreur lors de l'ouverture de la caisse: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible d'ouvrir la caisse: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Caisse ouvrirCaisse(Long caisseId) {
        Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new RessourceIntrouvableException("Caisse non trouvée avec l'ID: " + caisseId));

        if (caisse.isEstOuverte()) {
            log.info("Caisse déjà ouverte: {}", caisse.getNumeroCaisse());
            return caisse;
        }

        caisse.setEstOuverte(true);
        caisse.setDateOuverture(LocalDateTime.now());
        caisse.setDerniereOperation(LocalDateTime.now());
        caisse.setVerifiee(false);

        Caisse savedCaisse = caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
        operation.setCaisse(savedCaisse);
        operation.setType(TypeOperationCaisse.OUVERTURE);
        operation.setMontant(savedCaisse.getSoldeActuel());
        operation.setSoldeAvant(savedCaisse.getSoldeActuel());
        operation.setSoldeApres(savedCaisse.getSoldeActuel());
        operation.setMotif("Ouverture de caisse - " + savedCaisse.getNumeroCaisse());
        operation.setDateOperation(LocalDateTime.now());
        operation.setEstReglee(true);
        operationRepository.save(operation);

        return savedCaisse;
    }

    @Override
    @Transactional
    public Caisse fermerCaisse(Long utilisateurId) {
        Caisse caisse = getCaisseOuverte();
        return fermerCaisse(caisse.getId(), utilisateurId);
    }

    @Override
    @Transactional
    public Caisse fermerCaisse(Long caisseId, Long utilisateurId) {
        Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new RessourceIntrouvableException("Caisse non trouvée avec l'ID: " + caisseId));

        if (!caisse.isEstOuverte()) {
            throw new IllegalStateException("La caisse est déjà fermée");
        }

        caisse.setEstOuverte(false);
        caisse.setDateFermeture(LocalDateTime.now());
        caisse.setDerniereOperation(LocalDateTime.now());
        caisse.mettreAJourSoldeSysteme();

        Long nombreOps = operationRepository.countOperationsDuJourByCaisseId(caisseId);
        caisse.setNombreOperations(nombreOps != null ? nombreOps.intValue() : 0);

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(u ->
                    caisse.setUtilisateurVerification(u.getNomComplet()));
        }

        Double soldeAvantFermeture = caisse.getSoldeActuel();
        caisse.setSoldeActuel(0.0);
        caisse.setSoldeInitial(0.0);
        caisse.setSoldeSysteme(0.0);
        caisse.setSoldeReel(0.0);
        caisse.setEcart(0.0);
        caisse.setTotalEntrees(0.0);
        caisse.setTotalSorties(0.0);

        log.info("Fermeture de caisse - Solde avant fermeture: {}, Solde réinitialisé à 0", soldeAvantFermeture);

        Caisse savedCaisse = caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
        operation.setCaisse(savedCaisse);
        operation.setType(TypeOperationCaisse.FERMETURE);
        operation.setMontant(soldeAvantFermeture);
        operation.setSoldeAvant(soldeAvantFermeture);
        operation.setSoldeApres(0.0);
        operation.setMotif("Fermeture de caisse - " + caisse.getNumeroCaisse() + " - Montant final: " + soldeAvantFermeture);
        operation.setDateOperation(LocalDateTime.now());
        operation.setEstReglee(true);
        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }
        operationRepository.save(operation);

        return savedCaisse;
    }

    @Override
    public Caisse getCaisseOuverte() {
        return caisseRepository.findCaisseOuverte()
                .orElseThrow(() -> new IllegalStateException("Aucune caisse n'est ouverte. Veuillez ouvrir une caisse"));
    }

    @Override
    public Caisse getCaisseOuverte(Long caisseId) {
        Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new RessourceIntrouvableException("Caisse non trouvée avec l'ID: " + caisseId));
        if (!caisse.isEstOuverte()) {
            throw new IllegalStateException("La caisse " + caisse.getNumeroCaisse() + " n'est pas ouverte");
        }
        return caisse;
    }

    @Override
    public List<Caisse> obtenirToutesCaisses() {
        return caisseRepository.findAll();
    }

    @Override
    public boolean isCaisseOuverte() {
        return caisseRepository.findCaisseOuverte().isPresent();
    }

    // ==================== SOLDES ET VÉRIFICATIONS ====================

    @Override
    public Double getSoldeActuel() {
        return getCaisseOuverte().getSoldeActuel();
    }

    @Override
    public Double getSoldeSysteme() {
        return getCaisseOuverte().getSoldeSysteme();
    }

    @Override
    @Transactional
    public Caisse verifierCaisse(Double soldeReelSaisi, Long utilisateurId, String observations) {
        Caisse caisse = getCaisseOuverte();
        caisse.verifierCaisse(soldeReelSaisi, utilisateurId != null ? utilisateurId.toString() : "Système");
        caisse.mettreAJourSoldeSysteme();

        Caisse savedCaisse = caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
        operation.setCaisse(savedCaisse);
        operation.setType(TypeOperationCaisse.VERIFICATION);
        operation.setMontant(Math.abs(caisse.getEcart()));
        operation.setSoldeAvant(caisse.getSoldeSysteme());
        operation.setSoldeApres(caisse.getSoldeSysteme());
        operation.setMotif("Vérification de caisse - Écart: " + caisse.getEcart() +
                (observations != null ? " (" + observations + ")" : ""));
        operation.setDateOperation(LocalDateTime.now());
        operation.setEstReglee(true);
        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }
        operationRepository.save(operation);

        return savedCaisse;
    }

    @Override
    public Map<String, Object> getEcartCaisse() {
        Caisse caisse = getCaisseOuverte();
        Map<String, Object> ecart = new HashMap<>();
        ecart.put("soldeSysteme", caisse.getSoldeSysteme());
        ecart.put("soldeReel", caisse.getSoldeReel());
        ecart.put("ecart", caisse.getEcart());
        ecart.put("verifiee", caisse.isVerifiee());
        ecart.put("dateVerification", caisse.getDateVerification());
        return ecart;
    }

    // ==================== OPÉRATIONS DE CAISSE ====================

    @Override
    @Transactional
    public OperationCaisse entreeCaisse(Double montant, String motif, Long utilisateurId,
                                        String modePaiement, String reference) {
        if (montant == null || montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0");
        }

        verifierEtOuvrirCaisseSiNecessaire();
        Caisse caisse = getCaisseOuverte();
        Double soldeAvant = caisse.getSoldeActuel();
        caisse.setSoldeActuel(soldeAvant + montant);
        caisse.setTotalEntrees(caisse.getTotalEntrees() + montant);
        caisse.setDerniereOperation(LocalDateTime.now());
        caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
        operation.setCaisse(caisse);
        operation.setType(TypeOperationCaisse.ENTREE);
        operation.setMontant(montant);
        operation.setSoldeAvant(soldeAvant);
        operation.setSoldeApres(caisse.getSoldeActuel());
        operation.setMotif(motif);
        operation.setEstReglee(true);
        operation.setDateOperation(LocalDateTime.now());

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }

        if (modePaiement != null) {
            try {
                operation.setModePaiement(ModePaiementCaisse.valueOf(modePaiement));
            } catch (IllegalArgumentException e) {
                operation.setModePaiement(ModePaiementCaisse.ESPECES);
            }
        }
        operation.setReferencePaiement(reference);

        return operationRepository.save(operation);
    }

    @Override
    @Transactional
    public OperationCaisse sortieCaisse(Double montant, String motif, Long utilisateurId) {
        if (montant == null || montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0");
        }

        verifierEtOuvrirCaisseSiNecessaire();
        Caisse caisse = getCaisseOuverte();

        if (caisse.getSoldeActuel() < montant) {
            throw new SoldeInsuffisantException(
                    "Solde insuffisant. Disponible: " + caisse.getSoldeActuel() +
                            ", Demandé: " + montant);
        }

        Double soldeAvant = caisse.getSoldeActuel();

        caisse.setSoldeActuel(soldeAvant - montant);
        caisse.setTotalSorties(caisse.getTotalSorties() + montant);
        caisse.setDerniereOperation(LocalDateTime.now());
        caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
        operation.setCaisse(caisse);
        operation.setType(TypeOperationCaisse.SORTIE);
        operation.setMontant(montant);
        operation.setSoldeAvant(soldeAvant);
        operation.setSoldeApres(caisse.getSoldeActuel());
        operation.setMotif(motif);
        operation.setEstReglee(true);
        operation.setDateOperation(LocalDateTime.now());

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }

        return operationRepository.save(operation);
    }

    @Override
    @Transactional
    public OperationCaisse enregistrerVente(Vente vente, Long utilisateurId,
                                            String modePaiement, String reference) {
        if (vente == null) {
            throw new IllegalArgumentException("La vente ne peut pas être nulle");
        }

        verifierEtOuvrirCaisseSiNecessaire();
        Caisse caisse = getCaisseOuverte();
        Double soldeAvant = caisse.getSoldeActuel();

        caisse.setSoldeActuel(soldeAvant + vente.getMontantTotal());
        caisse.setTotalEntrees(caisse.getTotalEntrees() + vente.getMontantTotal());
        caisse.setDerniereOperation(LocalDateTime.now());
        caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
        operation.setCaisse(caisse);
        operation.setType(TypeOperationCaisse.VENTE_COMPTANT);
        operation.setMontant(vente.getMontantTotal());
        operation.setSoldeAvant(soldeAvant);
        operation.setSoldeApres(caisse.getSoldeActuel());
        operation.setMotif("Vente N°" + vente.getNumeroVente());
        operation.setVente(vente);
        operation.setEstReglee(true);
        operation.setDateOperation(LocalDateTime.now());

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }

        if (modePaiement != null) {
            try {
                operation.setModePaiement(ModePaiementCaisse.valueOf(modePaiement));
            } catch (IllegalArgumentException e) {
                operation.setModePaiement(ModePaiementCaisse.ESPECES);
            }
        }
        operation.setReferencePaiement(reference);

        return operationRepository.save(operation);
    }

    @Override
    @Transactional
    public OperationCaisse enregistrerVenteCredit(Vente vente, Long utilisateurId,
                                                  String clientNom, String clientTelephone,
                                                  LocalDate dateEcheance) {
        if (vente == null) {
            throw new IllegalArgumentException("La vente ne peut pas être nulle");
        }

        verifierEtOuvrirCaisseSiNecessaire();
        Caisse caisse = getCaisseOuverte();
        Double soldeAvant = caisse.getSoldeActuel();
        Double montantVerseInitial = vente.getMontantVerse() != null ? vente.getMontantVerse() : 0.0;
        Double montantRestant = Math.max(0.0, vente.getMontantTotal() - montantVerseInitial);

        OperationCaisse operation = new OperationCaisse();
        operation.setCaisse(caisse);
        operation.setType(TypeOperationCaisse.VENTE_CREDIT);
        operation.setMontant(vente.getMontantTotal());
        operation.setSoldeAvant(soldeAvant);
        operation.setSoldeApres(soldeAvant);
        operation.setMotif("Vente à crédit N°" + vente.getNumeroVente());
        operation.setVente(vente);
        operation.setEstReglee(montantRestant <= 0);
        operation.setClientNom(clientNom != null ? clientNom : vente.getClientNom());
        operation.setClientTelephone(clientTelephone != null ? clientTelephone : vente.getClientTelephone());
        operation.setDateOperation(LocalDateTime.now());
        operation.setMontantVerse(montantVerseInitial);
        operation.setMontantRestant(montantRestant);
        operation.setVenteCreditId(vente.getId());

        if (dateEcheance != null) {
            operation.setDateEcheance(dateEcheance.atTime(LocalTime.MAX));
        } else {
            operation.setDateEcheance(LocalDateTime.now().plusDays(30));
        }

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }

        return operationRepository.save(operation);
    }

    @Override
    @Transactional
    public OperationCaisse reglementCredit(Long venteCreditId, Double montantRegle,
                                           Long utilisateurId, String modePaiement,
                                           String reference) {
        Vente vente = venteRepository.findById(venteCreditId)
                .orElseThrow(() -> new RessourceIntrouvableException("Vente non trouvée avec l'ID: " + venteCreditId));

        if (!Boolean.TRUE.equals(vente.getEstCredit())) {
            throw new IllegalArgumentException("La vente avec l'ID " + venteCreditId + " n'est pas un crédit");
        }

        if (Boolean.TRUE.equals(vente.getCreditRegle())) {
            throw new IllegalStateException("Ce crédit est déjà totalement réglé");
        }

        if (Boolean.TRUE.equals(vente.getAnnulee())) {
            throw new IllegalStateException("Ce crédit a été annulé et ne peut plus être réglé");
        }

        if (montantRegle == null || montantRegle <= 0) {
            throw new IllegalArgumentException("Le montant réglé doit être supérieur à 0");
        }

        Double montantRestantActuel = vente.getMontantRestant();
        if (montantRegle > montantRestantActuel) {
            throw new IllegalArgumentException("Le montant réglé ne peut pas dépasser le montant restant (" +
                    montantRestantActuel + ")");
        }

        verifierEtOuvrirCaisseSiNecessaire();
        Caisse caisse = getCaisseOuverte();
        Double soldeAvant = caisse.getSoldeActuel();

        caisse.setSoldeActuel(soldeAvant + montantRegle);
        caisse.setTotalEntrees(caisse.getTotalEntrees() + montantRegle);
        caisse.setDerniereOperation(LocalDateTime.now());
        caisseRepository.save(caisse);

        Double nouveauMontantVerse = vente.getMontantVerse() + montantRegle;
        Double nouveauMontantRestant = vente.getMontantRestant() - montantRegle;

        vente.setMontantVerse(nouveauMontantVerse);
        vente.setMontantRestant(nouveauMontantRestant);

        if (nouveauMontantRestant <= 0) {
            vente.setCreditRegle(true);
            vente.setDateReglement(LocalDate.now());
        }
        venteRepository.save(vente);

        Optional<OperationCaisse> creditOperationOpt = operationRepository.findFirstByVenteIdAndType(
                vente.getId(), TypeOperationCaisse.VENTE_CREDIT);

        if (creditOperationOpt.isPresent()) {
            OperationCaisse creditOperation = creditOperationOpt.get();
            creditOperation.setMontantVerse(nouveauMontantVerse);
            creditOperation.setMontantRestant(nouveauMontantRestant);
            creditOperation.setEstReglee(nouveauMontantRestant <= 0);
            operationRepository.save(creditOperation);
        }

        OperationCaisse reglementOperation = new OperationCaisse();
        reglementOperation.setCaisse(caisse);
        reglementOperation.setType(TypeOperationCaisse.REGLEMENT_CREDIT);
        reglementOperation.setMontant(montantRegle);
        reglementOperation.setSoldeAvant(soldeAvant);
        reglementOperation.setSoldeApres(caisse.getSoldeActuel());
        reglementOperation.setMotif("Règlement crédit - Vente N°" + vente.getNumeroVente());
        reglementOperation.setVente(vente);
        reglementOperation.setEstReglee(true);
        reglementOperation.setDateOperation(LocalDateTime.now());
        reglementOperation.setMontantVerse(montantRegle);
        reglementOperation.setMontantRestant(nouveauMontantRestant);
        reglementOperation.setVenteCreditId(venteCreditId);
        reglementOperation.setClientNom(vente.getClientNom());
        reglementOperation.setClientTelephone(vente.getClientTelephone());

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(reglementOperation::setUtilisateur);
        }

        if (modePaiement != null) {
            try {
                reglementOperation.setModePaiement(ModePaiementCaisse.valueOf(modePaiement));
            } catch (IllegalArgumentException e) {
                reglementOperation.setModePaiement(ModePaiementCaisse.ESPECES);
            }
        }
        reglementOperation.setReferencePaiement(reference);

        return operationRepository.save(reglementOperation);
    }

    @Override
    @Transactional
    public OperationCaisse annulerVente(Vente vente, Long utilisateurId, String motif) {
        return annulerVenteAvecRepercussion(vente, utilisateurId, motif);
    }

    @Override
    @Transactional
    public OperationCaisse annulerVenteCredit(Vente vente, Long utilisateurId, String motif) {
        return annulerVenteCreditAvecRepercussion(vente, utilisateurId, motif);
    }

    @Override
    @Transactional
    public OperationCaisse annulerVenteAvecRepercussion(Vente vente, Long utilisateurId, String motif) {
        log.info("=== ANNULATION VENTE AVEC RÉPERCUSSION CAISSE ===");

        if (Boolean.TRUE.equals(vente.getEstCredit())) {
            return annulerVenteCreditAvecRepercussion(vente, utilisateurId, motif);
        }

        if (Boolean.TRUE.equals(vente.getAnnulee())) {
            log.warn("La vente {} est déjà annulée", vente.getNumeroVente());
            Optional<OperationCaisse> existingOp = operationRepository.findOperationVenteByVenteIdAndType(
                    vente.getId(), TypeOperationCaisse.ANNULATION_VENTE);
            if (existingOp.isPresent()) {
                return existingOp.get();
            }
            throw new IllegalStateException("Cette vente est déjà annulée");
        }

        verifierEtOuvrirCaisseSiNecessaire();
        Caisse caisse = getCaisseOuverte();

        Optional<OperationCaisse> venteOperationOpt = operationRepository.findOperationVenteByVenteIdAndType(
                vente.getId(), TypeOperationCaisse.VENTE_COMPTANT);

        if (venteOperationOpt.isEmpty()) {
            throw new IllegalStateException("Opération de vente non trouvée en caisse");
        }

        OperationCaisse venteOperation = venteOperationOpt.get();

        Double soldeAvant = caisse.getSoldeActuel();
        Double montantVente = vente.getMontantTotal();

        if (caisse.getSoldeActuel() < montantVente) {
            throw new SoldeInsuffisantException("Solde insuffisant pour annuler la vente");
        }

        caisse.setSoldeActuel(soldeAvant - montantVente);
        caisse.setTotalSorties(caisse.getTotalSorties() + montantVente);
        caisse.setDerniereOperation(LocalDateTime.now());
        caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
        operation.setCaisse(caisse);
        operation.setType(TypeOperationCaisse.ANNULATION_VENTE);
        operation.setMontant(montantVente);
        operation.setSoldeAvant(soldeAvant);
        operation.setSoldeApres(caisse.getSoldeActuel());
        operation.setMotif(motif != null ? "ANNULATION VENTE " + vente.getNumeroVente() + " - " + motif :
                "Annulation vente N°" + vente.getNumeroVente());
        operation.setVente(vente);
        operation.setEstReglee(true);
        operation.setDateOperation(LocalDateTime.now());
        operation.setVenteAnnulee(true);

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }

        OperationCaisse savedOperation = operationRepository.save(operation);

        venteOperation.setVenteAnnulee(true);
        operationRepository.save(venteOperation);

        return savedOperation;
    }

    @Override
    @Transactional
    public OperationCaisse annulerVenteCreditAvecRepercussion(Vente vente, Long utilisateurId, String motif) {
        log.info("=== ANNULATION CRÉDIT AVEC RÉPERCUSSION CAISSE ===");

        if (!Boolean.TRUE.equals(vente.getEstCredit())) {
            return annulerVenteAvecRepercussion(vente, utilisateurId, motif);
        }

        if (Boolean.TRUE.equals(vente.getAnnulee())) {
            log.warn("Le crédit {} est déjà annulé", vente.getNumeroVente());
            Optional<OperationCaisse> existingOp = operationRepository.findFirstByVenteIdAndType(
                    vente.getId(), TypeOperationCaisse.ANNULATION_CREDIT);
            if (existingOp.isPresent()) {
                return existingOp.get();
            }
            throw new IllegalStateException("Ce crédit est déjà annulé");
        }

        verifierEtOuvrirCaisseSiNecessaire();
        Caisse caisse = getCaisseOuverte();

        Optional<OperationCaisse> creditOperationOpt = operationRepository.findFirstByVenteIdAndType(
                vente.getId(), TypeOperationCaisse.VENTE_CREDIT);

        if (creditOperationOpt.isEmpty()) {
            throw new IllegalStateException("Opération de crédit non trouvée en caisse");
        }

        OperationCaisse creditOperation = creditOperationOpt.get();

        Double soldeAvant = caisse.getSoldeActuel();

        OperationCaisse operation = new OperationCaisse();
        operation.setCaisse(caisse);
        operation.setType(TypeOperationCaisse.ANNULATION_CREDIT);
        operation.setMontant(vente.getMontantTotal());
        operation.setSoldeAvant(soldeAvant);
        operation.setSoldeApres(soldeAvant);
        operation.setMotif(motif != null ? "ANNULATION CREDIT " + vente.getNumeroVente() + " - " + motif :
                "Annulation crédit N°" + vente.getNumeroVente());
        operation.setVente(vente);
        operation.setEstReglee(true);
        operation.setDateOperation(LocalDateTime.now());
        operation.setClientNom(vente.getClientNom());
        operation.setClientTelephone(vente.getClientTelephone());
        operation.setVenteCreditId(vente.getId());
        operation.setMontantVerse(vente.getMontantVerse());
        operation.setMontantRestant(vente.getMontantRestant());
        operation.setVenteAnnulee(true);

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }

        OperationCaisse savedOperation = operationRepository.save(operation);

        creditOperation.setVenteAnnulee(true);
        operationRepository.save(creditOperation);

        List<OperationCaisse> reglements = operationRepository.findReglementsByVenteCredit(vente.getId());
        for (OperationCaisse reglement : reglements) {
            reglement.setVenteAnnulee(true);
            operationRepository.save(reglement);
        }

        return savedOperation;
    }

    // ==================== GESTION DES CRÉDITS ====================

    @Override
    public List<OperationCaisse> getCreditsNonRegles() {
        List<OperationCaisse> credits = operationRepository.findCreditsNonRegles(TypeOperationCaisse.VENTE_CREDIT);
        List<OperationCaisse> creditsFiltres = new ArrayList<>();
        for (OperationCaisse credit : credits) {
            if (credit.getVente() != null && Boolean.TRUE.equals(credit.getVente().getAnnulee())) {
                continue;
            }
            if (credit.getVente() != null) {
                credit.setMontantVerse(credit.getVente().getMontantVerse());
                credit.setMontantRestant(credit.getVente().getMontantRestant());
                credit.setEstReglee(Boolean.TRUE.equals(credit.getVente().getCreditRegle()));
            }
            creditsFiltres.add(credit);
        }
        return creditsFiltres;
    }

    @Override
    public List<OperationCaisse> getCreditsEnRetard() {
        List<OperationCaisse> credits = operationRepository.findCreditsEnRetard(LocalDateTime.now());
        List<OperationCaisse> creditsFiltres = new ArrayList<>();
        for (OperationCaisse credit : credits) {
            if (credit.getVente() != null && Boolean.TRUE.equals(credit.getVente().getAnnulee())) {
                continue;
            }
            if (credit.getVente() != null) {
                credit.setMontantVerse(credit.getVente().getMontantVerse());
                credit.setMontantRestant(credit.getVente().getMontantRestant());
                credit.setEstReglee(Boolean.TRUE.equals(credit.getVente().getCreditRegle()));
            }
            creditsFiltres.add(credit);
        }
        return creditsFiltres;
    }

    @Override
    public Map<String, Object> getSituationCredits() {
        Map<String, Object> situation = new HashMap<>();
        List<OperationCaisse> creditsNonRegles = getCreditsNonRegles();
        List<OperationCaisse> creditsEnRetard = getCreditsEnRetard();

        Double montantTotalCredits = creditsNonRegles.stream().mapToDouble(OperationCaisse::getMontant).sum();
        Double montantRestantTotal = creditsNonRegles.stream().mapToDouble(c -> c.getMontantRestant() != null ? c.getMontantRestant() : c.getMontant()).sum();
        Double montantTotalRetard = creditsEnRetard.stream().mapToDouble(c -> c.getMontantRestant() != null ? c.getMontantRestant() : c.getMontant()).sum();

        situation.put("nombreCreditsNonRegles", creditsNonRegles.size());
        situation.put("montantTotalCredits", arrondir(montantTotalCredits));
        situation.put("montantRestantTotal", arrondir(montantRestantTotal));
        situation.put("nombreCreditsEnRetard", creditsEnRetard.size());
        situation.put("montantTotalRetard", arrondir(montantTotalRetard));

        return situation;
    }

    @Override
    public List<OperationCaisse> getHistoriqueReglementsCredit(Long venteCreditId) {
        return operationRepository.findReglementsByVenteCredit(venteCreditId);
    }

    // ==================== OPÉRATIONS ====================

    @Override
    public List<OperationCaisse> getOperationsDuJour() {
        return operationRepository.findOperationsDuJour().stream()
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();
    }

    @Override
    public List<OperationCaisse> getOperationsDeLaSemaine() {
        LocalDateTime debutSemaine = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime finSemaine = LocalDate.now().with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
        return operationRepository.findOperationsParPeriode(debutSemaine, finSemaine).stream()
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();
    }

    @Override
    public List<OperationCaisse> getOperationsDuMois() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDateTime debutMois = aujourdhui.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMois = aujourdhui.withDayOfMonth(aujourdhui.lengthOfMonth()).atTime(LocalTime.MAX);
        return operationRepository.findOperationsParPeriode(debutMois, finMois).stream()
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();
    }

    @Override
    public List<OperationCaisse> getOperationsDeLAnnee() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDateTime debutAnnee = aujourdhui.withDayOfYear(1).atStartOfDay();
        LocalDateTime finAnnee = aujourdhui.withDayOfYear(aujourdhui.lengthOfYear()).atTime(LocalTime.MAX);
        return operationRepository.findOperationsParPeriode(debutAnnee, finAnnee).stream()
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();
    }

    @Override
    public List<OperationCaisse> getOperationsParPeriode(LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);
        return operationRepository.findOperationsParPeriode(debut, fin).stream()
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();
    }

    // ==================== STATISTIQUES ====================

    @Override
    public Map<String, Object> getStatistiquesDuJour() {
        return getStatistiquesParPeriode(LocalDate.now(), LocalDate.now());
    }

    @Override
    public Map<String, Object> getStatistiquesDeLaSemaine() {
        LocalDate debutSemaine = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate finSemaine = LocalDate.now().with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        return getStatistiquesParPeriode(debutSemaine, finSemaine);
    }

    @Override
    public Map<String, Object> getStatistiquesDuMois() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutMois = aujourdhui.withDayOfMonth(1);
        LocalDate finMois = aujourdhui.withDayOfMonth(aujourdhui.lengthOfMonth());
        return getStatistiquesParPeriode(debutMois, finMois);
    }

    @Override
    public Map<String, Object> getStatistiquesDeLAnnee() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutAnnee = aujourdhui.withDayOfYear(1);
        LocalDate finAnnee = aujourdhui.withDayOfYear(aujourdhui.lengthOfYear());
        return getStatistiquesParPeriode(debutAnnee, finAnnee);
    }

    @Override
    public Map<String, Object> getStatistiquesParPeriode(LocalDate dateDebut, LocalDate dateFin) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);

        List<OperationCaisse> operations = operationRepository.findOperationsParPeriode(debut, fin).stream()
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();

        Double totalVentesComptant = operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.VENTE_COMPTANT, debut, fin);
        Double totalVentesCredit = operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.VENTE_CREDIT, debut, fin);
        Double totalReglementsCredit = operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.REGLEMENT_CREDIT, debut, fin);
        Double totalEntrees = operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.ENTREE, debut, fin);
        Double totalSorties = operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.SORTIE, debut, fin);

        Map<LocalDate, Double> chiffreParJour = new HashMap<>();
        Map<LocalDate, Integer> nombreOperationsParJour = new HashMap<>();

        for (OperationCaisse op : operations) {
            LocalDate date = op.getDateOperation().toLocalDate();
            chiffreParJour.merge(date, op.getMontant(), Double::sum);
            nombreOperationsParJour.merge(date, 1, Integer::sum);
        }

        Double totalEntreesCaisse = (totalVentesComptant != null ? totalVentesComptant : 0) +
                (totalReglementsCredit != null ? totalReglementsCredit : 0) +
                (totalEntrees != null ? totalEntrees : 0);

        Double totalSortiesCaisse = totalSorties != null ? totalSorties : 0;

        stats.put("periode", Map.of("debut", dateDebut, "fin", dateFin, "nbJours", ChronoUnit.DAYS.between(dateDebut, dateFin) + 1));
        stats.put("totalVentesComptant", arrondir(totalVentesComptant != null ? totalVentesComptant : 0));
        stats.put("totalNouveauxCredits", arrondir(totalVentesCredit != null ? totalVentesCredit : 0));
        stats.put("totalReglementsCredit", arrondir(totalReglementsCredit != null ? totalReglementsCredit : 0));
        stats.put("totalAutresEntrees", arrondir(totalEntrees != null ? totalEntrees : 0));
        stats.put("totalSorties", arrondir(totalSorties != null ? totalSorties : 0));
        stats.put("totalEntrees", arrondir(totalEntreesCaisse));
        stats.put("soldeNetPeriode", arrondir(totalEntreesCaisse - totalSortiesCaisse));
        stats.put("nombreOperations", operations.size());
        stats.put("moyenneJournaliere", arrondir(totalEntreesCaisse / (ChronoUnit.DAYS.between(dateDebut, dateFin) + 1)));
        stats.put("chiffreParJour", chiffreParJour);
        stats.put("operationsParJour", nombreOperationsParJour);

        Map<String, Double> parModePaiement = new HashMap<>();
        for (OperationCaisse op : operations) {
            if (op.getModePaiement() != null) {
                parModePaiement.merge(op.getModePaiement().toString(), op.getMontant(), Double::sum);
            }
        }
        stats.put("detailsParModePaiement", parModePaiement);

        return stats;
    }

    @Override
    public Map<String, Object> getRevenusEtPertesParPeriode(LocalDate dateDebut, LocalDate dateFin) {
        Map<String, Object> resultats = new HashMap<>();
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);

        List<OperationCaisse> operations = operationRepository.findOperationsParPeriode(debut, fin).stream()
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();

        Double totalRevenus = 0.0;
        Double totalPertes = 0.0;
        Double totalVentesComptant = 0.0;
        Double totalReglementsCredit = 0.0;
        Double totalAutresEntrees = 0.0;
        Double totalSorties = 0.0;
        Double totalAnnulations = 0.0;

        for (OperationCaisse op : operations) {
            switch (op.getType()) {
                case VENTE_COMPTANT:
                    totalVentesComptant += op.getMontant();
                    totalRevenus += op.getMontant();
                    break;
                case REGLEMENT_CREDIT:
                    totalReglementsCredit += op.getMontant();
                    totalRevenus += op.getMontant();
                    break;
                case ENTREE:
                    totalAutresEntrees += op.getMontant();
                    totalRevenus += op.getMontant();
                    break;
                case SORTIE:
                case RETRAIT:
                    totalSorties += op.getMontant();
                    totalPertes += op.getMontant();
                    break;
                case ANNULATION_VENTE:
                case ANNULATION_CREDIT:
                    totalAnnulations += op.getMontant();
                    totalPertes += op.getMontant();
                    break;
                default:
                    break;
            }
        }

        resultats.put("periode", Map.of("debut", dateDebut, "fin", dateFin));
        resultats.put("totalRevenus", arrondir(totalRevenus));
        resultats.put("totalPertes", arrondir(totalPertes));
        resultats.put("soldeNet", arrondir(totalRevenus - totalPertes));
        resultats.put("detailsRevenus", Map.of("ventesComptant", arrondir(totalVentesComptant), "reglementsCredit", arrondir(totalReglementsCredit), "autresEntrees", arrondir(totalAutresEntrees)));
        resultats.put("detailsPertes", Map.of("sorties", arrondir(totalSorties), "annulations", arrondir(totalAnnulations)));

        return resultats;
    }

    // ==================== RAPPORTS PDF ====================

    @Override
    public byte[] genererRapportJournalier(LocalDate date) { return new byte[0]; }
    @Override
    public byte[] genererRapportHebdomadaire(LocalDate debutSemaine, LocalDate finSemaine) { return new byte[0]; }
    @Override
    public byte[] genererRapportMensuel(int annee, int mois) { return new byte[0]; }
    @Override
    public byte[] genererRapportAnnuel(int annee) { return new byte[0]; }
    @Override
    public byte[] genererRapportPersonnalise(LocalDate dateDebut, LocalDate dateFin) { return new byte[0]; }

    // ==================== VENTES COMPTANT/CRÉDIT ====================

    @Override
    public Map<String, Object> getVentesComptantDuJour() {
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        List<OperationCaisse> operations = operationRepository.findOperationsParPeriode(debut, fin).stream()
                .filter(op -> op.getType() == TypeOperationCaisse.VENTE_COMPTANT)
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();
        Map<String, Object> result = new HashMap<>();
        result.put("date", LocalDate.now());
        result.put("nombreVentes", operations.size());
        result.put("totalVentesComptant", arrondir(operations.stream().mapToDouble(OperationCaisse::getMontant).sum()));
        return result;
    }

    @Override
    public Map<String, Object> getVentesCreditDuJour() {
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        List<OperationCaisse> operations = operationRepository.findOperationsParPeriode(debut, fin).stream()
                .filter(op -> op.getType() == TypeOperationCaisse.VENTE_CREDIT)
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();
        Map<String, Object> result = new HashMap<>();
        result.put("date", LocalDate.now());
        result.put("nombreVentes", operations.size());
        result.put("totalVentesCredit", arrondir(operations.stream().mapToDouble(OperationCaisse::getMontant).sum()));
        return result;
    }

    @Override
    public Map<String, Object> getVentesComptantParPeriode(LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);
        List<OperationCaisse> operations = operationRepository.findOperationsParPeriode(debut, fin).stream()
                .filter(op -> op.getType() == TypeOperationCaisse.VENTE_COMPTANT)
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();
        Map<String, Object> result = new HashMap<>();
        result.put("periode", Map.of("debut", dateDebut, "fin", dateFin));
        result.put("nombreVentes", operations.size());
        result.put("totalVentesComptant", arrondir(operations.stream().mapToDouble(OperationCaisse::getMontant).sum()));
        return result;
    }

    @Override
    public Map<String, Object> getVentesCreditParPeriode(LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);
        List<OperationCaisse> operations = operationRepository.findOperationsParPeriode(debut, fin).stream()
                .filter(op -> op.getType() == TypeOperationCaisse.VENTE_CREDIT)
                .filter(op -> !Boolean.TRUE.equals(op.getVenteAnnulee()))
                .toList();
        Map<String, Object> result = new HashMap<>();
        result.put("periode", Map.of("debut", dateDebut, "fin", dateFin));
        result.put("nombreVentes", operations.size());
        result.put("totalVentesCredit", arrondir(operations.stream().mapToDouble(OperationCaisse::getMontant).sum()));
        return result;
    }

    @Override
    public Map<String, Object> getStatistiquesVentesComptantCredit() {
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime finJour = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime debutSemaine = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime finSemaine = LocalDate.now().with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
        LocalDateTime debutMois = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMois = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX);

        Map<String, Object> stats = new HashMap<>();
        stats.put("jour", Map.of(
                "ventesComptant", arrondir(operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.VENTE_COMPTANT, debutJour, finJour)),
                "ventesCredit", arrondir(operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.VENTE_CREDIT, debutJour, finJour)),
                "reglementsCredit", arrondir(operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.REGLEMENT_CREDIT, debutJour, finJour))));
        stats.put("semaine", Map.of(
                "ventesComptant", arrondir(operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.VENTE_COMPTANT, debutSemaine, finSemaine)),
                "ventesCredit", arrondir(operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.VENTE_CREDIT, debutSemaine, finSemaine)),
                "reglementsCredit", arrondir(operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.REGLEMENT_CREDIT, debutSemaine, finSemaine))));
        stats.put("mois", Map.of(
                "ventesComptant", arrondir(operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.VENTE_COMPTANT, debutMois, finMois)),
                "ventesCredit", arrondir(operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.VENTE_CREDIT, debutMois, finMois)),
                "reglementsCredit", arrondir(operationRepository.getTotalByTypeAndPeriod(TypeOperationCaisse.REGLEMENT_CREDIT, debutMois, finMois))));
        return stats;
    }

    // ==================== FACTURES ====================

    @Override
    @Transactional
    public Facture creerFacture(FactureRequest request, Long utilisateurId) {
        if (request.getLignes() == null || request.getLignes().isEmpty()) {
            throw new IllegalArgumentException("Une facture doit contenir au moins une ligne");
        }

        Facture facture = new Facture();
        facture.setClientNom(request.getClientNom());
        facture.setClientPrenom(request.getClientPrenom());
        facture.setClientTelephone(request.getClientTelephone());
        facture.setClientAdresse(request.getClientAdresse());
        facture.setNotes(request.getNotes());

        if (utilisateurId != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                    .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé"));
            facture.setUtilisateur(utilisateur);
        }

        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Client non trouvé"));
            facture.setClient(client);
            facture.setClientNom(client.getNom());
            facture.setClientPrenom(client.getPrenom());
            facture.setClientTelephone(client.getNumeroTelephone());
            facture.setClientAdresse(client.getAdresse());
        }

        Facture savedFacture = factureRepository.save(facture);

        for (LigneFactureRequest ligneRequest : request.getLignes()) {
            Produit produit = produitRepository.findById(ligneRequest.getProduitId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

            LigneFacture ligne = new LigneFacture();
            ligne.setFacture(savedFacture);
            ligne.setProduit(produit);
            ligne.setQuantite(ligneRequest.getQuantite());
            ligne.setPrixUnitaire(ligneRequest.getPrixUnitaire() != null ? ligneRequest.getPrixUnitaire() : produit.getPrixVente());
            ligne.setPrixAchat(produit.getPrixAchat());

            if (ligneRequest.getRemisePourcentage() != null && ligneRequest.getRemisePourcentage() > 0) {
                ligne.appliquerRemisePourcentage(ligneRequest.getRemisePourcentage());
            } else if (ligneRequest.getRemiseMontant() != null && ligneRequest.getRemiseMontant() > 0) {
                ligne.appliquerRemiseMontant(ligneRequest.getRemiseMontant());
            }

            ligne.calculerSousTotal();
            ligneFactureRepository.save(ligne);
            savedFacture.getLignes().add(ligne);
        }

        savedFacture.calculerTotal();
        return factureRepository.save(savedFacture);
    }

    @Override
    @Transactional
    public Facture modifierFacture(Long factureId, FactureRequest request) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée"));

        facture.setClientNom(request.getClientNom());
        facture.setClientPrenom(request.getClientPrenom());
        facture.setClientTelephone(request.getClientTelephone());
        facture.setClientAdresse(request.getClientAdresse());
        facture.setNotes(request.getNotes());

        ligneFactureRepository.deleteByFactureId(factureId);
        facture.getLignes().clear();

        for (LigneFactureRequest ligneRequest : request.getLignes()) {
            Produit produit = produitRepository.findById(ligneRequest.getProduitId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé"));

            LigneFacture ligne = new LigneFacture();
            ligne.setFacture(facture);
            ligne.setProduit(produit);
            ligne.setQuantite(ligneRequest.getQuantite());
            ligne.setPrixUnitaire(ligneRequest.getPrixUnitaire() != null ? ligneRequest.getPrixUnitaire() : produit.getPrixVente());
            ligne.setPrixAchat(produit.getPrixAchat());

            if (ligneRequest.getRemisePourcentage() != null && ligneRequest.getRemisePourcentage() > 0) {
                ligne.appliquerRemisePourcentage(ligneRequest.getRemisePourcentage());
            } else if (ligneRequest.getRemiseMontant() != null && ligneRequest.getRemiseMontant() > 0) {
                ligne.appliquerRemiseMontant(ligneRequest.getRemiseMontant());
            }

            ligne.calculerSousTotal();
            ligneFactureRepository.save(ligne);
            facture.getLignes().add(ligne);
        }

        facture.calculerTotal();
        return factureRepository.save(facture);
    }

    @Override
    public Facture obtenirFactureParId(Long factureId) {
        return factureRepository.findById(factureId)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée"));
    }

    @Override
    public List<Facture> obtenirToutesFactures() {
        return factureRepository.findAll();
    }

    @Override
    public List<Facture> obtenirFacturesParStatut(String statut) {
        return factureRepository.findByStatut(statut);
    }

    @Override
    public List<Facture> obtenirFacturesParClient(String clientNom) {
        return factureRepository.findByClientNomContainingIgnoreCase(clientNom);
    }

    @Override
    public List<Facture> obtenirFacturesParPeriode(LocalDateTime dateDebut, LocalDateTime dateFin) {
        return factureRepository.findByDateCreationBetween(dateDebut, dateFin);
    }

    @Override
    @Transactional
    public void supprimerFacture(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée"));
        ligneFactureRepository.deleteByFactureId(factureId);
        factureRepository.deleteById(factureId);
    }

    @Override
    @Transactional
    public Facture validerFacture(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée"));
        facture.setStatut("VALIDE");
        return factureRepository.save(facture);
    }

    @Override
    @Transactional
    public Facture annulerFacture(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée"));
        if ("ANNULEE".equals(facture.getStatut())) {
            throw new IllegalStateException("La facture est déjà annulée");
        }
        facture.setStatut("ANNULEE");
        return factureRepository.save(facture);
    }

    @Override
    public Map<String, Object> getStatistiquesFactures() {
        List<Facture> toutesFactures = factureRepository.findAll();
        long nombreTotal = toutesFactures.size();
        long nombreBrouillons = toutesFactures.stream().filter(f -> "BROUILLON".equals(f.getStatut())).count();
        long nombreValides = toutesFactures.stream().filter(f -> "VALIDE".equals(f.getStatut())).count();
        long nombrePayees = toutesFactures.stream().filter(f -> "PAYEE".equals(f.getStatut())).count();
        long nombreAnnulees = toutesFactures.stream().filter(f -> "ANNULEE".equals(f.getStatut())).count();
        Double montantTotal = toutesFactures.stream()
                .filter(f -> "PAYEE".equals(f.getStatut()) || "VALIDE".equals(f.getStatut()))
                .mapToDouble(Facture::getMontantTotal).sum();
        Map<String, Object> stats = new HashMap<>();
        stats.put("nombreTotal", nombreTotal);
        stats.put("nombreBrouillons", nombreBrouillons);
        stats.put("nombreValides", nombreValides);
        stats.put("nombrePayees", nombrePayees);
        stats.put("nombreAnnulees", nombreAnnulees);
        stats.put("montantTotal", arrondir(montantTotal));
        return stats;
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private void verifierEtOuvrirCaisseSiNecessaire() {
        try {
            getCaisseOuverte();
        } catch (IllegalStateException e) {
            ouvrirCaisse();
        }
    }

    private Double arrondir(Double valeur) {
        if (valeur == null) return 0.0;
        return BigDecimal.valueOf(valeur).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}