package com.ges.boutique.vente;

import com.ges.boutique.utilisateur.UtilisateurMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ventes")
@RequiredArgsConstructor
@Tag(name = "Ventes", description = "Gestion des ventes")
public class VenteController {

    private final VenteService venteService;
    private final VenteMapper venteMapper;
    private final UtilisateurMapper utilisateurMapper;

    // ==================== ENDPOINTS EXISTANTS ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Créer une nouvelle vente (comptant ou crédit)")
    public ResponseEntity<Map<String, Object>> creerVente(@RequestBody VenteRequest request) {
        Vente vente;
        if (request.isEstCredit()) {
            VenteCreditRequest creditRequest = new VenteCreditRequest();
            creditRequest.setVendeurId(request.getVendeurId());
            creditRequest.setLignes(request.getLignes());
            creditRequest.setModePaiement(request.getModePaiement());
            creditRequest.setReferencePaiement(request.getReferencePaiement());
            creditRequest.setRemiseGlobale(request.getRemiseGlobale());
            creditRequest.setTypeRemiseGlobale(request.getTypeRemiseGlobale());
            creditRequest.setClientNom(request.getClientNom());
            creditRequest.setClientTelephone(request.getClientTelephone());
            creditRequest.setDateEcheance(request.getDateEcheance());
            creditRequest.setMontantVerse(request.getMontantVerse() != null ? request.getMontantVerse() : 0.0);
            vente = venteService.creerVenteCredit(creditRequest);
        } else {
            vente = venteService.creerVente(request);
        }
        return ResponseEntity.ok(venteMapper.toVenteMap(vente));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir une vente par ID")
    public ResponseEntity<Map<String, Object>> obtenirVenteParId(@PathVariable Long id) {
        Vente vente = venteService.obtenirVenteParId(id);
        return ResponseEntity.ok(venteMapper.toVenteMap(vente));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir toutes les ventes")
    public ResponseEntity<List<Map<String, Object>>> obtenirToutesVentes() {
        List<Vente> ventes = venteService.obtenirToutesVentes();
        return ResponseEntity.ok(venteMapper.toVenteMapList(ventes));
    }

    @GetMapping("/dto")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir toutes les ventes en format DTO")
    public ResponseEntity<List<VenteDto>> obtenirToutesVentesDto() {
        List<Vente> ventes = venteService.obtenirToutesVentes();
        List<VenteDto> dtos = ventes.stream()
                .map(venteMapper::toVenteDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/aujourdhui")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les ventes du jour")
    public ResponseEntity<Map<String, Object>> obtenirVentesDuJour() {
        List<Vente> ventes = venteService.obtenirVentesDuJour();

        Map<String, Object> response = new HashMap<>();
        response.put("ventes", venteMapper.toVenteMapList(ventes));
        response.put("totalVentes", ventes.size());
        response.put("montantTotal", ventes.stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum());
        response.put("montantTotalComptant", ventes.stream()
                .filter(v -> !Boolean.TRUE.equals(v.getEstCredit()))
                .mapToDouble(Vente::getMontantTotal)
                .sum());
        response.put("montantTotalCredit", ventes.stream()
                .filter(v -> Boolean.TRUE.equals(v.getEstCredit()))
                .mapToDouble(Vente::getMontantTotal)
                .sum());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/comptant")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir toutes les ventes comptant")
    public ResponseEntity<List<Map<String, Object>>> obtenirVentesComptant() {
        List<Vente> ventes = venteService.obtenirToutesVentes().stream()
                .filter(v -> !Boolean.TRUE.equals(v.getEstCredit()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(venteMapper.toVenteMapList(ventes));
    }

    @GetMapping("/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les ventes par période")
    public ResponseEntity<List<Map<String, Object>>> obtenirVentesParDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        List<Vente> ventes = venteService.obtenirVentesParDateRange(dateDebut, dateFin);
        return ResponseEntity.ok(venteMapper.toVenteMapList(ventes));
    }

    // ==================== ENDPOINTS STATISTIQUES ====================

    @GetMapping("/statistiques/chiffre-affaire")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques de chiffre d'affaires")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesChiffreAffaire() {
        return ResponseEntity.ok(venteService.obtenirStatistiquesChiffreAffaire());
    }

    @GetMapping("/statistiques/journalieres/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques journalières")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesJournalieres(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(venteService.obtenirStatistiquesJournalieres(date));
    }

    @GetMapping("/statistiques/hebdomadaires")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques hebdomadaires")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesHebdomadaires() {
        return ResponseEntity.ok(venteService.obtenirStatistiquesHebdomadaires());
    }

    @GetMapping("/statistiques/mensuelles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques mensuelles")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesMensuelles() {
        return ResponseEntity.ok(venteService.obtenirStatistiquesMensuelles());
    }

    // ==================== ENDPOINTS REMISES ====================

    @PostMapping("/{venteId}/remise-globale")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Appliquer une remise globale sur une vente")
    public ResponseEntity<Map<String, Object>> appliquerRemiseGlobale(
            @PathVariable Long venteId,
            @RequestParam Double remise,
            @RequestParam RemiseType type) {
        Vente vente = venteService.appliquerRemiseGlobale(venteId, remise, type);
        return ResponseEntity.ok(venteMapper.toVenteMap(vente));
    }

    @PostMapping("/lignes/{ligneId}/remise")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Appliquer une remise sur une ligne de vente")
    public ResponseEntity<Map<String, Object>> appliquerRemiseLigne(
            @PathVariable Long ligneId,
            @RequestParam Double remise,
            @RequestParam RemiseType type) {
        LigneVente ligne = venteService.appliquerRemiseLigne(ligneId, remise, type);
        return ResponseEntity.ok(venteMapper.toLigneMap(ligne));
    }

    @GetMapping("/avec-remise")
    @PreAuthorize("hasRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les ventes avec remise")
    public ResponseEntity<List<Map<String, Object>>> obtenirVentesAvecRemise() {
        List<Vente> ventes = venteService.obtenirVentesAvecRemise();
        return ResponseEntity.ok(venteMapper.toVenteMapList(ventes));
    }

    @GetMapping("/remises/total")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir le total des remises sur une période")
    public ResponseEntity<Double> obtenirTotalRemisesParPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        Double total = venteService.obtenirTotalRemisesParPeriode(dateDebut, dateFin);
        return ResponseEntity.ok(total);
    }

    @DeleteMapping("/{venteId}/remise-globale")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Annuler la remise globale d'une vente")
    public ResponseEntity<Map<String, Object>> annulerRemiseGlobale(@PathVariable Long venteId) {
        Vente vente = venteService.annulerRemiseGlobale(venteId);
        return ResponseEntity.ok(venteMapper.toVenteMap(vente));
    }

    @DeleteMapping("/lignes/{ligneId}/remise")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Annuler la remise d'une ligne de vente")
    public ResponseEntity<Map<String, Object>> annulerRemiseLigne(@PathVariable Long ligneId) {
        LigneVente ligne = venteService.annulerRemiseLigne(ligneId);
        return ResponseEntity.ok(venteMapper.toLigneMap(ligne));
    }

    // ==================== ENDPOINTS PRATIQUES ====================

    @GetMapping("/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un résumé des ventes")
    public ResponseEntity<List<Map<String, Object>>> obtenirResumeVentes() {
        List<Vente> ventes = venteService.obtenirToutesVentes();
        List<Map<String, Object>> resume = ventes.stream()
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", v.getId());
                    map.put("numeroVente", v.getNumeroVente());
                    map.put("dateVente", v.getDateVente());
                    map.put("vendeurId", v.getVendeurId());
                    map.put("vendeurNom", v.getVendeur() != null ? v.getVendeur().getNomComplet() : "Inconnu");
                    map.put("montantTotal", v.getMontantTotal());
                    map.put("modePaiement", v.getModePaiement().toString());
                    map.put("nombreProduits", v.getLignes() != null ? v.getLignes().size() : 0);
                    map.put("estCredit", v.getEstCredit());
                    map.put("clientNom", v.getClientNom());
                    map.put("creditRegle", v.getCreditRegle());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(resume);
    }

    @GetMapping("/vendeur/{vendeurId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les ventes par vendeur")
    public ResponseEntity<List<Map<String, Object>>> obtenirVentesParVendeur(@PathVariable Long vendeurId) {
        List<Vente> ventes = venteService.obtenirVentesParVendeur(vendeurId);
        return ResponseEntity.ok(venteMapper.toVenteMapList(ventes));
    }

    @GetMapping("/produit/{produitId}")
    @PreAuthorize("hasRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les ventes par produit")
    public ResponseEntity<List<Map<String, Object>>> obtenirVentesParProduit(@PathVariable Long produitId) {
        List<Vente> toutesVentes = venteService.obtenirToutesVentes();
        List<Map<String, Object>> ventesProduit = toutesVentes.stream()
                .filter(v -> v.getLignes() != null &&
                        v.getLignes().stream().anyMatch(l -> l.getProduitId().equals(produitId)))
                .map(venteMapper::toVenteMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ventesProduit);
    }

    @GetMapping("/{id}/vendeur")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les informations du vendeur d'une vente")
    public ResponseEntity<VendeurDto> obtenirVendeurDeVente(@PathVariable Long id) {
        Vente vente = venteService.obtenirVenteParId(id);
        if (vente.getVendeur() == null) {
            return ResponseEntity.notFound().build();
        }
        VendeurDto vendeurDto = utilisateurMapper.toVendeurDto(vente.getVendeur());
        return ResponseEntity.ok(vendeurDto);
    }

    // ==================== ENDPOINTS CRUD ====================

    @PutMapping("/{venteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Modifier une vente existante")
    public ResponseEntity<Map<String, Object>> modifierVente(
            @PathVariable Long venteId,
            @RequestBody VenteRequest request) {
        Vente venteModifiee;
        if (request.isEstCredit()) {
            VenteCreditRequest creditRequest = new VenteCreditRequest();
            creditRequest.setVendeurId(request.getVendeurId());
            creditRequest.setLignes(request.getLignes());
            creditRequest.setModePaiement(request.getModePaiement());
            creditRequest.setReferencePaiement(request.getReferencePaiement());
            creditRequest.setRemiseGlobale(request.getRemiseGlobale());
            creditRequest.setTypeRemiseGlobale(request.getTypeRemiseGlobale());
            creditRequest.setClientNom(request.getClientNom());
            creditRequest.setClientTelephone(request.getClientTelephone());
            creditRequest.setDateEcheance(request.getDateEcheance());
            creditRequest.setMontantVerse(request.getMontantVerse());
            venteModifiee = venteService.modifierVenteCredit(venteId, creditRequest);
        } else {
            venteModifiee = venteService.modifierVente(venteId, request);
        }
        return ResponseEntity.ok(venteMapper.toVenteMap(venteModifiee));
    }

    @DeleteMapping("/{venteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Supprimer une vente")
    public ResponseEntity<Map<String, Object>> supprimerVente(@PathVariable Long venteId) {
        Vente vente = venteService.obtenirVenteParId(venteId);

        if (Boolean.TRUE.equals(vente.getEstCredit())) {
            venteService.supprimerVenteCredit(venteId);
        } else {
            venteService.supprimerVente(venteId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vente supprimée avec succès");
        response.put("venteId", venteId);

        return ResponseEntity.ok(response);
    }

    // ==================== ENDPOINTS SPÉCIFIQUES AUX CRÉDITS ====================

    @PostMapping("/credit")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Créer une nouvelle vente à crédit")
    public ResponseEntity<Map<String, Object>> creerVenteCredit(@RequestBody VenteCreditRequest request) {
        Vente vente = venteService.creerVenteCredit(request);
        return ResponseEntity.ok(venteMapper.toVenteMap(vente));
    }

    @GetMapping("/credits")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir tous les crédits")
    public ResponseEntity<List<Map<String, Object>>> obtenirTousCredits() {
        List<Vente> credits = venteService.obtenirTousCredits();
        return ResponseEntity.ok(venteMapper.toVenteMapList(credits));
    }

    @GetMapping("/credits/non-regles")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les crédits non réglés")
    public ResponseEntity<Map<String, Object>> obtenirCreditsNonRegles() {
        List<Vente> credits = venteService.obtenirCreditsNonRegles();

        Map<String, Object> response = new HashMap<>();
        response.put("credits", venteMapper.toVenteMapList(credits));
        response.put("nombreCredits", credits.size());
        response.put("montantTotal", credits.stream()
                .mapToDouble(Vente::getMontantRestant)
                .sum());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/credits/en-retard")
    @PreAuthorize("hasRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les crédits en retard")
    public ResponseEntity<Map<String, Object>> obtenirCreditsEnRetard() {
        List<Vente> credits = venteService.obtenirCreditsEnRetard();

        Map<String, Object> response = new HashMap<>();
        response.put("credits", venteMapper.toVenteMapList(credits));
        response.put("nombreCredits", credits.size());
        response.put("montantTotal", credits.stream()
                .mapToDouble(Vente::getMontantRestant)
                .sum());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/credits/client/{clientNom}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les crédits par client")
    public ResponseEntity<List<Map<String, Object>>> obtenirCreditsParClient(@PathVariable String clientNom) {
        List<Vente> credits = venteService.obtenirCreditsParClient(clientNom);
        return ResponseEntity.ok(venteMapper.toVenteMapList(credits));
    }

    @GetMapping("/credits/regles")
    @PreAuthorize("hasRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les crédits réglés par période")
    public ResponseEntity<List<Map<String, Object>>> obtenirCreditsReglesParPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        List<Vente> credits = venteService.obtenirCreditsReglesParPeriode(dateDebut, dateFin);
        return ResponseEntity.ok(venteMapper.toVenteMapList(credits));
    }

    @GetMapping("/credits/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un crédit par ID")
    public ResponseEntity<Map<String, Object>> obtenirCreditParId(@PathVariable Long id) {
        Vente credit = venteService.obtenirVenteCreditParId(id);
        return ResponseEntity.ok(venteMapper.toVenteMap(credit));
    }

    @PutMapping("/credits/{venteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Modifier un crédit")
    public ResponseEntity<Map<String, Object>> modifierVenteCredit(
            @PathVariable Long venteId,
            @RequestBody VenteCreditRequest request) {
        Vente creditModifie = venteService.modifierVenteCredit(venteId, request);
        return ResponseEntity.ok(venteMapper.toVenteMap(creditModifie));
    }

    @DeleteMapping("/credits/{venteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Supprimer un crédit")
    public ResponseEntity<Map<String, Object>> supprimerVenteCredit(@PathVariable Long venteId) {
        venteService.supprimerVenteCredit(venteId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Crédit supprimé avec succès");
        response.put("venteId", venteId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/credits/{venteId}/reglement")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Enregistrer un règlement de crédit")
    public ResponseEntity<Map<String, Object>> enregistrerReglementCredit(
            @PathVariable Long venteId,
            @RequestBody ReglementCreditRequest request) {

        request.setVenteId(venteId);
        Vente vente = venteService.enregistrerReglementCredit(venteId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Règlement enregistré avec succès");
        response.put("vente", venteMapper.toVenteMap(vente));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/credits/statistiques")
    @PreAuthorize("hasRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les statistiques des crédits")
    public ResponseEntity<Map<String, Object>> getStatistiquesCredits() {
        return ResponseEntity.ok(venteService.getStatistiquesCredits());
    }

    @GetMapping("/credits/reglements/aujourdhui")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les règlements du jour")
    public ResponseEntity<Map<String, Object>> getReglementsDuJour() {
        List<Vente> reglements = venteService.obtenirCreditsReglesParPeriode(
                LocalDate.now(), LocalDate.now());

        Map<String, Object> response = new HashMap<>();
        response.put("reglements", venteMapper.toVenteMapList(reglements));
        response.put("nombreReglements", reglements.size());
        response.put("montantTotal", reglements.stream()
                .mapToDouble(Vente::getMontantVerse)
                .sum());

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{venteId}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Annuler une vente")
    public ResponseEntity<Map<String, Object>> annulerVente(
            @PathVariable Long venteId,
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) String motif) {

        Vente venteAnnulee = venteService.annulerVente(venteId, utilisateurId, motif);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vente annulée avec succès");
        response.put("vente", venteMapper.toVenteMap(venteAnnulee));

        return ResponseEntity.ok(response);
    }
}