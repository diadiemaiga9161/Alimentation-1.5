package com.ges.boutique.vente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VenteService {

    // Méthodes existantes
    Vente creerVente(VenteRequest request);
    Vente creerVente(Vente vente);
    Vente obtenirVenteParId(Long id);
    List<Vente> obtenirToutesVentes();
    List<Vente> obtenirVentesParDateRange(LocalDate dateDebut, LocalDate dateFin);
    List<Vente> obtenirVentesDuJour();
    Map<String, Object> obtenirStatistiquesChiffreAffaire();
    Map<String, Object> obtenirStatistiquesJournalieres(LocalDate date);
    Map<String, Object> obtenirStatistiquesHebdomadaires();
    Map<String, Object> obtenirStatistiquesMensuelles();
    Long compterVentesParDateRange(LocalDateTime debut, LocalDateTime fin);
    List<Vente> obtenirVentesParVendeur(Long vendeurId);
    Double obtenirChiffreAffaireVendeur(Long vendeurId);

    // Méthodes pour les remises
    Vente appliquerRemiseGlobale(Long venteId, Double remise, RemiseType type);
    LigneVente appliquerRemiseLigne(Long ligneId, Double remise, RemiseType type);
    List<Vente> obtenirVentesAvecRemise();
    Double obtenirTotalRemisesParPeriode(LocalDate dateDebut, LocalDate dateFin);
    Vente annulerRemiseGlobale(Long venteId);
    LigneVente annulerRemiseLigne(Long ligneId);

    // Méthodes CRUD existantes
    Vente modifierVente(Long venteId, VenteRequest request);
    void supprimerVente(Long venteId);

    // Méthodes pour les crédits
    Vente creerVenteCredit(VenteCreditRequest request);
    Vente obtenirVenteCreditParId(Long id);
    List<Vente> obtenirTousCredits();
    List<Vente> obtenirCreditsNonRegles();
    List<Vente> obtenirCreditsEnRetard();
    List<Vente> obtenirCreditsParClient(String clientNom);
    List<Vente> obtenirCreditsReglesParPeriode(LocalDate dateDebut, LocalDate dateFin);
    Vente modifierVenteCredit(Long venteId, VenteCreditRequest request);
    void supprimerVenteCredit(Long venteId);

    // Méthode pour enregistrer un règlement de crédit
    Vente enregistrerReglementCredit(Long venteId, ReglementCreditRequest request);

    // Statistiques des crédits
    Map<String, Object> getStatistiquesCredits();

    // ==================== NOUVELLES MÉTHODES ====================

    Vente annulerVente(Long venteId, Long utilisateurId, String motifAnnulation);
}