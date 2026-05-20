package com.ges.boutique.boutique;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/boutique")
@RequiredArgsConstructor
@Tag(name = "Boutique", description = "Gestion des informations de la boutique")
public class BoutiqueController {

    private final BoutiqueService boutiqueService;

    // ==================== Endpoints sans ID (existants) ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les informations de la boutique")
    public ResponseEntity<Map<String, Object>> obtenirBoutique() {
        Boutique boutique = boutiqueService.obtenirBoutique();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("boutique", boutique);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier les informations de la boutique")
    public ResponseEntity<Map<String, Object>> modifierBoutique(@RequestBody Boutique boutique) {
        Boutique boutiqueModifiee = boutiqueService.modifierBoutique(boutique);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Informations de la boutique modifiées avec succès");
        response.put("boutique", boutiqueModifiee);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une nouvelle boutique")
    public ResponseEntity<Map<String, Object>> creerBoutique(@RequestBody Boutique boutique) {
        Boutique nouvelleBoutique = boutiqueService.creerBoutique(boutique);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Boutique créée avec succès");
        response.put("boutique", nouvelleBoutique);
        return ResponseEntity.ok(response);
    }

    // ==================== Nouveaux endpoints avec ID (pour compatibilité frontend) ====================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les informations de la boutique par ID (ignoré car une seule boutique)")
    public ResponseEntity<Map<String, Object>> obtenirBoutiqueParId(@PathVariable Long id) {
        // L'ID est ignoré, on retourne toujours la boutique unique
        return obtenirBoutique();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier la boutique par ID (ignoré)")
    public ResponseEntity<Map<String, Object>> modifierBoutiqueParId(@PathVariable Long id,
                                                                     @RequestBody Boutique boutique) {
        // L'ID du chemin est ignoré, on utilise la boutique reçue
        return modifierBoutique(boutique);
    }
}