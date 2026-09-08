package com.ges.boutique.produit;

import com.ges.boutique.feature.CleFonctionnalite;
import com.ges.boutique.feature.RequireFeature;
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
 * Unités de vente alternatives par produit (Cartouche, Carton...) — voir UniteVente
 * pour le contexte complet. Désactivable par le super admin comme toute nouvelle
 * fonctionnalité de ce projet (voir CleFonctionnalite.VENTE_GROS_DETAIL) ; si
 * désactivée, le vendeur ne voit que l'unité de base du produit, comme aujourd'hui.
 */
@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
@RequireFeature(CleFonctionnalite.VENTE_GROS_DETAIL)
@Tag(name = "Unités de vente", description = "Vente en gros et au détail — unités alternatives par produit")
public class UniteVenteController {

    private final UniteVenteService uniteVenteService;

    @GetMapping("/{produitId}/unites-vente")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Lister les unités de vente d'un produit")
    public ResponseEntity<Map<String, Object>> lister(@PathVariable Long produitId) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("unites", uniteVenteService.lister(produitId));
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{produitId}/unites-vente")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une unité de vente pour un produit")
    public ResponseEntity<Map<String, Object>> creer(@PathVariable Long produitId, @RequestBody UniteVenteRequest request) {
        Map<String, Object> resp = new HashMap<>();
        try {
            UniteVente unite = uniteVenteService.creer(produitId, request);
            resp.put("success", true);
            resp.put("unite", unite);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PutMapping("/unites-vente/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une unité de vente")
    public ResponseEntity<Map<String, Object>> modifier(@PathVariable Long id, @RequestBody UniteVenteRequest request) {
        Map<String, Object> resp = new HashMap<>();
        try {
            UniteVente unite = uniteVenteService.modifier(id, request);
            resp.put("success", true);
            resp.put("unite", unite);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @DeleteMapping("/unites-vente/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une unité de vente")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable Long id) {
        uniteVenteService.supprimer(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        return ResponseEntity.ok(resp);
    }
}
