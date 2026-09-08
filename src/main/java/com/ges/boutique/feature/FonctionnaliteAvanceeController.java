package com.ges.boutique.feature;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints dédiés aux fonctionnalités avancées (Dépôt garde, Dettes anciennes,
 * Comptes bancaires...) — séparés de PUT /api/boutique/fonctionnalites (Transferts/
 * Vitrine, déjà en place) pour ne pas toucher à ce mécanisme existant.
 */
@RestController
@RequestMapping("/api/boutique/fonctionnalites-avancees")
@RequiredArgsConstructor
@Tag(name = "Fonctionnalités avancées", description = "Activation/désactivation fine réservée au super admin")
public class FonctionnaliteAvanceeController {

    private final FeatureToggleService featureToggleService;

    // Lecture ouverte à tout le personnel (ADMIN/VENDEUR) — sert à masquer les menus
    // des fonctionnalités désactivées pour tout le monde, pas seulement le super admin
    // (même principe que GET /api/boutique, lisible par tous, pour featureVitrineActif).
    // Seule la modification (PUT ci-dessous) reste réservée au super admin.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Lister toutes les fonctionnalités avancées et leur état")
    public ResponseEntity<Map<String, Object>> obtenirToutes() {
        List<Map<String, Object>> fonctionnalites = featureToggleService.obtenirToutes();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("fonctionnalites", fonctionnalites);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{cle}")
    @PreAuthorize("hasRole('ADMIN') and authentication.principal.superAdmin")
    @Operation(summary = "Activer/désactiver une fonctionnalité avancée — réservé au super admin")
    public ResponseEntity<Map<String, Object>> definirEtat(
            @PathVariable CleFonctionnalite cle,
            @RequestBody Map<String, Boolean> body) {
        Boolean actif = body.get("actif");
        Map<String, Object> response = new HashMap<>();
        if (actif == null) {
            response.put("success", false);
            response.put("error", "Le champ \"actif\" est requis");
            return ResponseEntity.badRequest().body(response);
        }
        featureToggleService.definirEtat(cle, actif);
        response.put("success", true);
        response.put("fonctionnalites", featureToggleService.obtenirToutes());
        return ResponseEntity.ok(response);
    }
}
