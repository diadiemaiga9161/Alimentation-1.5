package com.ges.boutique.facture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface FactureService {

    // Création
    Map<String, Object> creerFacture(FactureRequest request);
    Map<String, Object> creerFactureDepuisVente(Long venteId, LocalDateTime dateFacture, Long utilisateurId);

    // Consultation
    Map<String, Object> obtenirFacture(Long id);
    List<Map<String, Object>> obtenirToutesFactures();
    List<Map<String, Object>> obtenirFacturesParStatut(String statut);
    List<Map<String, Object>> obtenirFacturesParClient(Long clientId);
    List<Map<String, Object>> obtenirFacturesParClientNom(String clientNom);
    List<Facture> obtenirFacturesParPeriode(LocalDateTime debut, LocalDateTime fin);
    List<Map<String, Object>> obtenirFacturesParPeriodeMap(LocalDateTime debut, LocalDateTime fin);
    List<Facture> obtenirFacturesParVente(Long venteId);
    List<Map<String, Object>> obtenirFacturesParVenteMap(Long venteId);
    Map<String, Object> getStatistiques();

    // Modification
    Map<String, Object> modifierStatutFacture(Long id, String statut);
    Map<String, Object> modifierPrixLigne(Long factureId, Long ligneId, Double nouveauPrix);
    Map<String, Object> modifierFacture(Long id, FactureRequest request);

    // Suppression
    void supprimerFacture(Long id);
}