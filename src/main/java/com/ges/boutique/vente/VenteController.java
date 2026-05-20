package com.ges.boutique.vente;

import com.ges.boutique.caisse.CaisseService;
import com.ges.boutique.utilisateur.UtilisateurMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ventes")
@RequiredArgsConstructor
@Tag(name = "Ventes", description = "Gestion des ventes")
public class VenteController {

    private final VenteService venteService;
    private final VenteMapper venteMapper;
    private final UtilisateurMapper utilisateurMapper;
    private final CaisseService caisseService;

    // ==================== CRÉATION ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Créer une vente (comptant)")
    public ResponseEntity<Map<String, Object>> creerVente(@RequestBody VenteRequest request) {
        Vente vente = venteService.creerVente(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vente créée avec succès");
        response.put("vente", venteMapper.toVenteMap(vente));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/credit")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Créer une vente à crédit")
    public ResponseEntity<Map<String, Object>> creerVenteCredit(@RequestBody VenteCreditRequest request) {
        Vente vente = venteService.creerVenteCredit(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Crédit créé avec succès");
        response.put("vente", venteMapper.toVenteMap(vente));
        return ResponseEntity.ok(response);
    }

    // ==================== LECTURE ====================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir une vente par ID")
    public ResponseEntity<Map<String, Object>> obtenirVenteParId(@PathVariable Long id) {
        Vente vente = venteService.obtenirVenteParId(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("vente", venteMapper.toVenteMap(vente));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/credit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir un crédit par ID")
    public ResponseEntity<Map<String, Object>> obtenirVenteCreditParId(@PathVariable Long id) {
        Vente vente = venteService.obtenirVenteCreditParId(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("vente", venteMapper.toVenteMap(vente));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir toutes les ventes")
    public ResponseEntity<List<Map<String, Object>>> obtenirToutesVentes() {
        List<Vente> ventes = venteService.obtenirToutesVentes();
        return ResponseEntity.ok(venteMapper.toVenteMapList(ventes));
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
        response.put("montantTotal", credits.stream().mapToDouble(Vente::getMontantRestant).sum());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/credits/en-retard")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les crédits en retard")
    public ResponseEntity<Map<String, Object>> obtenirCreditsEnRetard() {
        List<Vente> credits = venteService.obtenirCreditsEnRetard();
        Map<String, Object> response = new HashMap<>();
        response.put("credits", venteMapper.toVenteMapList(credits));
        response.put("nombreCredits", credits.size());
        response.put("montantTotal", credits.stream().mapToDouble(Vente::getMontantRestant).sum());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/credits/client")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les crédits par client")
    public ResponseEntity<List<Map<String, Object>>> obtenirCreditsParClient(@RequestParam String clientNom) {
        List<Vente> credits = venteService.obtenirCreditsParClient(clientNom);
        return ResponseEntity.ok(venteMapper.toVenteMapList(credits));
    }

    @GetMapping("/vendeur/{vendeurId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les ventes par vendeur")
    public ResponseEntity<List<Map<String, Object>>> obtenirVentesParVendeur(@PathVariable Long vendeurId) {
        List<Vente> ventes = venteService.obtenirVentesParVendeur(vendeurId);
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

    @GetMapping("/aujourdhui")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les ventes du jour")
    public ResponseEntity<Map<String, Object>> obtenirVentesDuJour() {
        List<Vente> ventes = venteService.obtenirVentesDuJour();
        Map<String, Object> response = new HashMap<>();
        response.put("ventes", venteMapper.toVenteMapList(ventes));
        response.put("nombreVentes", ventes.size());
        response.put("montantTotal", ventes.stream().mapToDouble(Vente::getMontantTotal).sum());
        response.put("beneficeTotal", ventes.stream().mapToDouble(Vente::getBeneficeTotal).sum());
        return ResponseEntity.ok(response);
    }

    // ==================== MODIFICATION ====================

    @PutMapping("/{venteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Modifier une vente")
    public ResponseEntity<Map<String, Object>> modifierVente(@PathVariable Long venteId, @RequestBody VenteRequest request) {
        Vente vente = venteService.modifierVente(venteId, request);
        return ResponseEntity.ok(venteMapper.toVenteMap(vente));
    }

    @PutMapping("/credits/{venteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Modifier un crédit")
    public ResponseEntity<Map<String, Object>> modifierVenteCredit(@PathVariable Long venteId, @RequestBody VenteCreditRequest request) {
        Vente vente = venteService.modifierVenteCredit(venteId, request);
        return ResponseEntity.ok(venteMapper.toVenteMap(vente));
    }

    // ==================== REMISES ====================

    @PostMapping("/{venteId}/remise-globale")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Appliquer une remise globale")
    public ResponseEntity<Map<String, Object>> appliquerRemiseGlobale(
            @PathVariable Long venteId,
            @RequestParam Double remise,
            @RequestParam RemiseType type) {
        Vente vente = venteService.appliquerRemiseGlobale(venteId, remise, type);
        return ResponseEntity.ok(venteMapper.toVenteMap(vente));
    }

    @DeleteMapping("/{venteId}/remise-globale")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Annuler la remise globale")
    public ResponseEntity<Map<String, Object>> annulerRemiseGlobale(@PathVariable Long venteId) {
        Vente vente = venteService.annulerRemiseGlobale(venteId);
        return ResponseEntity.ok(venteMapper.toVenteMap(vente));
    }

    @PostMapping("/lignes/{ligneId}/remise")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Appliquer une remise sur une ligne")
    public ResponseEntity<Map<String, Object>> appliquerRemiseLigne(
            @PathVariable Long ligneId,
            @RequestParam Double remise,
            @RequestParam RemiseType type) {
        LigneVente ligne = venteService.appliquerRemiseLigne(ligneId, remise, type);
        return ResponseEntity.ok(venteMapper.toLigneMap(ligne));
    }

    @DeleteMapping("/lignes/{ligneId}/remise")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Annuler la remise sur une ligne")
    public ResponseEntity<Map<String, Object>> annulerRemiseLigne(@PathVariable Long ligneId) {
        LigneVente ligne = venteService.annulerRemiseLigne(ligneId);
        return ResponseEntity.ok(venteMapper.toLigneMap(ligne));
    }

    // ==================== RÈGLEMENTS CRÉDIT ====================

    @PostMapping("/credits/{venteId}/reglement")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Enregistrer un règlement de crédit")
    public ResponseEntity<Map<String, Object>> enregistrerReglementCredit(
            @PathVariable Long venteId,
            @RequestBody ReglementCreditRequest request) {
        request.setVenteId(venteId);
        Vente vente = venteService.enregistrerReglementCredit(venteId, request);
        return ResponseEntity.ok(venteMapper.toVenteMap(vente));
    }

    // ==================== SUPPRESSION ET ANNULATION ====================

    @DeleteMapping("/{venteId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer définitivement une vente")
    public ResponseEntity<Map<String, Object>> supprimerVente(@PathVariable Long venteId) {
        venteService.supprimerVente(venteId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vente supprimée avec succès");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/credits/{venteId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer définitivement un crédit")
    public ResponseEntity<Map<String, Object>> supprimerVenteCredit(@PathVariable Long venteId) {
        venteService.supprimerVenteCredit(venteId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Crédit supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * Annuler une vente (comptant)
     * @param venteId ID de la vente
     * @param utilisateurId ID de l'utilisateur qui annule
     * @param motif Motif de l'annulation
     * @param repercuterCaisse Si true, répercute l'annulation sur la caisse (retire le montant)
     */
    @PostMapping("/{venteId}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Annuler une vente avec répercussion optionnelle sur la caisse")
    public ResponseEntity<Map<String, Object>> annulerVente(
            @PathVariable Long venteId,
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) String motif,
            @RequestParam(required = false, defaultValue = "true") boolean repercuterCaisse) {

        log.info("=== ANNULATION VENTE ===");
        log.info("Vente ID: {}, Utilisateur: {}, Motif: {}, Répercuter en caisse: {}",
                venteId, utilisateurId, motif, repercuterCaisse);

        Vente vente = venteService.obtenirVenteParId(venteId);

        // Si la vente est déjà annulée, ne rien faire
        if (Boolean.TRUE.equals(vente.getAnnulee())) {
            log.warn("La vente {} est déjà annulée", vente.getNumeroVente());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cette vente est déjà annulée");
            response.put("vente", venteMapper.toVenteMap(vente));
            response.put("dejaAnnulee", true);
            return ResponseEntity.ok(response);
        }

        // Répercuter l'annulation en caisse si demandé
        if (repercuterCaisse) {
            try {
                log.info("Répercussion de l'annulation en caisse pour la vente {}", vente.getNumeroVente());
                caisseService.annulerVenteAvecRepercussion(vente, utilisateurId, motif);
                log.info("✅ Annulation répercutée en caisse avec succès");
            } catch (Exception e) {
                log.error("Erreur lors de la répercussion en caisse: {}", e.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Erreur lors de la répercussion en caisse: " + e.getMessage());
                return ResponseEntity.badRequest().body(errorResponse);
            }
        }

        // Annuler la vente dans le service vente
        Vente venteAnnulee = venteService.annulerVente(venteId, utilisateurId, motif);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vente annulée avec succès");
        response.put("vente", venteMapper.toVenteMap(venteAnnulee));
        response.put("repercuterCaisse", repercuterCaisse);
        return ResponseEntity.ok(response);
    }

    /**
     * Annuler un crédit
     * @param venteId ID du crédit
     * @param utilisateurId ID de l'utilisateur qui annule
     * @param motif Motif de l'annulation
     * @param repercuterCaisse Si true, répercute l'annulation sur la caisse
     */
    @PostMapping("/credits/{venteId}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Annuler un crédit avec répercussion optionnelle sur la caisse")
    public ResponseEntity<Map<String, Object>> annulerVenteCredit(
            @PathVariable Long venteId,
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) String motif,
            @RequestParam(required = false, defaultValue = "true") boolean repercuterCaisse) {

        log.info("=== ANNULATION CRÉDIT ===");
        log.info("Crédit ID: {}, Utilisateur: {}, Motif: {}, Répercuter en caisse: {}",
                venteId, utilisateurId, motif, repercuterCaisse);

        Vente vente = venteService.obtenirVenteParId(venteId);

        // Vérifier que c'est bien un crédit
        if (!Boolean.TRUE.equals(vente.getEstCredit())) {
            log.error("La vente {} n'est pas un crédit", vente.getNumeroVente());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Cette vente n'est pas un crédit");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Si le crédit est déjà annulé, ne rien faire
        if (Boolean.TRUE.equals(vente.getAnnulee())) {
            log.warn("Le crédit {} est déjà annulé", vente.getNumeroVente());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ce crédit est déjà annulé");
            response.put("vente", venteMapper.toVenteMap(vente));
            response.put("dejaAnnulee", true);
            return ResponseEntity.ok(response);
        }

        // Vérifier si le crédit a déjà été payé partiellement
        if (vente.getMontantVerse() != null && vente.getMontantVerse() > 0 && repercuterCaisse) {
            log.warn("Le crédit a déjà été partiellement payé ({} FCFA). L'annulation ne modifie pas le solde de la caisse.",
                    vente.getMontantVerse());
        }

        // Répercuter l'annulation en caisse si demandé
        if (repercuterCaisse) {
            try {
                log.info("Répercussion de l'annulation en caisse pour le crédit {}", vente.getNumeroVente());
                caisseService.annulerVenteCreditAvecRepercussion(vente, utilisateurId, motif);
                log.info("✅ Annulation du crédit répercutée en caisse avec succès");
            } catch (Exception e) {
                log.error("Erreur lors de la répercussion en caisse: {}", e.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Erreur lors de la répercussion en caisse: " + e.getMessage());
                return ResponseEntity.badRequest().body(errorResponse);
            }
        }

        // Annuler le crédit dans le service vente
        Vente venteAnnulee = venteService.annulerVenteCredit(venteId, utilisateurId, motif);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Crédit annulé avec succès");
        response.put("vente", venteMapper.toVenteMap(venteAnnulee));
        response.put("repercuterCaisse", repercuterCaisse);
        return ResponseEntity.ok(response);
    }

    // ==================== STATISTIQUES ====================

    @GetMapping("/statistiques/chiffre-affaire")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Statistiques chiffre d'affaires")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesChiffreAffaire() {
        return ResponseEntity.ok(venteService.obtenirStatistiquesChiffreAffaire());
    }

    @GetMapping("/statistiques/journalieres")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Statistiques journalières")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesJournalieres(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(venteService.obtenirStatistiquesJournalieres(date));
    }

    @GetMapping("/statistiques/hebdomadaires")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Statistiques hebdomadaires")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesHebdomadaires() {
        return ResponseEntity.ok(venteService.obtenirStatistiquesHebdomadaires());
    }

    @GetMapping("/statistiques/mensuelles")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Statistiques mensuelles")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesMensuelles() {
        return ResponseEntity.ok(venteService.obtenirStatistiquesMensuelles());
    }

    @GetMapping("/statistiques/credits")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Statistiques des crédits")
    public ResponseEntity<Map<String, Object>> getStatistiquesCredits() {
        return ResponseEntity.ok(venteService.getStatistiquesCredits());
    }

    @GetMapping("/statistiques/nombre-periodes")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Compter les ventes sur une période")
    public ResponseEntity<Map<String, Object>> compterVentesParDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        Long count = venteService.compterVentesParDateRange(debut, fin);
        Map<String, Object> response = new HashMap<>();
        response.put("debut", debut);
        response.put("fin", fin);
        response.put("nombreVentes", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistiques/vendeur/{vendeurId}/ca")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Chiffre d'affaires par vendeur")
    public ResponseEntity<Map<String, Object>> obtenirChiffreAffaireVendeur(@PathVariable Long vendeurId) {
        Double ca = venteService.obtenirChiffreAffaireVendeur(vendeurId);
        Map<String, Object> response = new HashMap<>();
        response.put("vendeurId", vendeurId);
        response.put("chiffreAffaire", ca);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistiques/top-clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Top clients")
    public ResponseEntity<List<Map<String, Object>>> obtenirTopClients() {
        return ResponseEntity.ok(venteService.obtenirTopClients());
    }

    @GetMapping("/statistiques/top-produits/quantite")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Top produits par quantité")
    public ResponseEntity<List<Map<String, Object>>> obtenirTopProduitsParQuantite() {
        return ResponseEntity.ok(venteService.obtenirTopProduitsParQuantite());
    }

    @GetMapping("/statistiques/top-produits/ca")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Top produits par chiffre d'affaires")
    public ResponseEntity<List<Map<String, Object>>> obtenirTopProduitsParChiffreAffaire() {
        return ResponseEntity.ok(venteService.obtenirTopProduitsParChiffreAffaire());
    }

    @GetMapping("/statistiques/top-produits")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Top produits (les deux classements)")
    public ResponseEntity<Map<String, Object>> obtenirTopProduits() {
        Map<String, Object> result = new HashMap<>();
        result.put("parQuantite", venteService.obtenirTopProduitsParQuantite());
        result.put("parChiffreAffaire", venteService.obtenirTopProduitsParChiffreAffaire());
        return ResponseEntity.ok(result);
    }
}