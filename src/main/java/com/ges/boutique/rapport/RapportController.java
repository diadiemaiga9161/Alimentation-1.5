package com.ges.boutique.rapport;

import com.ges.boutique.vente.VenteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
@Tag(name = "Rapports", description = "Génération de rapports")
public class RapportController {

    private final RapportService rapportService;
    private final VenteRepository venteRepository;

    @GetMapping("/journalier/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer un rapport journalier")
    public ResponseEntity<Map<String, Object>> genererRapportJournalier(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(rapportService.genererRapportJournalier(date));
    }

    @GetMapping("/hebdomadaire")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer un rapport hebdomadaire")
    public ResponseEntity<Map<String, Object>> genererRapportHebdomadaire() {
        return ResponseEntity.ok(rapportService.genererRapportHebdomadaire());
    }

    @GetMapping("/mensuel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer un rapport mensuel")
    public ResponseEntity<Map<String, Object>> genererRapportMensuel() {
        return ResponseEntity.ok(rapportService.genererRapportMensuel());
    }

    @GetMapping("/statistiques-generales")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques générales")
    public ResponseEntity<Map<String, Object>> genererStatistiquesGenerales() {
        return ResponseEntity.ok(rapportService.genererStatistiquesGenerales());
    }

    // =====================================================================
    // Endpoints graphiques Angular
    // =====================================================================

    /**
     * CA journalier sur les 30 derniers jours.
     * Retourne une liste de { "date": "YYYY-MM-DD", "ca": 12345.0 }
     */
    @GetMapping("/ca-30-jours")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "CA par jour sur les 30 derniers jours (graphique)")
    public ResponseEntity<List<Map<String, Object>>> getCA30Jours() {
        LocalDateTime debut = LocalDateTime.now().minusDays(30);
        LocalDateTime fin   = LocalDateTime.now();
        List<Object[]> rows = venteRepository.findCAParJour(debut, fin);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", row[0] != null ? row[0].toString() : null);
            item.put("ca",   row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Top 10 produits les plus vendus (quantité) sur les 30 derniers jours.
     * Retourne une liste de { "produitNom": "...", "quantiteVendue": 42 }
     */
    @GetMapping("/top-produits")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Top 10 produits vendus sur 30 jours (graphique)")
    public ResponseEntity<List<Map<String, Object>>> getTopProduits() {
        LocalDateTime debut = LocalDateTime.now().minusDays(30);
        List<Object[]> rows = venteRepository.findTopProduits(debut);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("produitNom",      row[0] != null ? row[0].toString() : "Inconnu");
            item.put("quantiteVendue",  row[1] != null ? ((Number) row[1]).longValue() : 0L);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Ventes agrégées par heure pour la journée courante.
     * Retourne une liste de { "heure": 9, "nbVentes": 5, "ca": 15000.0 }
     */
    @GetMapping("/ventes-par-heure")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Ventes par heure aujourd'hui (graphique)")
    public ResponseEntity<List<Map<String, Object>>> getVentesParHeure() {
        List<Object[]> rows = venteRepository.findVentesParHeure();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("heure",    row[0] != null ? ((Number) row[0]).intValue()  : 0);
            item.put("nbVentes", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            item.put("ca",       row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }
}