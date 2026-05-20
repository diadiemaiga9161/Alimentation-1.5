package com.ges.boutique.facture;

import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/caisse/factures")
@RequiredArgsConstructor
public class FactureController {

    private final FactureService factureService;

    // ==================== CRÉATION ====================
    @PostMapping
    public ResponseEntity<Map<String, Object>> creerFacture(@RequestBody FactureRequest request) {
        return ResponseEntity.ok(factureService.creerFacture(request));
    }

    @PostMapping("/depuis-vente/{venteId}")
    public ResponseEntity<Map<String, Object>> creerFactureDepuisVente(
            @PathVariable Long venteId,
            @RequestParam Long utilisateurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFacture) {
        return ResponseEntity.ok(factureService.creerFactureDepuisVente(venteId, dateFacture, utilisateurId));
    }

    // ==================== CONSULTATION ====================
    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenirToutesFactures() {
        List<Map<String, Object>> factures = factureService.obtenirToutesFactures();
        Map<String, Object> response = new HashMap<>();
        response.put("factures", factures);
        response.put("nombreFactures", factures.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenirFacture(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.obtenirFacture(id));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<Map<String, Object>> obtenirFacturesParStatut(@PathVariable String statut) {
        List<Map<String, Object>> factures = factureService.obtenirFacturesParStatut(statut);
        Map<String, Object> response = new HashMap<>();
        response.put("factures", factures);
        response.put("nombreFactures", factures.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client")
    public ResponseEntity<Map<String, Object>> obtenirFacturesParClient(@RequestParam String clientNom) {
        List<Map<String, Object>> factures = factureService.obtenirFacturesParClientNom(clientNom);
        Map<String, Object> response = new HashMap<>();
        response.put("factures", factures);
        response.put("nombreFactures", factures.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/periode")
    public ResponseEntity<Map<String, Object>> obtenirFacturesParPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String dateFin) {
        LocalDateTime debut = LocalDateTime.parse(dateDebut + "T00:00:00");
        LocalDateTime fin = LocalDateTime.parse(dateFin + "T23:59:59");
        List<Map<String, Object>> factures = factureService.obtenirFacturesParPeriodeMap(debut, fin);
        Map<String, Object> response = new HashMap<>();
        response.put("factures", factures);
        response.put("nombreFactures", factures.size());
        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT CORRIGÉ : retourne une Map<String, Object> contenant la liste
     * des factures associées à une vente (sous la clé "factures") et le nombre.
     */
    @GetMapping("/vente/{venteId}")
    public ResponseEntity<Map<String, Object>> obtenirFacturesParVente(@PathVariable Long venteId) {
        List<Map<String, Object>> factures = factureService.obtenirFacturesParVenteMap(venteId);
        Map<String, Object> response = new HashMap<>();
        response.put("factures", factures);
        response.put("nombreFactures", factures.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> getStatistiques() {
        Map<String, Object> stats = factureService.getStatistiques();
        Map<String, Object> response = new HashMap<>();
        response.put("statistiques", stats);
        return ResponseEntity.ok(response);
    }

    // ==================== MODIFICATION ====================
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> modifierFacture(@PathVariable Long id, @RequestBody FactureRequest request) {
        return ResponseEntity.ok(factureService.modifierFacture(id, request));
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<Map<String, Object>> modifierStatutFacture(@PathVariable Long id, @RequestParam String statut) {
        return ResponseEntity.ok(factureService.modifierStatutFacture(id, statut));
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<Map<String, Object>> validerFacture(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.modifierStatutFacture(id, "VALIDE"));
    }

    @PutMapping("/{id}/annuler")
    public ResponseEntity<Map<String, Object>> annulerFacture(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.modifierStatutFacture(id, "ANNULEE"));
    }

    @PutMapping("/{factureId}/lignes/{ligneId}/prix")
    public ResponseEntity<Map<String, Object>> modifierPrixLigne(
            @PathVariable Long factureId,
            @PathVariable Long ligneId,
            @RequestParam Double nouveauPrix) {
        return ResponseEntity.ok(factureService.modifierPrixLigne(factureId, ligneId, nouveauPrix));
    }

    // ==================== SUPPRESSION ====================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerFacture(@PathVariable Long id) {
        factureService.supprimerFacture(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== GESTION DES ERREURS ====================
    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RessourceIntrouvableException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("Erreur interne", e);
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Erreur interne : " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}