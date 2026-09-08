package com.ges.boutique.compte;

import com.ges.boutique.feature.CleFonctionnalite;
import com.ges.boutique.feature.RequireFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comptes")
@RequiredArgsConstructor
@RequireFeature(CleFonctionnalite.COMPTES_BANCAIRES)
public class CompteController {

    private final CompteService compteService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> creerCompte(@RequestBody CompteRequest request) {
        Compte compte = compteService.creerCompte(request);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Compte bancaire créé avec succès");
        resp.put("compte", CompteDto.fromEntity(compte));
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> modifierCompte(@PathVariable Long id, @RequestBody CompteRequest request) {
        Compte compte = compteService.modifierCompte(id, request);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("compte", CompteDto.fromEntity(compte));
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<List<CompteDto>> getTousLesComptes() {
        return ResponseEntity.ok(compteService.getTousLesComptes().stream()
                .map(CompteDto::fromEntity).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompteDto> getCompteById(@PathVariable Long id) {
        return ResponseEntity.ok(CompteDto.fromEntity(compteService.getCompteById(id)));
    }

    @PostMapping("/operation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> enregistrerOperation(@RequestBody OperationCompteRequest request) {
        OperationCompte op = compteService.enregistrerOperation(request);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Opération enregistrée");
        resp.put("operation", toMap(op));
        resp.put("soldeActuel", op.getSoldeApres());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}/operations")
    public ResponseEntity<List<Map<String, Object>>> getHistoriqueOperations(@PathVariable Long id) {
        return ResponseEntity.ok(
                compteService.getHistoriqueOperations(id).stream()
                        .map(this::toMap)
                        .collect(Collectors.toList())
        );
    }

    private Map<String, Object> toMap(OperationCompte op) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", op.getId());
        m.put("type", op.getType().toString());
        m.put("montant", op.getMontant());
        m.put("soldeAvant", op.getSoldeAvant());
        m.put("soldeApres", op.getSoldeApres());
        m.put("motif", op.getMotif() != null ? op.getMotif() : "");
        m.put("reference", op.getReference() != null ? op.getReference() : "");
        m.put("dateOperation", op.getDateOperation() != null ? op.getDateOperation().toString() : "");
        return m;
    }
}
