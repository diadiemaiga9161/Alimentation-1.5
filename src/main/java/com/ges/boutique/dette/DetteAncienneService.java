package com.ges.boutique.dette;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DetteAncienneService {

    // Gestion des dettes
    DetteAncienneDto creerDette(DetteAncienneRequest request);
    DetteAncienneDto modifierDette(Long id, DetteAncienneRequest request);
    DetteAncienneDto obtenirDetteParId(Long id);
    List<DetteAncienneDto> obtenirDettesParClient(Long clientId);
    List<DetteAncienneDto> obtenirToutesDettesNonReglees();
    List<DetteAncienneDto> obtenirToutesDettesReglees();
    List<DetteAncienneDto> obtenirToutesDettes();
    List<DetteAncienneDto> rechercherDettes(String search);
    void supprimerDette(Long id);

    // Gestion des règlements
    ReglementDetteAncienne enregistrerReglement(ReglementDetteRequest request);
    List<ReglementDetteAncienne> getHistoriqueReglements(Long detteId);
    List<ReglementDetteAncienne> getReglementsParPeriode(LocalDate dateDebut, LocalDate dateFin);

    // Statistiques
    Map<String, Object> getStatistiquesGlobales();
    Map<String, Object> getStatistiquesParClient(Long clientId);
    Map<String, Object> getStatistiquesReglementsParPeriode(LocalDate dateDebut, LocalDate dateFin);
}