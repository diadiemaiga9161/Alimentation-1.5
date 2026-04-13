package com.ges.boutique.caisse;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.exception.SoldeInsuffisantException;
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

    @Override
    @Transactional
    public Caisse ouvrirCaisse() {
        Optional<Caisse> caisseOuverte = caisseRepository.findCaisseOuverte();
        if (caisseOuverte.isPresent()) {
            return caisseOuverte.get();
        }

        Optional<Caisse> derniereCaisse = caisseRepository.findFirstByOrderByIdDesc();
        Caisse caisse;

        if (derniereCaisse.isPresent()) {
            Caisse derniere = derniereCaisse.get();
            derniere.setEstOuverte(true);
            derniere.setDateOuverture(LocalDateTime.now());
            derniere.setDerniereOperation(LocalDateTime.now());
            derniere.setVerifiee(false);
            derniere.setSoldeInitial(derniere.getSoldeActuel());
            derniere.setSoldeSysteme(derniere.getSoldeActuel());
            derniere.setSoldeReel(derniere.getSoldeActuel());
            caisse = caisseRepository.save(derniere);
        } else {
            caisse = new Caisse();
            caisse.setSoldeActuel(0.0);
            caisse.setSoldeInitial(0.0);
            caisse.setSoldeSysteme(0.0);
            caisse.setSoldeReel(0.0);
            caisse.setEstOuverte(true);
            caisse.setDateOuverture(LocalDateTime.now());
            caisse.setDerniereOperation(LocalDateTime.now());
            caisse.setNombreOperations(0);
            caisse = caisseRepository.save(caisse);
        }

        OperationCaisse operation = new OperationCaisse();
        operation.setType(TypeOperationCaisse.OUVERTURE);
        operation.setMontant(caisse.getSoldeActuel());
        operation.setSoldeAvant(caisse.getSoldeActuel());
        operation.setSoldeApres(caisse.getSoldeActuel());
        operation.setMotif("Ouverture de caisse");
        operation.setDateOperation(LocalDateTime.now());
        operationRepository.save(operation);

        return caisse;
    }

    @Override
    @Transactional
    public Caisse fermerCaisse(Long utilisateurId) {
        Caisse caisse = getCaisseOuverte();
        caisse.setEstOuverte(false);
        caisse.setDateFermeture(LocalDateTime.now());
        caisse.setDerniereOperation(LocalDateTime.now());
        caisse.mettreAJourSoldeSysteme();

        Long nombreOps = operationRepository.countOperationsDuJour();
        caisse.setNombreOperations(nombreOps != null ? nombreOps.intValue() : 0);

        Caisse savedCaisse = caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
        operation.setType(TypeOperationCaisse.FERMETURE);
        operation.setMontant(caisse.getSoldeActuel());
        operation.setSoldeAvant(caisse.getSoldeActuel());
        operation.setSoldeApres(caisse.getSoldeActuel());
        operation.setMotif("Fermeture de caisse");
        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }
        operation.setDateOperation(LocalDateTime.now());
        operationRepository.save(operation);

        return savedCaisse;
    }

    @Override
    public Caisse getCaisseOuverte() {
        return caisseRepository.findCaisseOuverte()
                .orElseThrow(() -> new IllegalStateException("Aucune caisse n'est ouverte"));
    }

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
        operation.setType(TypeOperationCaisse.VERIFICATION);
        operation.setMontant(Math.abs(caisse.getEcart()));
        operation.setSoldeAvant(caisse.getSoldeSysteme());
        operation.setSoldeApres(caisse.getSoldeSysteme());
        operation.setMotif("Vérification de caisse - Écart: " + caisse.getEcart() +
                (observations != null ? " (" + observations + ")" : ""));
        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }
        operation.setDateOperation(LocalDateTime.now());
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

    @Override
    @Transactional
    public OperationCaisse entreeCaisse(Double montant, String motif, Long utilisateurId,
                                        String modePaiement, String reference) {
        if (montant == null || montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0");
        }

        Caisse caisse = getCaisseOuverte();
        Double soldeAvant = caisse.getSoldeActuel();

        caisse.setSoldeActuel(soldeAvant + montant);
        caisse.setTotalEntrees(caisse.getTotalEntrees() + montant);
        caisse.setDerniereOperation(LocalDateTime.now());
        caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
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

        Caisse caisse = getCaisseOuverte();

        if (caisse.getSoldeActuel() < montant) {
            throw new SoldeInsuffisantException(
                    "Solde insuffisant. Disponible: " + caisse.getSoldeActuel() +
                            ", Demandé: " + montant
            );
        }

        Double soldeAvant = caisse.getSoldeActuel();

        caisse.setSoldeActuel(soldeAvant - montant);
        caisse.setTotalSorties(caisse.getTotalSorties() + montant);
        caisse.setDerniereOperation(LocalDateTime.now());
        caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
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

        Caisse caisse = getCaisseOuverte();
        Double soldeAvant = caisse.getSoldeActuel();

        caisse.setSoldeActuel(soldeAvant + vente.getMontantTotal());
        caisse.setTotalEntrees(caisse.getTotalEntrees() + vente.getMontantTotal());
        caisse.setDerniereOperation(LocalDateTime.now());
        caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
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

        OperationCaisse operation = new OperationCaisse();
        operation.setType(TypeOperationCaisse.VENTE_CREDIT);
        operation.setMontant(vente.getMontantTotal());
        operation.setSoldeAvant(soldeAvant);
        operation.setSoldeApres(soldeAvant);
        operation.setMotif("Vente à crédit N°" + vente.getNumeroVente());
        operation.setVente(vente);
        operation.setEstReglee(false);
        operation.setClientNom(clientNom);
        operation.setClientTelephone(clientTelephone);
        operation.setDateOperation(LocalDateTime.now());
        operation.setMontantVerse(0.0);
        operation.setMontantRestant(vente.getMontantTotal());

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
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Vente non trouvée avec l'ID: " + venteCreditId));

        if (!Boolean.TRUE.equals(vente.getEstCredit())) {
            throw new IllegalArgumentException("La vente avec l'ID " + venteCreditId + " n'est pas un crédit");
        }

        if (vente.getCreditRegle()) {
            throw new IllegalStateException("Ce crédit est déjà totalement réglé");
        }

        if (montantRegle == null || montantRegle <= 0) {
            throw new IllegalArgumentException("Le montant réglé doit être supérieur à 0");
        }

        Double montantRestantActuel = vente.getMontantRestant();
        if (montantRegle > montantRestantActuel) {
            throw new IllegalArgumentException("Le montant réglé ne peut pas dépasser le montant restant (" +
                    montantRestantActuel + ")");
        }

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

        Optional<OperationCaisse> operationCreditOpt = operationRepository.findFirstByVenteIdAndType(
                venteCreditId, TypeOperationCaisse.VENTE_CREDIT);

        OperationCaisse reglementOperation = new OperationCaisse();
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

        if (operationCreditOpt.isPresent()) {
            OperationCaisse opCredit = operationCreditOpt.get();
            reglementOperation.setClientNom(opCredit.getClientNom());
            reglementOperation.setClientTelephone(opCredit.getClientTelephone());
            reglementOperation.setNumeroCredit(opCredit.getNumeroCredit());
        } else {
            reglementOperation.setClientNom(vente.getClientNom());
            reglementOperation.setClientTelephone(vente.getClientTelephone());
        }

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
        log.info("Annulation de vente en caisse - Vente ID: {}, Numéro: {}, Montant: {}",
                vente.getId(), vente.getNumeroVente(), vente.getMontantTotal());

        Caisse caisse = getCaisseOuverte();
        Double soldeAvant = caisse.getSoldeActuel();

        if (caisse.getSoldeActuel() < vente.getMontantTotal()) {
            throw new SoldeInsuffisantException(
                    "Solde insuffisant pour annuler la vente. Disponible: " + caisse.getSoldeActuel() +
                            ", Montant vente: " + vente.getMontantTotal()
            );
        }

        caisse.setSoldeActuel(soldeAvant - vente.getMontantTotal());
        caisse.setTotalSorties(caisse.getTotalSorties() + vente.getMontantTotal());
        caisse.setDerniereOperation(LocalDateTime.now());
        caisseRepository.save(caisse);

        OperationCaisse operation = new OperationCaisse();
        operation.setType(TypeOperationCaisse.SORTIE);
        operation.setMontant(vente.getMontantTotal());
        operation.setSoldeAvant(soldeAvant);
        operation.setSoldeApres(caisse.getSoldeActuel());
        operation.setMotif(motif != null ? "ANNULATION VENTE " + vente.getNumeroVente() + " - " + motif :
                "Annulation vente N°" + vente.getNumeroVente());
        operation.setVente(vente);
        operation.setEstReglee(true);
        operation.setDateOperation(LocalDateTime.now());

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }

        return operationRepository.save(operation);
    }

    @Override
    @Transactional
    public OperationCaisse annulerVenteCredit(Vente vente, Long utilisateurId, String motif) {
        log.info("Annulation de crédit en caisse - Vente ID: {}, Numéro: {}, Client: {}, Montant: {}",
                vente.getId(), vente.getNumeroVente(), vente.getClientNom(), vente.getMontantTotal());

        Optional<OperationCaisse> operationCreditOpt = operationRepository.findFirstByVenteIdAndType(
                vente.getId(), TypeOperationCaisse.VENTE_CREDIT);

        if (operationCreditOpt.isPresent()) {
            OperationCaisse operationCredit = operationCreditOpt.get();

            if (!operationCredit.isEstReglee() && operationCredit.getMontantRestant() < operationCredit.getMontant()) {
                throw new IllegalStateException("Impossible d'annuler un crédit avec des règlements partiels");
            }

            operationCredit.setEstReglee(true);
            operationCredit.setMotif(operationCredit.getMotif() + " - ANNULE");
            operationRepository.save(operationCredit);
        }

        OperationCaisse operation = new OperationCaisse();
        operation.setType(TypeOperationCaisse.AJUSTEMENT);
        operation.setMontant(vente.getMontantTotal());
        operation.setSoldeAvant(0.0);
        operation.setSoldeApres(0.0);
        operation.setMotif(motif != null ? "ANNULATION CREDIT " + vente.getNumeroVente() + " - " + motif :
                "Annulation crédit N°" + vente.getNumeroVente());
        operation.setVente(vente);
        operation.setEstReglee(true);
        operation.setDateOperation(LocalDateTime.now());
        operation.setClientNom(vente.getClientNom());
        operation.setClientTelephone(vente.getClientTelephone());

        if (utilisateurId != null) {
            utilisateurRepository.findById(utilisateurId).ifPresent(operation::setUtilisateur);
        }

        return operationRepository.save(operation);
    }

    @Override
    public Map<String, Object> getRevenusEtPertesParPeriode(LocalDate dateDebut, LocalDate dateFin) {
        log.info("Calcul des revenus et pertes du {} au {}", dateDebut, dateFin);

        Map<String, Object> resultats = new HashMap<>();
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);

        List<OperationCaisse> operations = operationRepository.findOperationsParPeriode(debut, fin);

        Double totalRevenus = 0.0;
        Double totalPertes = 0.0;
        Double totalVentesComptant = 0.0;
        Double totalReglementsCredit = 0.0;
        Double totalAutresEntrees = 0.0;
        Double totalSorties = 0.0;
        Double totalAnnulations = 0.0;
        Double totalCreditsNonRegles = 0.0;

        Map<String, Double> revenusParCategorie = new HashMap<>();
        Map<String, Double> pertesParCategorie = new HashMap<>();
        Map<LocalDate, Double> revenusParJour = new HashMap<>();
        Map<LocalDate, Double> pertesParJour = new HashMap<>();
        Map<String, Double> parModePaiement = new HashMap<>();

        for (OperationCaisse op : operations) {
            LocalDate date = op.getDateOperation().toLocalDate();

            switch (op.getType()) {
                case VENTE_COMPTANT:
                    totalVentesComptant += op.getMontant();
                    revenusParCategorie.merge("Ventes comptant", op.getMontant(), Double::sum);
                    revenusParJour.merge(date, op.getMontant(), Double::sum);
                    break;

                case REGLEMENT_CREDIT:
                    totalReglementsCredit += op.getMontant();
                    revenusParCategorie.merge("Règlements crédits", op.getMontant(), Double::sum);
                    revenusParJour.merge(date, op.getMontant(), Double::sum);
                    break;

                case ENTREE:
                case DEPOT:
                    totalAutresEntrees += op.getMontant();
                    revenusParCategorie.merge("Autres entrées", op.getMontant(), Double::sum);
                    revenusParJour.merge(date, op.getMontant(), Double::sum);
                    break;

                case SORTIE:
                case RETRAIT:
                    totalSorties += op.getMontant();
                    pertesParCategorie.merge("Sorties/Retraits", op.getMontant(), Double::sum);
                    pertesParJour.merge(date, op.getMontant(), Double::sum);

                    // Vérifier si c'est une annulation de vente
                    if (op.getMotif() != null && op.getMotif().startsWith("ANNULATION VENTE")) {
                        totalAnnulations += op.getMontant();
                        pertesParCategorie.merge("Annulations ventes", op.getMontant(), Double::sum);
                        pertesParJour.merge(date, op.getMontant(), Double::sum);
                    }
                    break;

                case AJUSTEMENT:
                    totalAutresEntrees += op.getMontant(); // Par défaut, les ajustements sont des revenus

                    // Vérifier si c'est une annulation de crédit
                    if (op.getMotif() != null && op.getMotif().startsWith("ANNULATION CREDIT")) {
                        totalAnnulations += op.getMontant();
                        pertesParCategorie.merge("Annulations crédits", op.getMontant(), Double::sum);
                        pertesParJour.merge(date, op.getMontant(), Double::sum);
                    }
                    break;

                default:
                    break;
            }

            if (op.getModePaiement() != null) {
                parModePaiement.merge(op.getModePaiement().toString(), op.getMontant(), Double::sum);
            }
        }

        List<Vente> creditsNonRegles = venteRepository.findCreditsNonRegles()
                .stream()
                .filter(c -> !c.getDateVente().toLocalDate().isBefore(dateDebut) &&
                        !c.getDateVente().toLocalDate().isAfter(dateFin))
                .toList();

        for (Vente credit : creditsNonRegles) {
            totalCreditsNonRegles += credit.getMontantRestant();
        }

        totalRevenus = totalVentesComptant + totalReglementsCredit + totalAutresEntrees;
        totalPertes = totalSorties + totalAnnulations;

        Double soldeNet = totalRevenus - totalPertes;
        Double tauxMarge = totalRevenus > 0 ? (soldeNet / totalRevenus) * 100 : 0;

        resultats.put("periode", Map.of(
                "debut", dateDebut,
                "fin", dateFin,
                "nbJours", ChronoUnit.DAYS.between(dateDebut, dateFin) + 1
        ));

        resultats.put("totalRevenus", arrondir(totalRevenus));
        resultats.put("totalPertes", arrondir(totalPertes));
        resultats.put("soldeNet", arrondir(soldeNet));
        resultats.put("tauxMarge", arrondir(tauxMarge));

        resultats.put("detailsRevenus", Map.of(
                "ventesComptant", arrondir(totalVentesComptant),
                "reglementsCredit", arrondir(totalReglementsCredit),
                "autresEntrees", arrondir(totalAutresEntrees)
        ));

        resultats.put("detailsPertes", Map.of(
                "sorties", arrondir(totalSorties),
                "annulations", arrondir(totalAnnulations),
                "creditsNonRegles", arrondir(totalCreditsNonRegles)
        ));

        resultats.put("revenusParCategorie", revenusParCategorie);
        resultats.put("pertesParCategorie", pertesParCategorie);
        resultats.put("revenusParJour", revenusParJour);
        resultats.put("pertesParJour", pertesParJour);
        resultats.put("parModePaiement", parModePaiement);

        resultats.put("nombreOperations", operations.size());
        resultats.put("nombreVentesComptant", operations.stream()
                .filter(o -> o.getType() == TypeOperationCaisse.VENTE_COMPTANT).count());
        resultats.put("nombreReglementsCredit", operations.stream()
                .filter(o -> o.getType() == TypeOperationCaisse.REGLEMENT_CREDIT).count());

        // Compter les annulations basées sur le motif
        long nombreAnnulations = operations.stream()
                .filter(o -> (o.getType() == TypeOperationCaisse.SORTIE || o.getType() == TypeOperationCaisse.AJUSTEMENT) &&
                        o.getMotif() != null && (o.getMotif().startsWith("ANNULATION VENTE") || o.getMotif().startsWith("ANNULATION CREDIT")))
                .count();
        resultats.put("nombreAnnulations", nombreAnnulations);

        return resultats;
    }

    @Override
    public List<OperationCaisse> getCreditsNonRegles() {
        List<OperationCaisse> credits = operationRepository.findCreditsNonRegles(TypeOperationCaisse.VENTE_CREDIT);

        for (OperationCaisse credit : credits) {
            Double totalRegle = operationRepository.getTotalReglementsByVenteCredit(
                    credit.getVente() != null ? credit.getVente().getId() : credit.getVenteCreditId(),
                    TypeOperationCaisse.REGLEMENT_CREDIT);

            if (totalRegle != null) {
                credit.setMontantVerse(totalRegle);
                credit.setMontantRestant(credit.getMontant() - totalRegle);
            } else {
                credit.setMontantVerse(0.0);
                credit.setMontantRestant(credit.getMontant());
            }

            if (credit.getMontantRestant() <= 0) {
                credit.setEstReglee(true);
            }
        }

        return credits.stream()
                .filter(c -> !c.isEstReglee() && c.getMontantRestant() > 0)
                .toList();
    }

    @Override
    public List<OperationCaisse> getCreditsEnRetard() {
        return operationRepository.findCreditsEnRetard(LocalDateTime.now());
    }

    @Override
    public Map<String, Object> getSituationCredits() {
        Map<String, Object> situation = new HashMap<>();
        List<OperationCaisse> creditsNonRegles = getCreditsNonRegles();
        List<OperationCaisse> creditsEnRetard = getCreditsEnRetard();

        Double montantTotalCredits = creditsNonRegles.stream()
                .mapToDouble(OperationCaisse::getMontant)
                .sum();

        Double montantVerseTotal = creditsNonRegles.stream()
                .mapToDouble(OperationCaisse::getMontantVerse)
                .sum();

        Double montantRestantTotal = creditsNonRegles.stream()
                .mapToDouble(OperationCaisse::getMontantRestant)
                .sum();

        Double montantTotalRetard = creditsEnRetard.stream()
                .mapToDouble(OperationCaisse::getMontantRestant)
                .sum();

        situation.put("nombreCreditsNonRegles", creditsNonRegles.size());
        situation.put("montantTotalCredits", arrondir(montantTotalCredits));
        situation.put("montantVerseTotal", arrondir(montantVerseTotal));
        situation.put("montantRestantTotal", arrondir(montantRestantTotal));
        situation.put("nombreCreditsEnRetard", creditsEnRetard.size());
        situation.put("montantTotalRetard", arrondir(montantTotalRetard));

        List<Map<String, Object>> detailsCredits = creditsNonRegles.stream()
                .map(c -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", c.getId());
                    detail.put("clientNom", c.getClientNom());
                    detail.put("clientTelephone", c.getClientTelephone());
                    detail.put("montantTotal", arrondir(c.getMontant()));
                    detail.put("montantVerse", arrondir(c.getMontantVerse()));
                    detail.put("montantRestant", arrondir(c.getMontantRestant()));
                    detail.put("dateOperation", c.getDateOperation());
                    detail.put("dateEcheance", c.getDateEcheance());
                    detail.put("venteId", c.getVente() != null ? c.getVente().getId() : null);
                    detail.put("numeroVente", c.getVente() != null ? c.getVente().getNumeroVente() : null);
                    detail.put("enRetard", c.getDateEcheance() != null &&
                            c.getDateEcheance().isBefore(LocalDateTime.now()));
                    return detail;
                })
                .toList();

        situation.put("detailsCredits", detailsCredits);

        return situation;
    }

    @Override
    public List<OperationCaisse> getHistoriqueReglementsCredit(Long venteCreditId) {
        return operationRepository.findReglementsByVenteCredit(venteCreditId);
    }

    @Override
    public List<OperationCaisse> getOperationsDuJour() {
        return operationRepository.findOperationsDuJour();
    }

    @Override
    public List<OperationCaisse> getOperationsDeLaSemaine() {
        LocalDateTime debutSemaine = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime finSemaine = LocalDate.now().with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
        return operationRepository.findOperationsParPeriode(debutSemaine, finSemaine);
    }

    @Override
    public List<OperationCaisse> getOperationsDuMois() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDateTime debutMois = aujourdhui.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMois = aujourdhui.withDayOfMonth(aujourdhui.lengthOfMonth()).atTime(LocalTime.MAX);
        return operationRepository.findOperationsParPeriode(debutMois, finMois);
    }

    @Override
    public List<OperationCaisse> getOperationsDeLAnnee() {
        LocalDate aujourdhui = LocalDate.now();
        LocalDateTime debutAnnee = aujourdhui.withDayOfYear(1).atStartOfDay();
        LocalDateTime finAnnee = aujourdhui.withDayOfYear(aujourdhui.lengthOfYear()).atTime(LocalTime.MAX);
        return operationRepository.findOperationsParPeriode(debutAnnee, finAnnee);
    }

    @Override
    public List<OperationCaisse> getOperationsParPeriode(LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);
        return operationRepository.findOperationsParPeriode(debut, fin);
    }

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

        List<OperationCaisse> operations = operationRepository.findOperationsParPeriode(debut, fin);

        Double totalVentesComptant = operationRepository.getTotalByTypeAndPeriod(
                TypeOperationCaisse.VENTE_COMPTANT, debut, fin);
        Double totalVentesCredit = operationRepository.getTotalByTypeAndPeriod(
                TypeOperationCaisse.VENTE_CREDIT, debut, fin);
        Double totalReglementsCredit = operationRepository.getTotalByTypeAndPeriod(
                TypeOperationCaisse.REGLEMENT_CREDIT, debut, fin);
        Double totalEntrees = operationRepository.getTotalByTypeAndPeriod(
                TypeOperationCaisse.ENTREE, debut, fin);
        Double totalSorties = operationRepository.getTotalByTypeAndPeriod(
                TypeOperationCaisse.SORTIE, debut, fin);

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

        stats.put("periode", Map.of(
                "debut", dateDebut,
                "fin", dateFin,
                "nbJours", ChronoUnit.DAYS.between(dateDebut, dateFin) + 1
        ));

        stats.put("totalVentesComptant", arrondir(totalVentesComptant != null ? totalVentesComptant : 0));
        stats.put("totalNouveauxCredits", arrondir(totalVentesCredit != null ? totalVentesCredit : 0));
        stats.put("totalReglementsCredit", arrondir(totalReglementsCredit != null ? totalReglementsCredit : 0));
        stats.put("totalAutresEntrees", arrondir(totalEntrees != null ? totalEntrees : 0));
        stats.put("totalSorties", arrondir(totalSorties != null ? totalSorties : 0));

        stats.put("totalEntrees", arrondir(totalEntreesCaisse));
        stats.put("totalSorties", arrondir(totalSortiesCaisse));
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

        stats.put("situationCredits", getSituationCredits());

        return stats;
    }

    @Override
    public byte[] genererRapportJournalier(LocalDate date) {
        return new byte[0];
    }

    @Override
    public byte[] genererRapportHebdomadaire(LocalDate debutSemaine, LocalDate finSemaine) {
        return new byte[0];
    }

    @Override
    public byte[] genererRapportMensuel(int annee, int mois) {
        return new byte[0];
    }

    @Override
    public byte[] genererRapportAnnuel(int annee) {
        return new byte[0];
    }

    @Override
    public byte[] genererRapportPersonnalise(LocalDate dateDebut, LocalDate dateFin) {
        return new byte[0];
    }

    private void verifierEtOuvrirCaisseSiNecessaire() {
        try {
            getCaisseOuverte();
        } catch (IllegalStateException e) {
            ouvrirCaisse();
        }
    }

    private Double arrondir(Double valeur) {
        if (valeur == null) return 0.0;
        return BigDecimal.valueOf(valeur)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}