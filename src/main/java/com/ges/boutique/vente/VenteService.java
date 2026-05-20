package com.ges.boutique.vente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VenteService {

    Vente creerVente(VenteRequest request);
    Vente creerVenteCredit(VenteCreditRequest request);
    Vente obtenirVenteParId(Long id);
    Vente obtenirVenteCreditParId(Long id);
    List<Vente> obtenirToutesVentes();
    List<Vente> obtenirTousCredits();
    List<Vente> obtenirCreditsNonRegles();
    List<Vente> obtenirCreditsEnRetard();
    List<Vente> obtenirCreditsParClient(String clientNom);
    List<Vente> obtenirVentesParVendeur(Long vendeurId);
    List<Vente> obtenirVentesParDateRange(LocalDate dateDebut, LocalDate dateFin);
    List<Vente> obtenirVentesDuJour();

    Vente modifierVente(Long venteId, VenteRequest request);
    Vente modifierVenteCredit(Long venteId, VenteCreditRequest request);
    void supprimerVente(Long venteId);
    void supprimerVenteCredit(Long venteId);
    Vente annulerVente(Long venteId, Long utilisateurId, String motif);
    Vente annulerVenteCredit(Long venteId, Long utilisateurId, String motif);

    Vente appliquerRemiseGlobale(Long venteId, Double remise, RemiseType type);
    LigneVente appliquerRemiseLigne(Long ligneId, Double remise, RemiseType type);
    Vente annulerRemiseGlobale(Long venteId);
    LigneVente annulerRemiseLigne(Long ligneId);

    Vente enregistrerReglementCredit(Long venteId, ReglementCreditRequest request);

    Map<String, Object> obtenirStatistiquesChiffreAffaire();
    Map<String, Object> obtenirStatistiquesJournalieres(LocalDate date);
    Map<String, Object> obtenirStatistiquesHebdomadaires();
    Map<String, Object> obtenirStatistiquesMensuelles();
    Map<String, Object> getStatistiquesCredits();

    Long compterVentesParDateRange(LocalDateTime debut, LocalDateTime fin);
    Double obtenirChiffreAffaireVendeur(Long vendeurId);

    List<Map<String, Object>> obtenirTopClients();
    List<Map<String, Object>> obtenirTopProduitsParQuantite();
    List<Map<String, Object>> obtenirTopProduitsParChiffreAffaire();
}