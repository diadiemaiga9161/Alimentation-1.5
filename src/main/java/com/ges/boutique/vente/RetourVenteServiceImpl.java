package com.ges.boutique.vente;

import com.ges.boutique.avance.AvanceClientService;
import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.inventaire.InventaireService;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetourVenteServiceImpl {

    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final ProduitRepository produitRepository;
    private final RetourVenteRepository retourVenteRepository;
    private final CaisseService caisseService;
    private final AvanceClientService avanceClientService;
    private final InventaireService inventaireService;

    @Transactional
    public RetourVente effectuerRetour(RetourVenteRequest request) {
        log.info("=== EFFETUER RETOUR ===");
        log.info("Vente ID: {}", request.getVenteId());

        Vente vente = venteRepository.findById(request.getVenteId())
                .orElseThrow(() -> new RessourceIntrouvableException("Vente introuvable: " + request.getVenteId()));

        if (Boolean.TRUE.equals(vente.getAnnulee())) {
            throw new IllegalArgumentException("Impossible de faire un retour sur une vente annulée");
        }

        if (request.getLignes() == null || request.getLignes().isEmpty()) {
            throw new IllegalArgumentException("Au moins une ligne de retour est requise");
        }

        RetourVente retour = new RetourVente();
        retour.setVente(vente);
        retour.setMotif(request.getMotif());
        retour.setUtilisateurId(request.getUtilisateurId());
        retour.setDateRetour(LocalDateTime.now());

        if (vente.getClient() != null) {
            retour.setClientNom(vente.getClient().getNom());
        } else if (vente.getClientNom() != null) {
            retour.setClientNom(vente.getClientNom());
        } else {
            retour.setClientNom("Client divers");
        }

        double totalRetour = 0.0;
        List<LigneRetourVente> lignes = new ArrayList<>();

        for (RetourVenteRequest.LigneRetourVenteRequest ligneReq : request.getLignes()) {
            log.info("Traitement ligne - Produit ID: {}", ligneReq.getProduitId());

            Produit produit = produitRepository.findById(ligneReq.getProduitId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable: " + ligneReq.getProduitId()));

            log.info("Produit trouvé: ID={}, Nom={}", produit.getId(), produit.getNom());

            if (ligneReq.getQuantiteRetournee() == null || ligneReq.getQuantiteRetournee() <= 0) {
                throw new IllegalArgumentException("Quantité invalide pour: " + produit.getNom());
            }

            LigneRetourVente ligne = new LigneRetourVente();
            ligne.setRetour(retour);
            ligne.setProduit(produit);
            ligne.setLigneVenteId(ligneReq.getLigneVenteId());
            ligne.setQuantiteRetournee(ligneReq.getQuantiteRetournee());
            ligne.setPrixUnitaire(ligneReq.getPrixUnitaire());
            ligne.setSousTotal(ligneReq.getQuantiteRetournee() * ligneReq.getPrixUnitaire());
            totalRetour += ligne.getSousTotal();
            lignes.add(ligne);

            log.info("Ligne retour créée - Produit: {}, Quantité: {}, Sous-total: {}",
                    produit.getNom(), ligneReq.getQuantiteRetournee(), ligne.getSousTotal());

            String motifRetourStock = "Retour vente " + vente.getNumeroVente() + " - " + produit.getNom();
            inventaireService.retourStock(produit.getId(), ligneReq.getQuantiteRetournee(), request.getUtilisateurId(), motifRetourStock);
        }

        retour.setLignes(lignes);
        retour.setMontantTotal(totalRetour);

        String motifRemboursement = "Retour vente " + vente.getNumeroVente() +
                " - " + retour.getClientNom() +
                (request.getMotif() != null ? " - " + request.getMotif() : "");

        double montantAvanceUtilise = vente.getMontantAvanceUtilise() != null ? vente.getMontantAvanceUtilise() : 0.0;
        String clientNom = vente.getClientNom() != null ? vente.getClientNom() :
                (vente.getClient() != null ? vente.getClient().getNom() : null);

        double montantAvanceRetourne = Math.min(totalRetour, montantAvanceUtilise);
        double montantCaisseRetourne = totalRetour - montantAvanceRetourne;

        if (montantAvanceRetourne > 0 && clientNom != null) {
            avanceClientService.remettreAvance(clientNom, montantAvanceRetourne);
            vente.setMontantAvanceUtilise(Math.max(0, montantAvanceUtilise - montantAvanceRetourne));
        }

        if (Boolean.TRUE.equals(vente.getEstCredit())) {
            // Vente à crédit : la part du retour non couverte par une avance n'a
            // jamais été encaissée (le client n'a pas encore payé) -> on réduit
            // simplement le solde restant à payer, la caisse n'est pas touchée.
            double montantRetourneCumule = (vente.getMontantRetourne() != null ? vente.getMontantRetourne() : 0.0) + montantCaisseRetourne;
            vente.setMontantRetourne(montantRetourneCumule);
            // montantRestant/creditRegle sont recalculés automatiquement (dans les deux sens)
            // par le hook @PreUpdate de Vente au moment du save ci-dessous — pas besoin de les
            // recalculer ici (voir Vente.onUpdate()).
        } else if (montantCaisseRetourne > 0) {
            // Vente comptant : la somme avait bien été encaissée à la vente, elle est remboursée depuis la caisse.
            caisseService.sortieCaisseRemboursementRetour(montantCaisseRetourne, motifRemboursement, request.getUtilisateurId());
        }

        // Un retour peut ne concerner qu'une partie des produits de la vente
        // (ex: 3 articles vendus, 1 seul retourné). On ne marque la vente comme
        // "Retourné" (totalement) que si TOUTE la quantité vendue a été rendue ;
        // sinon c'est un retour partiel, affiché différemment côté UI.
        int quantiteVendueTotale = vente.getLignes() == null ? 0 :
                vente.getLignes().stream().mapToInt(l -> l.getQuantite() != null ? l.getQuantite() : 0).sum();

        int quantiteDejaRetournee = retourVenteRepository.findByVenteIdOrderByDateRetourDesc(vente.getId()).stream()
                .flatMap(r -> r.getLignes().stream())
                .mapToInt(l -> l.getQuantiteRetournee() != null ? l.getQuantiteRetournee() : 0)
                .sum();
        int quantiteRetourneeCetteFois = lignes.stream()
                .mapToInt(l -> l.getQuantiteRetournee() != null ? l.getQuantiteRetournee() : 0)
                .sum();
        int quantiteRetourneeTotale = quantiteDejaRetournee + quantiteRetourneeCetteFois;

        vente.setEstRetourne(true);
        vente.setRetourPartiel(quantiteVendueTotale > 0 && quantiteRetourneeTotale < quantiteVendueTotale);
        venteRepository.save(vente);

        RetourVente saved = retourVenteRepository.save(retour);
        log.info("Retour sauvegardé avec ID: {}", saved.getId());

        return saved;
    }

    public List<RetourVente> getRetoursByVente(Long venteId) {
        return retourVenteRepository.findByVenteIdOrderByDateRetourDesc(venteId);
    }

    public List<RetourVente> getAllRetours() {
        return retourVenteRepository.findAllByOrderByDateRetourDesc();
    }
}