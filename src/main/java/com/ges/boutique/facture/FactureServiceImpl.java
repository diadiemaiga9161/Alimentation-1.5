package com.ges.boutique.facture;

import com.ges.boutique.boutique.Boutique;
import com.ges.boutique.boutique.BoutiqueRepository;
import com.ges.boutique.client.Client;
import com.ges.boutique.client.ClientRepository;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import com.ges.boutique.vente.LigneVente;
import com.ges.boutique.vente.RemiseType;
import com.ges.boutique.vente.Vente;
import com.ges.boutique.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FactureServiceImpl implements FactureService {

    private final FactureRepository factureRepository;
    private final ProduitRepository produitRepository;
    private final ClientRepository clientRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final VenteRepository venteRepository;

    // ==================== CRÉATION ====================

    @Override
    @Transactional
    public Map<String, Object> creerFacture(FactureRequest request) {
        log.info("Création d'une nouvelle facture");

        Facture facture = new Facture();
        facture.setNotes(request.getNotes());
        facture.setChauffeur(request.getChauffeur());

        if (request.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé"));
            facture.setUtilisateur(utilisateur);
        }

        Boutique boutique = boutiqueRepository.findAll().stream().findFirst().orElse(null);
        facture.setBoutique(boutique);
        facture.setStatut("BROUILLON");

        gererClientFacture(facture, request);

        if (request.getLignes() == null || request.getLignes().isEmpty()) {
            throw new IllegalArgumentException("La facture doit contenir au moins un produit");
        }

        for (LigneFactureRequest ligneReq : request.getLignes()) {
            LigneFacture ligne = new LigneFacture();

            if (ligneReq.getProduitId() != null) {
                Produit produit = produitRepository.findById(ligneReq.getProduitId()).orElse(null);
                if (produit != null) {
                    ligne.setProduit(produit);
                    ligne.setPrixAchat(produit.getPrixAchat());
                    ligne.setDesignation(ligneReq.getDesignation() != null ? ligneReq.getDesignation() : produit.getNom());
                    ligne.setPrixUnitaire(ligneReq.getPrixUnitaire() != null ? ligneReq.getPrixUnitaire() : produit.getPrixVente());
                    ligne.setPrixOriginalProduit(produit.getPrixVente());
                } else {
                    ligne.setProduit(null);
                    ligne.setDesignation(ligneReq.getDesignation() != null ? ligneReq.getDesignation() : "Article");
                    ligne.setPrixUnitaire(ligneReq.getPrixUnitaire() != null ? ligneReq.getPrixUnitaire() : 0.0);
                    ligne.setPrixAchat(0.0);
                }
            } else {
                ligne.setProduit(null);
                ligne.setDesignation(ligneReq.getDesignation() != null ? ligneReq.getDesignation() : "Article");
                ligne.setPrixUnitaire(ligneReq.getPrixUnitaire() != null ? ligneReq.getPrixUnitaire() : 0.0);
                ligne.setPrixAchat(0.0);
            }

            ligne.setQuantite(ligneReq.getQuantite());
            ligne.setDescription(ligneReq.getDescription());

            if (ligneReq.getRemisePourcentage() != null && ligneReq.getRemisePourcentage() > 0) {
                ligne.appliquerRemisePourcentage(ligneReq.getRemisePourcentage());
            } else if (ligneReq.getRemiseMontant() != null && ligneReq.getRemiseMontant() > 0) {
                ligne.appliquerRemiseMontant(ligneReq.getRemiseMontant());
            }

            facture.ajouterLigne(ligne);
        }

        if (request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0) {
            if (RemiseType.POURCENTAGE.name().equals(request.getTypeRemiseGlobale())) {
                facture.appliquerRemiseGlobalePourcentage(request.getRemiseGlobale());
            } else if (RemiseType.MONTANT_FIXE.name().equals(request.getTypeRemiseGlobale())) {
                facture.appliquerRemiseGlobaleMontant(request.getRemiseGlobale());
            }
        }

        Facture savedFacture = factureRepository.save(facture);
        return wrapFactureWithBoutique(savedFacture);
    }

    @Override
    @Transactional
    public Map<String, Object> creerFactureDepuisVente(Long venteId, LocalDateTime dateFacture, Long utilisateurId) {
        log.info("Création facture depuis vente ID: {}", venteId);

        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RessourceIntrouvableException("Vente non trouvée: " + venteId));

        Facture facture = new Facture();
        facture.setDateCreation(dateFacture != null ? dateFacture : LocalDateTime.now());

        if (vente.getClient() != null) {
            facture.setClient(vente.getClient());
            facture.setClientNom(vente.getClient().getNom());
            facture.setClientPrenom(vente.getClient().getPrenom());
            facture.setClientTelephone(vente.getClient().getNumeroTelephone());
            facture.setClientAdresse(vente.getClient().getAdresse());
            facture.setClientDivers(false);
        } else {
            facture.setClientNom(vente.getClientNom());
            facture.setClientPrenom(vente.getClientPrenom());
            facture.setClientTelephone(vente.getClientTelephone());
            facture.setClientDivers(vente.getClientDivers() != null && vente.getClientDivers());
        }

        for (LigneVente lv : vente.getLignes()) {
            LigneFacture lf = new LigneFacture();
            lf.setProduit(lv.getProduit());
            lf.setQuantite(lv.getQuantite());
            lf.setPrixUnitaire(lv.getPrixUnitaire());
            lf.setPrixOriginalProduit(lv.getPrixOriginalProduit());
            lf.setPrixAchat(lv.getPrixAchat());
            lf.setRemisePourcentage(lv.getRemisePourcentage());
            lf.setRemiseMontant(lv.getRemiseMontant());
            lf.setDesignation(lv.getProduit() != null ? lv.getProduit().getNom() : "Produit");
            lf.calculerSousTotal();
            facture.ajouterLigne(lf);
        }

        if (vente.getRemiseGlobale() != null && vente.getRemiseGlobale() > 0 && vente.getTypeRemiseGlobale() != null) {
            facture.setRemiseGlobale(vente.getRemiseGlobale());
            facture.setTypeRemiseGlobale(vente.getTypeRemiseGlobale());
        }

        facture.setVente(vente);
        facture.setUtilisateur(utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé")));
        facture.setStatut("VALIDE");

        Boutique boutique = boutiqueRepository.findAll().stream().findFirst().orElse(null);
        facture.setBoutique(boutique);

        facture.calculerTotal();
        Facture saved = factureRepository.save(facture);
        return wrapFactureWithBoutique(saved);
    }

    // ==================== CONSULTATION ====================

    @Override
    public Map<String, Object> obtenirFacture(Long id) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée: " + id));
        return wrapFactureWithBoutique(facture);
    }

    @Override
    public Facture obtenirFactureEntite(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée: " + id));
    }

    @Override
    public List<Map<String, Object>> obtenirToutesFactures() {
        return factureRepository.findAll().stream()
                .map(this::wrapFactureWithBoutique)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> obtenirFacturesParStatut(String statut) {
        return factureRepository.findByStatut(statut).stream()
                .map(this::wrapFactureWithBoutique)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> obtenirFacturesParClient(Long clientId) {
        return factureRepository.findByClientId(clientId).stream()
                .map(this::wrapFactureWithBoutique)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> obtenirFacturesParClientNom(String clientNom) {
        return factureRepository.findByClientNomContainingIgnoreCase(clientNom).stream()
                .map(this::wrapFactureWithBoutique)
                .collect(Collectors.toList());
    }

    @Override
    public List<Facture> obtenirFacturesParPeriode(LocalDateTime debut, LocalDateTime fin) {
        return factureRepository.findByDateCreationBetween(debut, fin);
    }

    // Nouvelle méthode pour retourner des Map au lieu d'entités brutes
    @Override
    public List<Map<String, Object>> obtenirFacturesParPeriodeMap(LocalDateTime debut, LocalDateTime fin) {
        return factureRepository.findByDateCreationBetween(debut, fin).stream()
                .map(this::wrapFactureWithBoutique)
                .collect(Collectors.toList());
    }

    @Override
    public List<Facture> obtenirFacturesParVente(Long venteId) {
        return factureRepository.findByVenteId(venteId);
    }

    // Nouvelle méthode pour retourner des Map depuis la vente
    @Override
    public List<Map<String, Object>> obtenirFacturesParVenteMap(Long venteId) {
        return factureRepository.findByVenteId(venteId).stream()
                .map(this::wrapFactureWithBoutique)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getStatistiques() {
        Map<String, Object> stats = new HashMap<>();
        long total = factureRepository.count();
        long brouillons = factureRepository.findByStatut("BROUILLON").size();
        long valides = factureRepository.findByStatut("VALIDE").size();
        long payees = factureRepository.findByStatut("PAYEE").size();
        long annulees = factureRepository.findByStatut("ANNULEE").size();

        Double montantTotal = factureRepository.findAll().stream()
                .mapToDouble(Facture::getMontantTotal)
                .sum();

        stats.put("nombreTotal", total);
        stats.put("nombreBrouillons", brouillons);
        stats.put("nombreValides", valides);
        stats.put("nombrePayees", payees);
        stats.put("nombreAnnulees", annulees);
        stats.put("montantTotal", montantTotal != null ? montantTotal : 0.0);
        return stats;
    }

    // ==================== MODIFICATION ====================

    @Override
    @Transactional
    public Map<String, Object> modifierStatutFacture(Long id, String statut) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée: " + id));
        facture.setStatut(statut);
        Facture saved = factureRepository.save(facture);
        return wrapFactureWithBoutique(saved);
    }

    @Override
    @Transactional
    public Map<String, Object> modifierPrixLigne(Long factureId, Long ligneId, Double nouveauPrix) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée: " + factureId));

        LigneFacture ligne = facture.getLignes().stream()
                .filter(l -> l.getId().equals(ligneId))
                .findFirst()
                .orElseThrow(() -> new RessourceIntrouvableException("Ligne non trouvée: " + ligneId));

        ligne.modifierPrixUnitaire(nouveauPrix);
        Facture saved = factureRepository.save(facture);
        return wrapFactureWithBoutique(saved);
    }

    @Override
    @Transactional
    public Map<String, Object> modifierFacture(Long id, FactureRequest request) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée: " + id));

        if (!"BROUILLON".equals(facture.getStatut())) {
            throw new IllegalArgumentException("Seules les factures au statut BROUILLON peuvent être modifiées");
        }

        facture.setClientNom(request.getClientNom());
        facture.setClientPrenom(request.getClientPrenom());
        facture.setClientTelephone(request.getClientTelephone());
        facture.setClientAdresse(request.getClientAdresse());
        facture.setNotes(request.getNotes());
        facture.setChauffeur(request.getChauffeur());

        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Client non trouvé"));
            facture.setClient(client);
            facture.setClientDivers(false);
        } else {
            facture.setClient(null);
            facture.setClientDivers(true);
        }

        ligneFactureRepository.deleteByFactureId(id);
        facture.getLignes().clear();

        for (LigneFactureRequest ligneReq : request.getLignes()) {
            LigneFacture ligne = new LigneFacture();

            if (ligneReq.getProduitId() != null) {
                Produit produit = produitRepository.findById(ligneReq.getProduitId()).orElse(null);
                if (produit != null) {
                    ligne.setProduit(produit);
                    ligne.setPrixAchat(produit.getPrixAchat());
                    ligne.setDesignation(ligneReq.getDesignation() != null ? ligneReq.getDesignation() : produit.getNom());
                    ligne.setPrixUnitaire(ligneReq.getPrixUnitaire() != null ? ligneReq.getPrixUnitaire() : produit.getPrixVente());
                    ligne.setPrixOriginalProduit(produit.getPrixVente());
                } else {
                    ligne.setProduit(null);
                    ligne.setDesignation(ligneReq.getDesignation() != null ? ligneReq.getDesignation() : "Article");
                    ligne.setPrixUnitaire(ligneReq.getPrixUnitaire() != null ? ligneReq.getPrixUnitaire() : 0.0);
                    ligne.setPrixAchat(0.0);
                }
            } else {
                ligne.setProduit(null);
                ligne.setDesignation(ligneReq.getDesignation() != null ? ligneReq.getDesignation() : "Article");
                ligne.setPrixUnitaire(ligneReq.getPrixUnitaire() != null ? ligneReq.getPrixUnitaire() : 0.0);
                ligne.setPrixAchat(0.0);
            }

            ligne.setQuantite(ligneReq.getQuantite());
            ligne.setDescription(ligneReq.getDescription());

            if (ligneReq.getRemisePourcentage() != null && ligneReq.getRemisePourcentage() > 0) {
                ligne.appliquerRemisePourcentage(ligneReq.getRemisePourcentage());
            } else if (ligneReq.getRemiseMontant() != null && ligneReq.getRemiseMontant() > 0) {
                ligne.appliquerRemiseMontant(ligneReq.getRemiseMontant());
            }

            facture.ajouterLigne(ligne);
        }

        if (request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0) {
            if (RemiseType.POURCENTAGE.name().equals(request.getTypeRemiseGlobale())) {
                facture.appliquerRemiseGlobalePourcentage(request.getRemiseGlobale());
            } else if (RemiseType.MONTANT_FIXE.name().equals(request.getTypeRemiseGlobale())) {
                facture.appliquerRemiseGlobaleMontant(request.getRemiseGlobale());
            }
        } else {
            facture.setRemiseGlobale(0.0);
            facture.setTypeRemiseGlobale(null);
        }

        Facture saved = factureRepository.save(facture);
        return wrapFactureWithBoutique(saved);
    }

    // ==================== SUPPRESSION ====================

    @Override
    @Transactional
    public void supprimerFacture(Long id) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Facture non trouvée: " + id));
        if (!"BROUILLON".equals(facture.getStatut())) {
            throw new IllegalArgumentException("Seules les factures au statut BROUILLON peuvent être supprimées");
        }
        ligneFactureRepository.deleteByFactureId(id);
        factureRepository.delete(facture);
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private void gererClientFacture(Facture facture, FactureRequest request) {
        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Client non trouvé"));
            facture.setClient(client);
            facture.setClientNom(client.getNom());
            facture.setClientPrenom(client.getPrenom());
            facture.setClientTelephone(client.getNumeroTelephone());
            facture.setClientAdresse(client.getAdresse());
            facture.setClientDivers(false);
        } else if (request.getClientTelephone() != null && !request.getClientTelephone().isEmpty()) {
            Client client = clientRepository.findByNumeroTelephone(request.getClientTelephone()).orElse(null);
            if (client == null && Boolean.TRUE.equals(request.getCreerClient())) {
                client = new Client();
                client.setNom(request.getClientNom() != null ? request.getClientNom() : "Client");
                client.setPrenom(request.getClientPrenom() != null ? request.getClientPrenom() : "");
                client.setNumeroTelephone(request.getClientTelephone());
                client.setAdresse(request.getClientAdresse());
                client = clientRepository.save(client);
            }
            if (client != null) {
                facture.setClient(client);
                facture.setClientNom(client.getNom());
                facture.setClientPrenom(client.getPrenom());
                facture.setClientTelephone(client.getNumeroTelephone());
                facture.setClientAdresse(client.getAdresse());
                facture.setClientDivers(false);
            } else {
                facture.setClientNom(request.getClientNom());
                facture.setClientPrenom(request.getClientPrenom());
                facture.setClientTelephone(request.getClientTelephone());
                facture.setClientAdresse(request.getClientAdresse());
                facture.setClientDivers(true);
            }
        } else {
            facture.setClientNom(request.getClientNom() != null ? request.getClientNom() : "Client Divers");
            facture.setClientPrenom(request.getClientPrenom());
            facture.setClientTelephone(request.getClientTelephone());
            facture.setClientAdresse(request.getClientAdresse());
            facture.setClientDivers(true);
        }
    }

    private Map<String, Object> wrapFactureWithBoutique(Facture facture) {
        Map<String, Object> response = new HashMap<>();
        response.put("facture", facture);
        if (facture.getBoutique() == null) {
            Boutique boutique = boutiqueRepository.findAll().stream().findFirst().orElse(null);
            response.put("boutique", boutique);
        } else {
            response.put("boutique", facture.getBoutique());
        }
        return response;
    }
}