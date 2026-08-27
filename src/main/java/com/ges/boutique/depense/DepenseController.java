package com.ges.boutique.depense;

import com.ges.boutique.utilisateur.Utilisateur;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/depenses")
@RequiredArgsConstructor
@Tag(name = "Dépenses", description = "Gestion des dépenses de la boutique")
public class DepenseController {

    private final DepenseService depenseService;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Utilisateur u) return u.getId();
        return null;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Lister toutes les dépenses")
    public ResponseEntity<Map<String, Object>> obtenirToutes() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("depenses", depenseService.obtenirToutes());
        response.put("total", depenseService.getTotalDepenses());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir une dépense par ID")
    public ResponseEntity<Map<String, Object>> obtenirParId(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("depense", depenseService.obtenirParId(id));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Dépenses par période")
    public ResponseEntity<Map<String, Object>> obtenirParPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("depenses", depenseService.obtenirParPeriode(debut, fin));
        response.put("total", depenseService.getTotalDepensesParPeriode(debut, fin));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/par-type")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Totaux des dépenses groupés par type")
    public ResponseEntity<Map<String, Object>> obtenirParType(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        if (debut != null && fin != null) {
            response.put("totaux", depenseService.getTotauxParTypePeriode(debut, fin));
        } else {
            response.put("totaux", depenseService.getTotauxParType());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une dépense — déduit automatiquement de la caisse")
    public ResponseEntity<Map<String, Object>> creer(@RequestBody DepenseRequest request) {
        Depense depense = depenseService.creerDepense(request, getUserId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dépense enregistrée et déduite de la caisse");
        response.put("depense", depense);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une dépense — ajuste automatiquement la caisse")
    public ResponseEntity<Map<String, Object>> modifier(
            @PathVariable Long id,
            @RequestBody DepenseRequest request) {
        Depense depense = depenseService.modifierDepense(id, request, getUserId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dépense modifiée et caisse ajustée");
        response.put("depense", depense);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Valider une dépense — la verrouille dans les comptes (elle ne pourra plus être supprimée, seulement annulée avec trace)")
    public ResponseEntity<Map<String, Object>> valider(@PathVariable Long id) {
        Depense depense = depenseService.validerDepense(id, getUserId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dépense validée");
        response.put("depense", depense);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une dépense — remboursement automatique en caisse. Si la dépense est validée, elle est annulée avec trace au lieu d'être supprimée.")
    public ResponseEntity<Map<String, Object>> supprimer(
            @PathVariable Long id,
            @RequestParam(required = false) String motif) {
        depenseService.supprimerDepense(id, getUserId(), motif);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dépense supprimée et montant remis en caisse");
        return ResponseEntity.ok(response);
    }
}
