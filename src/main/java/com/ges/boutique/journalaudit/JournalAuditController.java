package com.ges.boutique.journalaudit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Consultation du journal d'audit — réservé aux admins (voir aussi SecurityConfig,
 * règle sur /api/journal-audit/**). Aucun endpoint d'écriture n'est exposé ici :
 * les entrées sont créées uniquement en interne par JournalAuditService.enregistrer(...).
 */
@RestController
@RequestMapping("/api/journal-audit")
@RequiredArgsConstructor
@Tag(name = "Journal d'audit", description = "Consultation des actions sensibles tracées (admin uniquement)")
public class JournalAuditController {

    private final JournalAuditService journalAuditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister le journal d'audit (paginé, filtres optionnels dateDebut/dateFin/utilisateurId)")
    public ResponseEntity<Map<String, Object>> lister(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long utilisateurId) {

        Page<JournalAuditDto> resultat = journalAuditService.rechercher(dateDebut, dateFin, utilisateurId, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("journaux", resultat.getContent());
        response.put("page", resultat.getNumber());
        response.put("size", resultat.getSize());
        response.put("totalElements", resultat.getTotalElements());
        response.put("totalPages", resultat.getTotalPages());
        return ResponseEntity.ok(response);
    }
}
