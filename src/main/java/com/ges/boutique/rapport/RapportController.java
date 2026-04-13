package com.ges.boutique.rapport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
@Tag(name = "Rapports", description = "Génération de rapports")
public class RapportController {

    private final RapportService rapportService;

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
}