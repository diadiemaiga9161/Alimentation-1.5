package com.ges.boutique.fournisseur;

import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.caisse.OperationCaisse;
import com.ges.boutique.compte.CompteService;
import com.ges.boutique.compte.TypeOperationCompte;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.exception.SoldeInsuffisantException;
import com.ges.boutique.inventaire.InventaireService;
import com.ges.boutique.objectif.ObjectifFournisseur;
import com.ges.boutique.objectif.ObjectifFournisseurRepository;
import com.ges.boutique.objectif.StatutObjectif;
import com.ges.boutique.produit.Categorie;
import com.ges.boutique.produit.CategorieRepository;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FournisseurComptableService {

    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;
    private final AchatFournisseurRepository achatRepository;
    private final PaiementFournisseurRepository paiementRepository;
    private final CaisseService caisseService;
    private final CategorieRepository categorieRepository;
    private final CompteService compteService;
    private final AvanceFournisseurService avanceFournisseurService;
    private final InventaireService inventaireService;
    private final AchatPaiementLienRepository achatPaiementLienRepository;
    private final ObjectifFournisseurRepository objectifRepository;

    // ============================================
    // MÉTHODE 1 : CRÉER UN ACHAT
    // ============================================
    @Transactional
    public AchatFournisseur creerAchat(AchatFournisseurRequest request) {
        log.info("=== CRÉATION ACHAT FOURNISSEUR ===");
        log.info("Request: {}", request);

        Fournisseur fournisseur;
        if (request.getFournisseurId() != null) {
            fournisseur = fournisseurRepository.findById(request.getFournisseurId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Fournisseur introuvable"));
        } else if (request.getNouveauFournisseur() != null) {
            fournisseur = creerNouveauFournisseur(request.getNouveauFournisseur());
        } else {
            throw new IllegalArgumentException("Fournisseur existant ou nouveau fournisseur requis");
        }

        AchatFournisseur achat = new AchatFournisseur();
        achat.setDateAchat(LocalDateTime.now());
        achat.setFournisseur(fournisseur);
        achat.setCommentaire(request.getCommentaire());
        achat.setUtilisateurId(request.getUtilisateurId());
        achat.setMontantTotal(0.0);

        double avanceUtilisee = request.getMontantAvanceUtilise() != null ? request.getMontantAvanceUtilise() : 0.0;
        double paiementImmediat = request.getMontantPaye() != null ? request.getMontantPaye() : 0.0;
        achat.setMontantPaye(paiementImmediat + avanceUtilisee);

        achat.setMontantAvanceUtilise(avanceUtilisee);

        String modePaiement = request.getModePaiementImmediat();
        if (paiementImmediat > 0) {
            if (modePaiement == null || modePaiement.trim().isEmpty()) {
                if (request.getCompteIdPaiement() != null && request.getCompteIdPaiement() > 0) {
                    modePaiement = "BANQUE";
                    log.info("Mode de paiement détecté automatiquement: BANQUE (compteId={})", request.getCompteIdPaiement());
                } else {
                    modePaiement = "ESPECES";
                    log.info("Mode de paiement détecté automatiquement: ESPECES (par défaut)");
                }
            }

            achat.setModePaiementImmediat(modePaiement);

            if ("BANQUE".equals(modePaiement)) {
                if (request.getCompteIdPaiement() == null) {
                    throw new IllegalArgumentException("Le compte bancaire est obligatoire pour un paiement par BANQUE");
                }
                achat.setCompteIdPaiement(request.getCompteIdPaiement());
            } else {
                achat.setCompteIdPaiement(null);
            }

            log.info("Paiement immédiat enregistré: {} F - Mode: {}", paiementImmediat, modePaiement);
        } else {
            log.info("Aucun paiement immédiat, achat payé uniquement par avance");
        }

        double totalAchat = 0.0;

        for (LigneAchatRequest ligneReq : request.getLignes()) {
            Produit produit;
            if (ligneReq.getProduitId() != null) {
                produit = produitRepository.findById(ligneReq.getProduitId())
                        .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable : " + ligneReq.getProduitId()));
            } else {
                produit = creerNouveauProduitDepuisAchat(ligneReq);
            }

            LigneAchatFournisseur ligne = new LigneAchatFournisseur();
            ligne.setAchat(achat);
            ligne.setProduit(produit);
            ligne.setQuantite(ligneReq.getQuantite());
            ligne.setPrixAchatUnitaire(ligneReq.getPrixAchatUnitaire());

            double sousTotal = ligneReq.getQuantite() * ligneReq.getPrixAchatUnitaire();
            ligne.setSousTotal(sousTotal);
            totalAchat += sousTotal;

            achat.getLignes().add(ligne);

            int ancienneQuantite = produit.getQuantite() != null ? produit.getQuantite() : 0;
            String motifEntree = "Achat fournisseur - " + fournisseur.getNom() + " - " + produit.getNom();
            inventaireService.entreeStock(produit.getId(), ligneReq.getQuantite(), request.getUtilisateurId(), motifEntree);
            log.info("Stock ajouté (achat): +{} x {} (stock avant: {}, stock après: {})",
                    ligneReq.getQuantite(), produit.getNom(), ancienneQuantite, ancienneQuantite + ligneReq.getQuantite());

            // Produit existant : mise à jour optionnelle du prix d'achat (CUMP), déjà calculé et confirmé côté front
            if (ligneReq.getProduitId() != null
                    && ligneReq.getNouveauPrixAchat() != null
                    && ligneReq.getNouveauPrixAchat() > 0) {
                double ancienPrixAchat = produit.getPrixAchat() != null ? produit.getPrixAchat() : 0.0;
                produit.setPrixAchat(ligneReq.getNouveauPrixAchat());
                produitRepository.save(produit);
                log.info("Prix d'achat mis à jour (CUMP) pour produit {} : {} -> {}",
                        produit.getNom(), ancienPrixAchat, ligneReq.getNouveauPrixAchat());
            }
        }

        achat.setMontantTotal(totalAchat);
        achat.setMontantRestant(totalAchat - achat.getMontantPaye());
        if (achat.getMontantRestant() <= 0.01) {
            achat.setStatut(StatutAchat.PAYE);
        } else {
            achat.setStatut(StatutAchat.EN_COURS);
        }

        if (avanceUtilisee > 0) {
            avanceFournisseurService.utiliserAvance(fournisseur.getId(), avanceUtilisee);
            log.info("Avance fournisseur utilisée: {} F pour {}", avanceUtilisee, fournisseur.getNom());
        }

        fournisseur.setTotalAchats(fournisseur.getTotalAchats() + totalAchat);
        fournisseur.setSolde(fournisseur.getSolde() + totalAchat - achat.getMontantPaye());
        fournisseur.setTotalPaye(fournisseur.getTotalPaye() + achat.getMontantPaye());
        fournisseurRepository.save(fournisseur);

        AchatFournisseur savedAchat = achatRepository.save(achat);

        if (paiementImmediat > 0) {
            PaiementFournisseur paiement = new PaiementFournisseur();
            paiement.setDatePaiement(LocalDateTime.now());
            paiement.setFournisseur(fournisseur);
            paiement.setMontant(paiementImmediat);

            if ("BANQUE".equals(modePaiement)) {
                paiement.setModePaiement(ModePaiementFournisseur.BANQUE);
                paiement.setCompteId(request.getCompteIdPaiement());
                compteService.debiterCompte(
                        request.getCompteIdPaiement(),
                        paiementImmediat,
                        "Paiement fournisseur - " + fournisseur.getNom() + " (achat #" + savedAchat.getId() + ")",
                        TypeOperationCompte.PAIEMENT_FOURNISSEUR.toString(),
                        request.getUtilisateurId()
                );
                log.info("Débit compte bancaire id={} pour paiement fournisseur", request.getCompteIdPaiement());
            } else {
                paiement.setModePaiement(ModePaiementFournisseur.ESPECES);
                try {
                    OperationCaisse sortie = caisseService.sortieCaisseFournisseur(
                            paiementImmediat,
                            "Paiement fournisseur - " + fournisseur.getNom() + " (achat #" + savedAchat.getId() + ")",
                            request.getUtilisateurId()
                    );
                    paiement.setOperationCaisse(sortie);
                    log.info("Opération caisse créée pour paiement fournisseur");
                } catch (SoldeInsuffisantException e) {
                    throw new SoldeInsuffisantException(
                            "Solde caisse insuffisant pour payer " + paiementImmediat +
                                    " F. Veuillez utiliser un autre mode de paiement."
                    );
                }
            }

            paiement.setReference("Paiement immédiat achat #" + savedAchat.getId());
            paiement.setObservation(request.getCommentaire());
            paiement.setUtilisateurId(request.getUtilisateurId());

            PaiementFournisseur savedPaiement = paiementRepository.save(paiement);

            AchatPaiementLien lien = new AchatPaiementLien();
            lien.setAchatId(savedAchat.getId());
            lien.setPaiementId(savedPaiement.getId());
            lien.setMontantApplique(paiementImmediat);
            lien.setUtilisateurId(request.getUtilisateurId());
            achatPaiementLienRepository.save(lien);

            log.info("🔗 Lien créé: Paiement immédiat #{} → Achat #{} : {} F",
                    savedPaiement.getId(), savedAchat.getId(), paiementImmediat);
        }

        log.info("✅ Achat créé: id={}, total={}, paye={}, avanceUtilise={}, modePaiement={}, restant={}, statut={}",
                savedAchat.getId(), totalAchat, achat.getMontantPaye(), avanceUtilisee,
                savedAchat.getModePaiementImmediat(), savedAchat.getMontantRestant(), savedAchat.getStatut());
        log.info("=== FIN CRÉATION ACHAT ===");

        mettreAJourObjectifsApresAchat(savedAchat);

        return savedAchat;
    }

    private void mettreAJourObjectifsApresAchat(AchatFournisseur achat) {
        int mois = achat.getDateAchat().getMonthValue();
        int annee = achat.getDateAchat().getYear();
        Long fournisseurId = achat.getFournisseur().getId();

        List<ObjectifFournisseur> objectifs = objectifRepository
                .findByFournisseurIdAndMoisAndAnneeAndStockAjouteFalse(fournisseurId, mois, annee);

        if (objectifs.isEmpty()) return;

        for (LigneAchatFournisseur ligne : achat.getLignes()) {
            for (ObjectifFournisseur objectif : objectifs) {
                // Si l'objectif est lié à un produit spécifique → vérifier la correspondance
                if (objectif.getProduit() != null &&
                        !objectif.getProduit().getId().equals(ligne.getProduit().getId())) {
                    continue;
                }
                double nouvelleQte = (objectif.getQuantiteAtteinte() != null ? objectif.getQuantiteAtteinte() : 0.0)
                        + ligne.getQuantite();
                objectif.setQuantiteAtteinte(nouvelleQte);
                objectif.setBonusCalcule(nouvelleQte * objectif.getBonusParUnite());
                objectif.setStatut(nouvelleQte >= objectif.getObjectifQuantite()
                        ? StatutObjectif.ATTEINT : StatutObjectif.NON_ATTEINT);
                objectifRepository.save(objectif);
                log.info("Objectif #{} mis à jour automatiquement: qteAtteinte={}/{} statut={}",
                        objectif.getId(), nouvelleQte, objectif.getObjectifQuantite(), objectif.getStatut());
            }
        }
    }

    // ============================================
    // MÉTHODE 2 : PAYER FOURNISSEUR (MODIFIÉE POUR SUPPORTER achatCibleId)
    // ============================================
    @Transactional
    public PaiementFournisseur payerFournisseur(PaiementFournisseurRequest request) {
        log.info("=== DÉBUT PAIEMENT FOURNISSEUR ===");
        log.info("Paiement fournisseur request: {}", request);

        Fournisseur fournisseur = fournisseurRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Fournisseur introuvable"));

        log.info("Fournisseur trouvé: id={}, nom={}, solde avant={}", fournisseur.getId(), fournisseur.getNom(), fournisseur.getSolde());

        if (request.getMontant() <= 0) throw new IllegalArgumentException("Montant invalide");

        // Vérification du solde fournisseur
        if (fournisseur.getSolde() < request.getMontant() - 0.01) {
            throw new IllegalStateException("Le solde dû au fournisseur (" + fournisseur.getSolde() +
                    ") est inférieur au montant payé (" + request.getMontant() + ")");
        }

        // Créer et sauvegarder le paiement
        PaiementFournisseur paiement = new PaiementFournisseur();
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setFournisseur(fournisseur);
        paiement.setMontant(request.getMontant());
        paiement.setModePaiement(request.getModePaiement());
        paiement.setReference(request.getReference());
        paiement.setObservation(request.getObservation());
        paiement.setUtilisateurId(request.getUtilisateurId());

        if (request.getModePaiement() == ModePaiementFournisseur.ESPECES) {
            try {
                OperationCaisse sortie = caisseService.sortieCaisseFournisseur(
                        request.getMontant(),
                        "Paiement fournisseur - " + fournisseur.getNom(),
                        request.getUtilisateurId()
                );
                paiement.setOperationCaisse(sortie);
                log.info("Opération caisse créée pour paiement fournisseur");
            } catch (SoldeInsuffisantException e) {
                throw new SoldeInsuffisantException(
                        "Solde caisse insuffisant pour payer " + request.getMontant() +
                                ". Veuillez utiliser un autre mode de paiement."
                );
            }
        } else if (request.getModePaiement() == ModePaiementFournisseur.BANQUE) {
            if (request.getCompteId() == null)
                throw new IllegalArgumentException("Le compte bancaire est obligatoire pour un paiement par banque");
            compteService.debiterCompte(
                    request.getCompteId(),
                    request.getMontant(),
                    "Paiement fournisseur - " + fournisseur.getNom(),
                    TypeOperationCompte.PAIEMENT_FOURNISSEUR.toString(),
                    request.getUtilisateurId()
            );
            paiement.setCompteId(request.getCompteId());
            log.info("Débit compte bancaire id={} pour paiement fournisseur", request.getCompteId());
        }

        PaiementFournisseur savedPaiement = paiementRepository.save(paiement);
        log.info("Paiement sauvegardé: id={}, montant={}", savedPaiement.getId(), savedPaiement.getMontant());

        // ========== NOUVEAU: Si achatCibleId est spécifié, payer UNIQUEMENT cet achat ==========
        if (request.getAchatCibleId() != null) {
            log.info("Paiement spécifique pour l'achat #{}", request.getAchatCibleId());

            AchatFournisseur achatCible = achatRepository.findById(request.getAchatCibleId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Achat cible introuvable: " + request.getAchatCibleId()));

            // Vérifier que l'achat appartient bien au fournisseur
            if (!achatCible.getFournisseur().getId().equals(fournisseur.getId())) {
                throw new IllegalArgumentException("L'achat #" + request.getAchatCibleId() + " n'appartient pas à ce fournisseur");
            }

            double montantRestantAchat = achatCible.getMontantTotal() - achatCible.getMontantPaye();
            double montantApplique = Math.min(request.getMontant(), montantRestantAchat);

            // Créer le lien
            AchatPaiementLien lien = new AchatPaiementLien();
            lien.setAchatId(achatCible.getId());
            lien.setPaiementId(savedPaiement.getId());
            lien.setMontantApplique(montantApplique);
            lien.setUtilisateurId(request.getUtilisateurId());
            achatPaiementLienRepository.save(lien);

            // Mettre à jour l'achat
            double nouveauPaye = achatCible.getMontantPaye() + montantApplique;
            achatCible.setMontantPaye(nouveauPaye);
            achatCible.setMontantRestant(achatCible.getMontantTotal() - nouveauPaye);
            if (achatCible.getMontantRestant() <= 0.01) {
                achatCible.setStatut(StatutAchat.PAYE);
            }
            achatRepository.save(achatCible);

            log.info("🔗 Lien créé: Paiement #{} → Achat #{} : {} F",
                    savedPaiement.getId(), achatCible.getId(), montantApplique);

            // Mettre à jour le fournisseur (réduire le solde)
            fournisseur.setTotalPaye(fournisseur.getTotalPaye() + montantApplique);
            fournisseur.setSolde(fournisseur.getSolde() - montantApplique);
            fournisseurRepository.save(fournisseur);

            log.info("Fournisseur mis à jour: totalPaye={}, nouveau solde={}", fournisseur.getTotalPaye(), fournisseur.getSolde());

            log.info("=== FIN PAIEMENT FOURNISSEUR (spécifique) ===");
            return savedPaiement;
        }
        // ===================================================================================

        // Mode FIFO: répartir sur tous les achats non payés (ancien comportement)
        List<AchatFournisseur> achatsNonPayes = achatRepository.findByFournisseurIdAndStatutNot(fournisseur.getId(), StatutAchat.PAYE);
        achatsNonPayes.sort((a1, a2) -> a1.getDateAchat().compareTo(a2.getDateAchat()));

        log.info("Nombre d'achats non payés trouvés: {}", achatsNonPayes.size());

        double montantRestantAPayer = request.getMontant();
        log.info("Montant à répartir (FIFO): {}", montantRestantAPayer);

        for (AchatFournisseur achat : achatsNonPayes) {
            if (montantRestantAPayer <= 0.01) break;

            double montantDuPourCetAchat = achat.getMontantTotal() - achat.getMontantPaye();
            double montantApplique = Math.min(montantDuPourCetAchat, montantRestantAPayer);

            log.info("Achat id={}: total={}, dejaPaye={}, du={}, montantApplique={}, reste={}",
                    achat.getId(), achat.getMontantTotal(), achat.getMontantPaye(),
                    montantDuPourCetAchat, montantApplique, montantRestantAPayer - montantApplique);

            AchatPaiementLien lien = new AchatPaiementLien();
            lien.setAchatId(achat.getId());
            lien.setPaiementId(savedPaiement.getId());
            lien.setMontantApplique(montantApplique);
            lien.setUtilisateurId(request.getUtilisateurId());
            achatPaiementLienRepository.save(lien);
            log.info("🔗 Lien créé: Paiement #{} → Achat #{} : {} F",
                    savedPaiement.getId(), achat.getId(), montantApplique);

            if (montantDuPourCetAchat <= montantRestantAPayer + 0.01) {
                achat.setMontantPaye(achat.getMontantTotal());
                achat.setMontantRestant(0.0);
                achat.setStatut(StatutAchat.PAYE);
                montantRestantAPayer -= montantDuPourCetAchat;
                log.info("✅ Achat id={} entièrement payé!", achat.getId());
            } else {
                double nouveauPaye = achat.getMontantPaye() + montantRestantAPayer;
                achat.setMontantPaye(nouveauPaye);
                achat.setMontantRestant(achat.getMontantTotal() - nouveauPaye);
                log.info("⚠️ Achat id={} partiellement payé: nouveauPaye={}, restant={}",
                        achat.getId(), nouveauPaye, achat.getMontantRestant());
                montantRestantAPayer = 0;
            }
            achatRepository.save(achat);
        }

        fournisseur.setTotalPaye(fournisseur.getTotalPaye() + request.getMontant());
        fournisseur.setSolde(fournisseur.getSolde() - request.getMontant());
        fournisseurRepository.save(fournisseur);
        log.info("Fournisseur mis à jour: totalPaye={}, nouveau solde={}", fournisseur.getTotalPaye(), fournisseur.getSolde());

        // Vérification finale
        List<AchatFournisseur> tousLesAchats = achatRepository.findByFournisseurIdOrderByDateAchatDesc(fournisseur.getId());
        for (AchatFournisseur achat : tousLesAchats) {
            double restant = achat.getMontantTotal() - achat.getMontantPaye();
            if (restant <= 0.01 && achat.getStatut() != StatutAchat.PAYE) {
                achat.setStatut(StatutAchat.PAYE);
                achatRepository.save(achat);
                log.info("🔧 Correction finale: achat id={} marqué PAYE", achat.getId());
            }
        }

        log.info("=== FIN PAIEMENT FOURNISSEUR ===");
        return savedPaiement;
    }

    // ============================================
    // MÉTHODE 3 : ANNULER UN ACHAT
    // ============================================
    @Transactional
    public AchatFournisseur annulerAchat(Long achatId, Long utilisateurId) {
        log.info("=== ANNULATION ACHAT ID: {} ===", achatId);

        AchatFournisseur achat = achatRepository.findById(achatId)
                .orElseThrow(() -> new RessourceIntrouvableException("Achat introuvable: " + achatId));

        if (achat.getStatut() == StatutAchat.ANNULE) {
            throw new IllegalStateException("Cet achat est déjà annulé");
        }

        Fournisseur fournisseur = achat.getFournisseur();

        List<AchatPaiementLien> liens = achatPaiementLienRepository.findByAchatId(achatId);

        double totalRemboursementCaisse = 0.0;
        double totalRemboursementBanque = 0.0;
        Map<Long, Double> remboursementParCompte = new HashMap<>();

        log.info("Nombre de liens trouvés pour l'achat #{}: {}", achatId, liens.size());

        for (AchatPaiementLien lien : liens) {
            PaiementFournisseur paiement = paiementRepository.findById(lien.getPaiementId())
                    .orElse(null);
            if (paiement == null) {
                log.warn("Paiement #{} non trouvé pour le lien {}", lien.getPaiementId(), lien.getId());
                continue;
            }

            if (paiement.getModePaiement() == ModePaiementFournisseur.ESPECES) {
                totalRemboursementCaisse += lien.getMontantApplique();
                log.info("📋 Paiement #{} (ESPECES): {} F", lien.getPaiementId(), lien.getMontantApplique());
            } else if (paiement.getModePaiement() == ModePaiementFournisseur.BANQUE) {
                totalRemboursementBanque += lien.getMontantApplique();
                remboursementParCompte.merge(paiement.getCompteId(), lien.getMontantApplique(), Double::sum);
                log.info("📋 Paiement #{} (BANQUE, compte {}): {} F",
                        lien.getPaiementId(), paiement.getCompteId(), lien.getMontantApplique());
            }
        }

        double montantAvanceUtilise = achat.getMontantAvanceUtilise() != null ? achat.getMontantAvanceUtilise() : 0.0;

        log.info("Détails paiement - Avance utilisée: {} F, Paiements caisse: {} F, Paiements banque: {} F",
                montantAvanceUtilise, totalRemboursementCaisse, totalRemboursementBanque);

        List<LigneAchatFournisseur> lignes = achat.getLignes();
        if (lignes != null) {
            for (LigneAchatFournisseur ligne : lignes) {
                Produit produit = ligne.getProduit();
                if (produit == null) continue;
                int qteARetirer = ligne.getQuantite();
                int stockActuel = produit.getQuantite() != null ? produit.getQuantite() : 0;
                int qteEffective = Math.min(qteARetirer, stockActuel);
                if (qteEffective <= 0) {
                    log.warn("Stock déjà à 0 pour {} — aucune sortie créée", produit.getNom());
                    continue;
                }
                String motif = "Annulation achat fournisseur " + (fournisseur != null ? fournisseur.getNom() : "") + " - " + produit.getNom();
                inventaireService.sortieStock(produit.getId(), qteEffective, utilisateurId, motif);
                log.info("Stock retiré (annulation achat): -{} x {} (stock avant: {})", qteEffective, produit.getNom(), stockActuel);
            }
        }

        if (montantAvanceUtilise > 0.01) {
            log.info("Remboursement de l'avance: {} F pour le fournisseur {}", montantAvanceUtilise, fournisseur.getNom());
            avanceFournisseurService.annulerUtilisationAvance(fournisseur.getId(), montantAvanceUtilise);
            log.info("✅ Avance restituée: {} F", montantAvanceUtilise);
        } else {
            log.info("Aucune avance utilisée pour cet achat");
        }

        if (totalRemboursementCaisse > 0.01) {
            String motif = "Annulation achat #" + achatId + " - Remboursement espèces";
            caisseService.entreeCaisse(totalRemboursementCaisse, motif, utilisateurId, "ESPECES", "Annulation achat #" + achatId);
            log.info("✅ Remboursement caisse effectué: {} F retournés en caisse", totalRemboursementCaisse);
        }

        for (Map.Entry<Long, Double> entry : remboursementParCompte.entrySet()) {
            Long compteId = entry.getKey();
            Double montant = entry.getValue();
            String motif = "Annulation achat #" + achatId + " - Remboursement banque";
            compteService.crediterCompte(compteId, montant, motif,
                    TypeOperationCompte.REMBOURSEMENT_ACHAT.toString(), utilisateurId);
            log.info("✅ Remboursement banque effectué: {} F crédités sur le compte {}", montant, compteId);
        }

        double montantNonPaye = achat.getMontantRestant() != null ? achat.getMontantRestant() : 0.0;
        if (fournisseur != null) {
            double montantPayeAchat = achat.getMontantPaye() != null ? achat.getMontantPaye() : 0.0;
            fournisseur.setTotalAchats(Math.max(0, fournisseur.getTotalAchats() - achat.getMontantTotal()));
            fournisseur.setSolde(Math.max(0, fournisseur.getSolde() - montantNonPaye));
            fournisseur.setTotalPaye(Math.max(0, fournisseur.getTotalPaye() - montantPayeAchat));
            fournisseurRepository.save(fournisseur);
            log.info("Fournisseur ajusté: totalAchats={}, solde={}, totalPaye={}", fournisseur.getTotalAchats(), fournisseur.getSolde(), fournisseur.getTotalPaye());
        }

        achat.setStatut(StatutAchat.ANNULE);
        AchatFournisseur saved = achatRepository.save(achat);

        log.info("=== ANNULATION TERMINÉE AVEC SUCCÈS ===");
        log.info("Résumé: Stock retiré, Avance remboursée: {} F, Caisse remboursée: {} F, Banque remboursée: {} F",
                montantAvanceUtilise, totalRemboursementCaisse, totalRemboursementBanque);

        return saved;
    }

    // ============================================
    // MÉTHODES PRIVÉES
    // ============================================

    private Produit creerNouveauProduitDepuisAchat(LigneAchatRequest ligneReq) {
        if (ligneReq.getNouveauProduitNom() == null || ligneReq.getNouveauProduitNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du nouveau produit est requis");
        }
        if (ligneReq.getNouvelleCategorieId() == null) {
            throw new IllegalArgumentException("La catégorie du nouveau produit est requise");
        }

        Categorie categorie = categorieRepository.findById(ligneReq.getNouvelleCategorieId())
                .orElseThrow(() -> new RessourceIntrouvableException("Catégorie non trouvée"));

        Produit produit = new Produit();
        produit.setNom(ligneReq.getNouveauProduitNom().trim());
        produit.setDescription(ligneReq.getDescription() != null ? ligneReq.getDescription() : "");
        produit.setCategorie(categorie);
        produit.setPrixAchat(ligneReq.getPrixAchatUnitaire());

        if (ligneReq.getPrixVente() != null && ligneReq.getPrixVente() > 0) {
            produit.setPrixVente(ligneReq.getPrixVente());
        } else {
            produit.setPrixVente(ligneReq.getPrixAchatUnitaire() * 1.3);
        }

        produit.setQuantite(0);
        produit.setSeuilAlerte(ligneReq.getSeuilAlerte() != null ? ligneReq.getSeuilAlerte() : 10);
        produit.setCodeBarre(ligneReq.getCodeBarre());
        produit.setDateCreation(LocalDate.now());
        produit.setUniteMesure(ligneReq.getUniteMesure());
        produit.setBio(ligneReq.isBio());
        produit.setOrigine(ligneReq.getOrigine());
        produit.setTypeVente(ligneReq.getTypeVente() != null ? ligneReq.getTypeVente() : "DETAIL");

        return produitRepository.save(produit);
    }

    private Fournisseur creerNouveauFournisseur(FournisseurRequest request) {
        Fournisseur f = new Fournisseur();
        f.setNom(request.getNom());
        f.setCode(request.getCode());
        f.setAdresse(request.getAdresse());
        f.setTelephone(request.getTelephone());
        f.setEmail(request.getEmail());
        f.setSiteWeb(request.getSiteWeb());
        f.setContactNom(request.getContactNom());
        f.setContactTelephone(request.getContactTelephone());
        f.setContactEmail(request.getContactEmail());
        f.setDescription(request.getDescription());
        f.setTypeProduits(request.getTypeProduits());
        f.setConditionsPaiement(request.getConditionsPaiement());
        f.setDelaiLivraison(request.getDelaiLivraison());
        f.setNote(request.getNote());
        f.setActif(request.isActif());
        f.setTotalAchats(0.0);
        f.setTotalPaye(0.0);
        f.setSolde(0.0);
        return fournisseurRepository.save(f);
    }

    // ============================================
    // MÉTHODES DE CONSULTATION
    // ============================================

    @Transactional(readOnly = true)
    public FournisseurCompteDto getSituationFournisseur(Long fournisseurId) {
        return getSituationFournisseur(fournisseurId, null, null);
    }

    @Transactional(readOnly = true)
    public FournisseurCompteDto getSituationFournisseur(Long fournisseurId, String dateDebutStr, String dateFinStr) {
        log.info("=== Récupération situation fournisseur id={} (periode: {} -> {}) ===", fournisseurId, dateDebutStr, dateFinStr);

        Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Fournisseur introuvable"));

        FournisseurDto fournisseurDto = new FournisseurDto();
        fournisseurDto.setId(fournisseur.getId());
        fournisseurDto.setNom(fournisseur.getNom());
        fournisseurDto.setCode(fournisseur.getCode());
        fournisseurDto.setAdresse(fournisseur.getAdresse());
        fournisseurDto.setTelephone(fournisseur.getTelephone());
        fournisseurDto.setEmail(fournisseur.getEmail());
        fournisseurDto.setSiteWeb(fournisseur.getSiteWeb());
        fournisseurDto.setContactNom(fournisseur.getContactNom());
        fournisseurDto.setContactTelephone(fournisseur.getContactTelephone());
        fournisseurDto.setContactEmail(fournisseur.getContactEmail());
        fournisseurDto.setDescription(fournisseur.getDescription());
        fournisseurDto.setTypeProduits(fournisseur.getTypeProduits());
        fournisseurDto.setConditionsPaiement(fournisseur.getConditionsPaiement());
        fournisseurDto.setDelaiLivraison(fournisseur.getDelaiLivraison());
        fournisseurDto.setNote(fournisseur.getNote());
        fournisseurDto.setActif(fournisseur.isActif());

        int nombreProduits = (int) produitRepository.countByFournisseurId(fournisseur.getId());
        fournisseurDto.setNombreProduits((long) nombreProduits);

        boolean periodeFournie = dateDebutStr != null && !dateDebutStr.trim().isEmpty()
                && dateFinStr != null && !dateFinStr.trim().isEmpty();

        List<AchatFournisseur> achatsRecents;
        List<PaiementFournisseur> paiementsRecents;
        if (periodeFournie) {
            LocalDateTime dateDebut = LocalDate.parse(dateDebutStr).atStartOfDay();
            LocalDateTime dateFin = LocalDate.parse(dateFinStr).atTime(23, 59, 59);
            achatsRecents = achatRepository.findByFournisseurIdAndDateAchatBetweenOrderByDateAchatDesc(fournisseurId, dateDebut, dateFin);
            paiementsRecents = paiementRepository.findByFournisseurIdAndDatePaiementBetweenOrderByDatePaiementDesc(fournisseurId, dateDebut, dateFin);
        } else {
            achatsRecents = achatRepository.findByFournisseurIdOrderByDateAchatDesc(fournisseurId);
            paiementsRecents = paiementRepository.findByFournisseurIdOrderByDatePaiementDesc(fournisseurId);
        }

        for (AchatFournisseur achat : achatsRecents) {
            if (achat.getStatut() == StatutAchat.ANNULE) continue;
            double restantCalcule = achat.getMontantTotal() - achat.getMontantPaye();
            achat.setMontantRestant(restantCalcule);
            if (restantCalcule <= 0.01) {
                achat.setStatut(StatutAchat.PAYE);
            } else if (achat.getMontantPaye() > 0 && restantCalcule > 0.01) {
                achat.setStatut(StatutAchat.EN_COURS);
            }
        }

        List<AchatFournisseur> achatsSansFournisseur = new ArrayList<>();
        for (AchatFournisseur achat : achatsRecents) {
            AchatFournisseur achatCopy = new AchatFournisseur();
            achatCopy.setId(achat.getId());
            achatCopy.setDateAchat(achat.getDateAchat());
            achatCopy.setMontantTotal(achat.getMontantTotal());
            achatCopy.setMontantPaye(achat.getMontantPaye());
            achatCopy.setMontantRestant(achat.getMontantRestant());
            achatCopy.setStatut(achat.getStatut());
            achatCopy.setCommentaire(achat.getCommentaire());
            achatCopy.setDateCreation(achat.getDateCreation());
            achatsSansFournisseur.add(achatCopy);
        }

        List<PaiementFournisseur> paiementsSansFournisseur = new ArrayList<>();
        for (PaiementFournisseur paiement : paiementsRecents) {
            PaiementFournisseur paiementCopy = new PaiementFournisseur();
            paiementCopy.setId(paiement.getId());
            paiementCopy.setDatePaiement(paiement.getDatePaiement());
            paiementCopy.setMontant(paiement.getMontant());
            paiementCopy.setModePaiement(paiement.getModePaiement());
            paiementCopy.setReference(paiement.getReference());
            paiementCopy.setObservation(paiement.getObservation());
            paiementsSansFournisseur.add(paiementCopy);
        }

        FournisseurCompteDto dto = new FournisseurCompteDto();
        dto.setFournisseur(fournisseurDto);
        dto.setTotalAchats(fournisseur.getTotalAchats());
        dto.setTotalPaye(fournisseur.getTotalPaye());
        dto.setSolde(fournisseur.getSolde());
        dto.setAchatsRecents(achatsSansFournisseur);
        dto.setPaiementsRecents(paiementsSansFournisseur);

        log.info("Situation retournée: solde={}, {} achats, {} paiements",
                dto.getSolde(), achatsSansFournisseur.size(), paiementsSansFournisseur.size());
        return dto;
    }

    public List<AchatFournisseur> getHistoriqueAchats(Long fournisseurId) {
        return getHistoriqueAchats(fournisseurId, null, null);
    }

    public List<AchatFournisseur> getHistoriqueAchats(Long fournisseurId, String dateDebutStr, String dateFinStr) {
        List<AchatFournisseur> achats;
        if (dateDebutStr != null && !dateDebutStr.trim().isEmpty()
                && dateFinStr != null && !dateFinStr.trim().isEmpty()) {
            LocalDateTime dateDebut = LocalDate.parse(dateDebutStr).atStartOfDay();
            LocalDateTime dateFin = LocalDate.parse(dateFinStr).atTime(23, 59, 59);
            achats = achatRepository.findByFournisseurIdAndDateAchatBetweenOrderByDateAchatDesc(fournisseurId, dateDebut, dateFin);
        } else {
            achats = achatRepository.findByFournisseurIdOrderByDateAchatDesc(fournisseurId);
        }
        for (AchatFournisseur achat : achats) {
            if (achat.getStatut() == StatutAchat.ANNULE) continue;
            double restantCalcule = achat.getMontantTotal() - achat.getMontantPaye();
            achat.setMontantRestant(restantCalcule);
            if (restantCalcule <= 0.01 && achat.getStatut() != StatutAchat.PAYE) {
                achat.setStatut(StatutAchat.PAYE);
            }
        }
        return achats;
    }

    public List<PaiementFournisseur> getHistoriquePaiements(Long fournisseurId) {
        return getHistoriquePaiements(fournisseurId, null, null);
    }

    public List<PaiementFournisseur> getHistoriquePaiements(Long fournisseurId, String dateDebutStr, String dateFinStr) {
        if (dateDebutStr != null && !dateDebutStr.trim().isEmpty()
                && dateFinStr != null && !dateFinStr.trim().isEmpty()) {
            LocalDateTime dateDebut = LocalDate.parse(dateDebutStr).atStartOfDay();
            LocalDateTime dateFin = LocalDate.parse(dateFinStr).atTime(23, 59, 59);
            return paiementRepository.findByFournisseurIdAndDatePaiementBetweenOrderByDatePaiementDesc(fournisseurId, dateDebut, dateFin);
        }
        return paiementRepository.findByFournisseurIdOrderByDatePaiementDesc(fournisseurId);
    }

    @Transactional(readOnly = true)
    public List<AchatFournisseur> getAchatsNonPayes(Long fournisseurId) {
        List<AchatFournisseur> achats = achatRepository.findAchatsNonPayesByFournisseurId(fournisseurId, StatutAchat.EN_COURS);
        for (AchatFournisseur achat : achats) {
            double restantCalcule = achat.getMontantTotal() - achat.getMontantPaye();
            achat.setMontantRestant(restantCalcule);
        }
        return achats;
    }

    // ============================================
    // MÉTHODE 4 : ANNULER UN PAIEMENT FOURNISSEUR
    // ============================================
    @Transactional
    public PaiementFournisseur annulerPaiementFournisseur(Long paiementId, Long utilisateurId) {
        PaiementFournisseur paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Paiement introuvable: " + paiementId));

        if (paiement.isAnnule()) {
            throw new IllegalStateException("Ce paiement est déjà annulé");
        }

        Fournisseur fournisseur = paiement.getFournisseur();

        // Remboursement selon le mode de paiement
        if (paiement.getModePaiement() == ModePaiementFournisseur.ESPECES) {
            caisseService.entreeCaisse(
                    paiement.getMontant(),
                    "Annulation paiement fournisseur " + fournisseur.getNom() + " #" + paiementId,
                    utilisateurId,
                    "ESPECES",
                    "Annulation paiement #" + paiementId
            );
        } else if (paiement.getModePaiement() == ModePaiementFournisseur.BANQUE && paiement.getCompteId() != null) {
            compteService.crediterCompte(
                    paiement.getCompteId(),
                    paiement.getMontant(),
                    "Annulation paiement fournisseur " + fournisseur.getNom() + " #" + paiementId,
                    TypeOperationCompte.REMBOURSEMENT_ACHAT.toString(),
                    utilisateurId
            );
        }

        // Rétablir la dette fournisseur
        fournisseur.setTotalPaye(Math.max(0.0, fournisseur.getTotalPaye() - paiement.getMontant()));
        fournisseur.setSolde(fournisseur.getSolde() + paiement.getMontant());
        fournisseurRepository.save(fournisseur);

        // Remettre les achats liés en statut non-payé
        List<AchatPaiementLien> liens = achatPaiementLienRepository.findByPaiementId(paiementId);
        for (AchatPaiementLien lien : liens) {
            achatRepository.findById(lien.getAchatId()).ifPresent(achat -> {
                if (achat.getStatut() != StatutAchat.ANNULE) {
                    double nouveauPaye = Math.max(0.0, achat.getMontantPaye() - lien.getMontantApplique());
                    achat.setMontantPaye(nouveauPaye);
                    achat.setMontantRestant(achat.getMontantTotal() - nouveauPaye);
                    achat.setStatut(achat.getMontantRestant() > 0.01 ? StatutAchat.EN_COURS : StatutAchat.PAYE);
                    achatRepository.save(achat);
                }
            });
        }

        paiement.setAnnule(true);
        paiement.setDateAnnulation(LocalDateTime.now());
        paiement.setMotifAnnulation("Annulé par utilisateur " + utilisateurId);
        log.info("Paiement fournisseur #{} annulé par utilisateur {}", paiementId, utilisateurId);
        return paiementRepository.save(paiement);
    }

    // ============================================
    // MÉTHODE 5 : PAIEMENTS PAR PÉRIODE
    // ============================================
    @Transactional(readOnly = true)
    public List<PaiementFournisseur> getPaiementsParPeriode(String dateDebutStr, String dateFinStr) {
        if (dateDebutStr != null && dateFinStr != null) {
            LocalDateTime dateDebut = LocalDate.parse(dateDebutStr).atStartOfDay();
            LocalDateTime dateFin = LocalDate.parse(dateFinStr).atTime(23, 59, 59);
            return paiementRepository.findByPeriode(dateDebut, dateFin);
        }
        return paiementRepository.findAllByOrderByDatePaiementDesc();
    }
}