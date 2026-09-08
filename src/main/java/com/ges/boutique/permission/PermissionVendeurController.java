package com.ges.boutique.permission;

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
 * Permissions de consultation accordables au vendeur (ex: Inventaire) — décision de
 * l'ADMIN normal de la boutique, PAS du super admin (voir com.ges.boutique.feature pour
 * ça). Séparé du système de fonctionnalités avancées à dessein : ici on élargit l'accès
 * du vendeur, on ne restreint jamais celui de l'admin.
 */
@RestController
@RequestMapping("/api/boutique/permissions-vendeur")
@RequiredArgsConstructor
@Tag(name = "Permissions vendeur", description = "Consultation accordée au vendeur par l'admin de la boutique")
public class PermissionVendeurController {

    private final PermissionVendeurService permissionVendeurService;

    // Lecture ouverte à tout le personnel (ADMIN/VENDEUR) — sert au vendeur lui-même à
    // savoir ce qu'il peut consulter (affichage des menus), même principe que
    // GET /api/boutique/fonctionnalites-avancees.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Lister toutes les permissions vendeur et leur état")
    public ResponseEntity<Map<String, Object>> obtenirToutes() {
        List<Map<String, Object>> permissions = permissionVendeurService.obtenirToutes();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("permissions", permissions);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{cle}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Accorder/retirer une permission au vendeur — n'importe quel admin de la boutique")
    public ResponseEntity<Map<String, Object>> definirEtat(
            @PathVariable CleVendeur cle,
            @RequestBody Map<String, Boolean> body) {
        Boolean actif = body.get("actif");
        Map<String, Object> response = new HashMap<>();
        if (actif == null) {
            response.put("success", false);
            response.put("error", "Le champ \"actif\" est requis");
            return ResponseEntity.badRequest().body(response);
        }
        permissionVendeurService.definirEtat(cle, actif);
        response.put("success", true);
        response.put("permissions", permissionVendeurService.obtenirToutes());
        return ResponseEntity.ok(response);
    }
}
