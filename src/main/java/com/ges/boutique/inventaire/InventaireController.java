package com.ges.boutique.inventaire;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventaire")
@RequiredArgsConstructor
@Tag(name = "Inventaire", description = "Gestion de l'inventaire")
public class InventaireController {

    private final InventaireService inventaireService;

    @PostMapping("/entree")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enregistrer une entrée de stock")
    public ResponseEntity<Void> entreeStock(@RequestBody Map<String, Object> request) {
        Long produitId = Long.valueOf(request.get("produitId").toString());
        Integer quantite = Integer.valueOf(request.get("quantite").toString());
        Long utilisateurId = request.get("utilisateurId") != null ?
                Long.valueOf(request.get("utilisateurId").toString()) : null;
        String motif = (String) request.get("motif");

        LocalDateTime dateMouvement = null;
        if (request.get("dateMouvement") != null) {
            try {
                String dateStr = request.get("dateMouvement").toString();
                // Supprimer le 'Z' ou offset timezone si présent
                dateStr = dateStr.replaceAll("Z$", "").replaceAll("\\+[0-9]{2}:[0-9]{2}$", "");
                dateMouvement = LocalDateTime.parse(dateStr);
            } catch (Exception e) {
                dateMouvement = LocalDateTime.now();
            }
        }

        inventaireService.entreeStock(produitId, quantite, utilisateurId, motif, dateMouvement);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sortie")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enregistrer une sortie de stock")
    public ResponseEntity<Void> sortieStock(@RequestBody Map<String, Object> request) {
        Long produitId = Long.valueOf(request.get("produitId").toString());
        Integer quantite = Integer.valueOf(request.get("quantite").toString());
        Long utilisateurId = request.get("utilisateurId") != null ?
                Long.valueOf(request.get("utilisateurId").toString()) : null;
        String motif = (String) request.get("motif");

        inventaireService.sortieStock(produitId, quantite, utilisateurId, motif);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ajustement")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ajuster le stock")
    public ResponseEntity<Void> ajusterStock(@RequestBody Map<String, Object> request) {
        Long produitId = Long.valueOf(request.get("produitId").toString());
        Integer nouvelleQuantite = Integer.valueOf(request.get("nouvelleQuantite").toString());
        Long utilisateurId = request.get("utilisateurId") != null ?
                Long.valueOf(request.get("utilisateurId").toString()) : null;
        String motif = (String) request.get("motif");

        inventaireService.ajusterStock(produitId, nouvelleQuantite, utilisateurId, motif);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/historique/produit/{produitId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir l'historique d'un produit")
    public ResponseEntity<List<MouvementStock>> obtenirHistoriqueProduit(@PathVariable Long produitId) {
        return ResponseEntity.ok(inventaireService.obtenirHistoriqueProduit(produitId));
    }

    @GetMapping("/historique")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les mouvements par date")
    public ResponseEntity<List<MouvementStock>> obtenirMouvementsParDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(inventaireService.obtenirMouvementsParDate(debut, fin));
    }

    @GetMapping("/stock-faible")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les produits en stock faible")
    public ResponseEntity<List<com.ges.boutique.produit.Produit>> obtenirProduitsStockFaible() {
        return ResponseEntity.ok(inventaireService.obtenirProduitsStockFaible());
    }

    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques de l'inventaire")
    public ResponseEntity<Map<String, Object>> obtenirStatistiquesInventaire() {
        return ResponseEntity.ok(inventaireService.obtenirStatistiquesInventaire());
    }

    @GetMapping("/mouvements")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir tous les mouvements de stock (du plus récent au plus ancien)")
    public ResponseEntity<List<MouvementStock>> obtenirTousMouvements() {
        return ResponseEntity.ok(inventaireService.obtenirTousMouvements());
    }
}