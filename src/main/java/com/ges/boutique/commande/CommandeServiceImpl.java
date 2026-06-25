package com.ges.boutique.commande;

import com.ges.boutique.client.Client;
import com.ges.boutique.client.ClientRepository;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import com.ges.boutique.vente.*;
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
public class CommandeServiceImpl implements CommandeService {

    private final CommandeRepository commandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final VenteService venteService;

    @Override
    @Transactional
    public Commande creer(CommandeRequest request) {
        Commande commande = new Commande();
        commande.setStatut(StatutCommande.BROUILLON);
        commande.setModePaiement(request.getModePaiement());
        commande.setReferencePaiement(request.getReferencePaiement());
        commande.setEstCredit(request.getEstCredit());
        commande.setMontantVerse(request.getMontantVerse() != null ? request.getMontantVerse() : 0.0);
        commande.setDateEcheance(request.getDateEcheance());
        commande.setNotes(request.getNotes());

        // Vendeur
        if (request.getVendeurId() != null) {
            Utilisateur vendeur = utilisateurRepository.findById(request.getVendeurId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Vendeur non trouvé"));
            commande.setVendeur(vendeur);
        }

        // Client
        setClient(commande, request);

        // Lignes
        List<LigneCommande> lignes = buildLignes(request.getLignes(), commande);
        commande.setLignes(lignes);
        commande.recalculer();

        return commandeRepository.save(commande);
    }

    @Override
    @Transactional
    public Commande modifier(Long id, CommandeRequest request) {
        Commande commande = findById(id);

        commande.setModePaiement(request.getModePaiement());
        commande.setReferencePaiement(request.getReferencePaiement());
        commande.setEstCredit(request.getEstCredit());
        commande.setMontantVerse(request.getMontantVerse() != null ? request.getMontantVerse() : 0.0);
        commande.setDateEcheance(request.getDateEcheance());
        commande.setNotes(request.getNotes());

        // Client
        setClient(commande, request);

        // Lignes — remplacer complètement
        commande.getLignes().clear();
        List<LigneCommande> nouvellesLignes = buildLignes(request.getLignes(), commande);
        commande.getLignes().addAll(nouvellesLignes);
        commande.recalculer();

        return commandeRepository.save(commande);
    }

    @Override
    @Transactional
    public Commande valider(Long id) {
        Commande commande = findById(id);
        if (commande.getStatut() == StatutCommande.VALIDEE) {
            throw new IllegalStateException("Cette commande est déjà validée");
        }

        // Construire une VenteRequest depuis la commande
        VenteRequest venteRequest = new VenteRequest();
        venteRequest.setVendeurId(commande.getVendeurId());
        venteRequest.setClientId(commande.getClientId());
        venteRequest.setClientNom(commande.getClientNom());
        venteRequest.setClientPrenom(commande.getClientPrenom());
        venteRequest.setClientTelephone(commande.getClientTelephone());
        venteRequest.setModePaiement(commande.getModePaiement());
        venteRequest.setReferencePaiement(commande.getReferencePaiement());
        venteRequest.setEstCredit(commande.getEstCredit());
        venteRequest.setMontantVerse(commande.getMontantVerse());
        venteRequest.setDateEcheance(commande.getDateEcheance());
        venteRequest.setClientDivers(commande.getClientId() == null);

        List<LigneVenteRequest> lignesVente = new ArrayList<>();
        for (LigneCommande lc : commande.getLignes()) {
            LigneVenteRequest lv = new LigneVenteRequest();
            lv.setProduitId(lc.getProduitId());
            lv.setQuantite(lc.getQuantite());
            lv.setPrixUnitaire(lc.getPrixUnitaire());
            lv.setPrixAchat(lc.getPrixAchat());
            lignesVente.add(lv);
        }
        venteRequest.setLignes(lignesVente);

        // Créer la vente → décrémente le stock + caisse
        Vente vente = venteService.creerVente(venteRequest);

        // Marquer la commande comme validée
        commande.setStatut(StatutCommande.VALIDEE);
        commande.setDateValidation(LocalDateTime.now());
        commande.setVenteId(vente.getId());

        return commandeRepository.save(commande);
    }

    @Override
    @Transactional
    public void supprimer(Long id) {
        Commande commande = findById(id);
        if (commande.getStatut() == StatutCommande.VALIDEE) {
            throw new IllegalStateException("Impossible de supprimer une commande validée");
        }
        commandeRepository.deleteById(id);
    }

    @Override
    public Commande findById(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Commande non trouvée: " + id));
    }

    @Override
    public List<Commande> findAll() {
        return commandeRepository.findAllByOrderByDateCommandeDesc();
    }

    @Override
    public List<Commande> findByStatut(StatutCommande statut) {
        return commandeRepository.findByStatutOrderByDateCommandeDesc(statut);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void setClient(Commande commande, CommandeRequest request) {
        if (request.getClientId() != null) {
            clientRepository.findById(request.getClientId()).ifPresent(client -> {
                commande.setClient(client);
                commande.setClientNom(client.getNom());
                commande.setClientPrenom(client.getPrenom());
                commande.setClientTelephone(client.getNumeroTelephone());
            });
        } else {
            commande.setClient(null);
            commande.setClientNom(request.getClientNom());
            commande.setClientPrenom(request.getClientPrenom());
            commande.setClientTelephone(request.getClientTelephone());
        }
    }

    private List<LigneCommande> buildLignes(List<LigneCommandeRequest> lignesReq, Commande commande) {
        List<LigneCommande> lignes = new ArrayList<>();
        if (lignesReq == null) return lignes;
        for (LigneCommandeRequest req : lignesReq) {
            Produit produit = produitRepository.findById(req.getProduitId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit non trouvé: " + req.getProduitId()));
            LigneCommande ligne = new LigneCommande();
            ligne.setCommande(commande);
            ligne.setProduit(produit);
            ligne.setQuantite(req.getQuantite());
            ligne.setPrixUnitaire(req.getPrixUnitaire() != null ? req.getPrixUnitaire() : produit.getPrixVente());
            ligne.setPrixAchat(produit.getPrixAchat());
            ligne.calculer();
            lignes.add(ligne);
        }
        return lignes;
    }
}
