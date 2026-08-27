package com.ges.boutique.fournisseur;

import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.compte.CompteService;
import com.ges.boutique.compte.TypeOperationCompte;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.inventaire.InventaireService;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetourAchatServiceImpl {

    private final AchatFournisseurRepository achatRepository;
    private final LigneAchatFournisseurRepository ligneAchatRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;
    private final RetourAchatRepository retourAchatRepository;
    private final CaisseService caisseService;
    private final CompteService compteService;
    private final InventaireService inventaireService;

    @Transactional
    public RetourAchat effectuerRetour(RetourAchatRequest request) {
        AchatFournisseur achat = achatRepository.findById(request.getAchatId())
                .orElseThrow(() -> new RessourceIntrouvableException("Achat introuvable: " + request.getAchatId()));

        // BUG FIX (audit comptable/stock) : un achat déjà annulé n'a plus de dette réelle et son
        // stock a déjà été retiré — traiter un retour dessus corromprait le stock une seconde
        // fois. Même garde que payerFournisseur()/annulerPaiementFournisseur().
        if (achat.getStatut() == StatutAchat.ANNULE) {
            throw new IllegalStateException("Impossible d'effectuer un retour sur l'achat #" + achat.getId() + " : cet achat est annulé");
        }

        Fournisseur fournisseur = achat.getFournisseur();
        if (fournisseur == null) {
            fournisseur = fournisseurRepository.findById(
                    achat.getFournisseur().getId()
            ).orElseThrow(() -> new RessourceIntrouvableException("Fournisseur introuvable"));
        }

        if (request.getLignes() == null || request.getLignes().isEmpty()) {
            throw new IllegalArgumentException("Au moins une ligne de retour est requise");
        }
        if (request.getModeRemboursement() == null ||
                (!request.getModeRemboursement().equals("CAISSE") && !request.getModeRemboursement().equals("BANQUE"))) {
            throw new IllegalArgumentException("Mode de remboursement invalide (CAISSE ou BANQUE)");
        }
        if ("BANQUE".equals(request.getModeRemboursement()) && request.getCompteId() == null) {
            throw new IllegalArgumentException("Compte bancaire requis pour un remboursement par banque");
        }

        RetourAchat retour = new RetourAchat();
        retour.setAchat(achat);
        retour.setFournisseur(fournisseur);
        retour.setMotif(request.getMotif());
        retour.setModeRemboursement(request.getModeRemboursement());
        retour.setCompteId(request.getCompteId());
        retour.setUtilisateurId(request.getUtilisateurId());

        double totalRetour = 0.0;
        List<LigneRetourAchat> lignes = new ArrayList<>();

        for (RetourAchatRequest.LigneRetourAchatRequest ligneReq : request.getLignes()) {
            Produit produit = produitRepository.findById(ligneReq.getProduitId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable: " + ligneReq.getProduitId()));

            if (ligneReq.getQuantiteRetournee() == null || ligneReq.getQuantiteRetournee() <= 0) {
                throw new IllegalArgumentException("Quantité invalide pour le produit: " + produit.getNom());
            }

            if (ligneReq.getLigneAchatId() != null) {
                LigneAchatFournisseur ligneOrigine = ligneAchatRepository.findById(ligneReq.getLigneAchatId())
                        .orElse(null);
                if (ligneOrigine != null && ligneReq.getQuantiteRetournee() > ligneOrigine.getQuantite()) {
                    throw new IllegalArgumentException(
                            "Quantité retournée (" + ligneReq.getQuantiteRetournee() +
                                    ") dépasse la quantité achetée (" + ligneOrigine.getQuantite() +
                                    ") pour: " + produit.getNom());
                }
            }

            LigneRetourAchat ligne = new LigneRetourAchat();
            ligne.setRetour(retour);
            ligne.setProduit(produit);
            ligne.setLigneAchatId(ligneReq.getLigneAchatId());
            ligne.setQuantiteRetournee(ligneReq.getQuantiteRetournee());
            ligne.setPrixUnitaire(ligneReq.getPrixUnitaire());
            ligne.setSousTotal(ligneReq.getQuantiteRetournee() * ligneReq.getPrixUnitaire());
            totalRetour += ligne.getSousTotal();
            lignes.add(ligne);

            // BUG FIX (audit comptable/stock) : la mutation directe de produit.getQuantite() ne
            // touchait jamais ProduitNiveau.stock (désynchronisation cascade, même famille de
            // bug que l'annulation d'achat) et ne créait aucun MouvementStock — un retour
            // fournisseur était donc invisible dans l'historique des mouvements de stock,
            // contrairement à toutes les autres opérations du système. inventaireService.sortieStock
            // est niveau-aware et trace le mouvement.
            String motifStock = "Retour achat fournisseur #" + achat.getId() + " - " + produit.getNom();
            inventaireService.sortieStock(produit.getId(), ligneReq.getQuantiteRetournee(), request.getUtilisateurId(), motifStock, "RETOUR_ACHAT");
        }

        retour.setLignes(lignes);
        retour.setMontantTotal(totalRetour);

        double montantRestantActuel = achat.getMontantRestant() != null ? achat.getMontantRestant() : 0.0;
        double montantDetteReduit;
        double montantRembourse;

        if (totalRetour <= montantRestantActuel) {
            montantDetteReduit = totalRetour;
            montantRembourse = 0.0;
        } else {
            montantDetteReduit = montantRestantActuel;
            montantRembourse = totalRetour - montantRestantActuel;
        }

        retour.setMontantDetteReduit(montantDetteReduit);
        retour.setMontantRembourse(montantRembourse);

        double newMontantTotal = Math.max(0, achat.getMontantTotal() - totalRetour);
        double newMontantPaye = Math.min(achat.getMontantPaye(), newMontantTotal);
        double newMontantRestant = newMontantTotal - newMontantPaye;
        achat.setMontantTotal(newMontantTotal);
        achat.setMontantPaye(newMontantPaye);
        achat.setMontantRestant(newMontantRestant);
        if (newMontantRestant <= 0.01) achat.setStatut(StatutAchat.PAYE);
        achatRepository.save(achat);

        fournisseur.setTotalAchats(Math.max(0, fournisseur.getTotalAchats() - totalRetour));
        fournisseur.setSolde(Math.max(0, fournisseur.getSolde() - montantDetteReduit));
        if (montantRembourse > 0) {
            fournisseur.setTotalPaye(Math.max(0, fournisseur.getTotalPaye() - montantRembourse));
        }
        fournisseurRepository.save(fournisseur);

        // ========== PARTIE CORRIGÉE ==========
        if (montantRembourse > 0.01) {
            String motifOp = "Retour achat fournisseur " + fournisseur.getNom() +
                    " - " + (request.getMotif() != null ? request.getMotif() : "retour marchandise");

            if ("BANQUE".equals(request.getModeRemboursement())) {
                // CORRECTION: Convertir l'enum en String avec .toString()
                compteService.crediterCompte(
                        request.getCompteId(),
                        montantRembourse,
                        motifOp,
                        TypeOperationCompte.VERSEMENT.toString(),  // <-- AJOUT DE .toString()
                        request.getUtilisateurId()
                );
                log.info("Remboursement banque: {} F vers compte {}", montantRembourse, request.getCompteId());
            } else {
                caisseService.entreeCaisse(montantRembourse, motifOp, request.getUtilisateurId(), "ESPECES", null);
                log.info("Remboursement caisse: {} F", montantRembourse);
            }
        }

        RetourAchat saved = retourAchatRepository.save(retour);
        log.info("Retour achat créé: id={}, total={}, rembourse={}, dette_reduit={}",
                saved.getId(), totalRetour, montantRembourse, montantDetteReduit);
        return saved;
    }

    public List<RetourAchat> getRetoursByFournisseur(Long fournisseurId) {
        return retourAchatRepository.findByFournisseurIdOrderByDateRetourDesc(fournisseurId);
    }

    public List<RetourAchat> getRetoursByAchat(Long achatId) {
        return retourAchatRepository.findByAchatIdOrderByDateRetourDesc(achatId);
    }

    public List<RetourAchat> getAllRetours() {
        return retourAchatRepository.findAll();
    }
}