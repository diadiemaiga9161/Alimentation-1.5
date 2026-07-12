package com.ges.boutique.promo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Gestion des promotions et envoi WhatsApp")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Lister toutes les promotions")
    public ResponseEntity<Map<String, Object>> obtenirToutes() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("promotions", promotionService.obtenirToutes());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/actives")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Lister les promotions actives non expirées")
    public ResponseEntity<Map<String, Object>> obtenirActives() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("promotions", promotionService.obtenirActives());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir une promotion par ID")
    public ResponseEntity<Map<String, Object>> obtenirParId(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("promotion", promotionService.obtenirParId(id));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une promotion")
    public ResponseEntity<Map<String, Object>> creer(@RequestBody PromotionRequest request) {
        Promotion promo = promotionService.creer(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Promotion créée avec succès");
        response.put("promotion", promo);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une promotion")
    public ResponseEntity<Map<String, Object>> modifier(
            @PathVariable Long id,
            @RequestBody PromotionRequest request) {
        Promotion promo = promotionService.modifier(id, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Promotion modifiée avec succès");
        response.put("promotion", promo);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une promotion")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable Long id) {
        promotionService.supprimer(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Promotion supprimée avec succès");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/whatsapp")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Préparer les liens WhatsApp pour tous les clients")
    public ResponseEntity<Map<String, Object>> preparerWhatsApp(@PathVariable Long id) {
        Map<String, Object> response = promotionService.preparerMessagesWhatsApp(id);
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/produit/{produitId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les promotions actives pour un produit (globales + liées au produit)")
    public ResponseEntity<Map<String, Object>> promosPourProduit(@PathVariable Long produitId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("promotions", promotionService.obtenirPromosPourProduit(produitId));
        return ResponseEntity.ok(response);
    }
}
