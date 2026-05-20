package com.ges.boutique.fournisseur;

import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.caisse.OperationCaisse;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.exception.SoldeInsuffisantException;
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
import java.util.List;

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

    @Transactional
    public AchatFournisseur creerAchat(AchatFournisseurRequest request) {
        log.info("Création d'un achat fournisseur: {}", request);

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
        achat.setMontantPaye(request.getMontantPaye() != null ? request.getMontantPaye() : 0.0);

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
            produit.setQuantite(produit.getQuantite() + ligneReq.getQuantite());
            produitRepository.save(produit);
        }

        achat.setMontantTotal(totalAchat);
        achat.setMontantRestant(totalAchat - achat.getMontantPaye());
        if (achat.getMontantRestant() <= 0.01) {
            achat.setStatut(StatutAchat.PAYE);
        } else {
            achat.setStatut(StatutAchat.EN_COURS);
        }

        fournisseur.setTotalAchats(fournisseur.getTotalAchats() + totalAchat);
        fournisseur.setSolde(fournisseur.getSolde() + totalAchat - achat.getMontantPaye());
        fournisseur.setTotalPaye(fournisseur.getTotalPaye() + achat.getMontantPaye());
        fournisseurRepository.save(fournisseur);

        AchatFournisseur savedAchat = achatRepository.save(achat);
        log.info("Achat créé: id={}, total={}, paye={}, restant={}, statut={}",
                savedAchat.getId(), totalAchat, achat.getMontantPaye(), achat.getMontantRestant(), achat.getStatut());

        return savedAchat;
    }

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

    @Transactional
    public PaiementFournisseur payerFournisseur(PaiementFournisseurRequest request) {
        log.info("=== DÉBUT PAIEMENT FOURNISSEUR ===");
        log.info("Paiement fournisseur request: {}", request);

        Fournisseur fournisseur = fournisseurRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Fournisseur introuvable"));

        log.info("Fournisseur trouvé: id={}, nom={}, solde avant={}", fournisseur.getId(), fournisseur.getNom(), fournisseur.getSolde());

        if (request.getMontant() <= 0) throw new IllegalArgumentException("Montant invalide");
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
                OperationCaisse sortie = caisseService.sortieCaisse(
                        request.getMontant(),
                        "Paiement fournisseur - " + fournisseur.getNom(),
                        request.getUtilisateurId()
                );
                paiement.setOperationCaisse(sortie);
                log.info("Opération caisse créée pour paiement espèces");
            } catch (SoldeInsuffisantException e) {
                throw new SoldeInsuffisantException(
                        "Solde caisse insuffisant pour payer " + request.getMontant() +
                                ". Veuillez utiliser un autre mode de paiement (VIREMENT, CHEQUE)."
                );
            }
        }

        PaiementFournisseur savedPaiement = paiementRepository.save(paiement);
        log.info("Paiement sauvegardé: id={}, montant={}", savedPaiement.getId(), savedPaiement.getMontant());

        // Récupérer tous les achats non payés du fournisseur (FIFO - les plus anciens d'abord)
        List<AchatFournisseur> achatsNonPayes = achatRepository.findByFournisseurIdAndStatutNot(fournisseur.getId(), StatutAchat.PAYE);
        achatsNonPayes.sort((a1, a2) -> a1.getDateAchat().compareTo(a2.getDateAchat()));

        log.info("Nombre d'achats non payés trouvés: {}", achatsNonPayes.size());

        double montantRestantAPayer = request.getMontant();
        log.info("Montant à répartir: {}", montantRestantAPayer);

        // Répartir le paiement sur les achats non payés
        for (AchatFournisseur achat : achatsNonPayes) {
            if (montantRestantAPayer <= 0.01) {
                break;
            }

            double montantDuPourCetAchat = achat.getMontantTotal() - achat.getMontantPaye();
            log.info("Achat id={}: total={}, dejaPaye={}, du={}, montantRestantAPayer={}",
                    achat.getId(), achat.getMontantTotal(), achat.getMontantPaye(), montantDuPourCetAchat, montantRestantAPayer);

            if (montantDuPourCetAchat <= montantRestantAPayer + 0.01) {
                // Ce paiement couvre entièrement cet achat
                achat.setMontantPaye(achat.getMontantTotal());
                achat.setMontantRestant(0.0);
                achat.setStatut(StatutAchat.PAYE);
                montantRestantAPayer -= montantDuPourCetAchat;
                log.info("✅ Achat id={} entièrement payé! Nouveau statut=PAYE", achat.getId());
            } else {
                // Paiement partiel de cet achat
                double nouveauPaye = achat.getMontantPaye() + montantRestantAPayer;
                achat.setMontantPaye(nouveauPaye);
                achat.setMontantRestant(achat.getMontantTotal() - nouveauPaye);
                achat.setStatut(StatutAchat.EN_COURS);
                log.info("⚠️ Achat id={} partiellement payé: nouveauPaye={}, restant={}",
                        achat.getId(), nouveauPaye, achat.getMontantRestant());
                montantRestantAPayer = 0;
            }
            achatRepository.save(achat);
        }

        // Mettre à jour le fournisseur APRÈS avoir mis à jour les achats
        fournisseur.setTotalPaye(fournisseur.getTotalPaye() + request.getMontant());
        fournisseur.setSolde(fournisseur.getSolde() - request.getMontant());
        fournisseurRepository.save(fournisseur);
        log.info("Fournisseur mis à jour: totalPaye={}, nouveau solde={}", fournisseur.getTotalPaye(), fournisseur.getSolde());

        // Vérification finale: s'assurer que tous les achats avec restant <= 0 sont marqués PAYES
        List<AchatFournisseur> tousLesAchats = achatRepository.findByFournisseurIdOrderByDateAchatDesc(fournisseur.getId());
        for (AchatFournisseur achat : tousLesAchats) {
            double restant = achat.getMontantTotal() - achat.getMontantPaye();
            if (restant <= 0.01 && achat.getStatut() != StatutAchat.PAYE) {
                achat.setStatut(StatutAchat.PAYE);
                achatRepository.save(achat);
                log.info("🔧 Correction finale: achat id={} marqué PAYE", achat.getId());
            }
            log.info("État final achat id={}: total={}, paye={}, restant={}, statut={}",
                    achat.getId(), achat.getMontantTotal(), achat.getMontantPaye(),
                    achat.getMontantTotal() - achat.getMontantPaye(), achat.getStatut());
        }

        log.info("=== FIN PAIEMENT FOURNISSEUR ===");
        return savedPaiement;
    }

    @Transactional(readOnly = true)
    public FournisseurCompteDto getSituationFournisseur(Long fournisseurId) {
        log.info("=== Récupération situation fournisseur id={} ===", fournisseurId);

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

        List<AchatFournisseur> achatsRecents = achatRepository.findByFournisseurIdOrderByDateAchatDesc(fournisseurId);

        // Recalculer les montants restants et statuts
        for (AchatFournisseur achat : achatsRecents) {
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

        List<PaiementFournisseur> paiementsRecents = paiementRepository.findByFournisseurIdOrderByDatePaiementDesc(fournisseurId);
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
        List<AchatFournisseur> achats = achatRepository.findByFournisseurIdOrderByDateAchatDesc(fournisseurId);
        for (AchatFournisseur achat : achats) {
            double restantCalcule = achat.getMontantTotal() - achat.getMontantPaye();
            achat.setMontantRestant(restantCalcule);
            if (restantCalcule <= 0.01 && achat.getStatut() != StatutAchat.PAYE) {
                achat.setStatut(StatutAchat.PAYE);
            }
        }
        return achats;
    }

    public List<PaiementFournisseur> getHistoriquePaiements(Long fournisseurId) {
        return paiementRepository.findByFournisseurIdOrderByDatePaiementDesc(fournisseurId);
    }
}