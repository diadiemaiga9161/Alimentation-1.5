package com.ges.boutique.fournisseur;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/avances-fournisseurs")
@RequiredArgsConstructor
public class AvanceFournisseurController {

    private final AvanceFournisseurService avanceService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> enregistrerAvance(@RequestBody AvanceFournisseurRequest request) {
        AvanceFournisseur avance = avanceService.enregistrerAvance(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Avance de " + avance.getMontant() + " F enregistrée pour " + avance.getFournisseur().getNom(),
                "avance", AvanceFournisseurDto.fromEntity(avance)
        ));
    }

    @GetMapping("/solde/{fournisseurId}")
    public ResponseEntity<Map<String, Object>> getSolde(@PathVariable Long fournisseurId) {
        Double solde = avanceService.getSoldeDisponible(fournisseurId);
        return ResponseEntity.ok(Map.of(
                "fournisseurId", fournisseurId,
                "soldeDisponible", solde
        ));
    }

    @GetMapping("/historique/{fournisseurId}")
    public ResponseEntity<Map<String, Object>> getHistorique(@PathVariable Long fournisseurId) {
        List<AvanceFournisseur> avances = avanceService.getHistoriqueParFournisseur(fournisseurId);
        Double solde = avanceService.getSoldeDisponible(fournisseurId);
        return ResponseEntity.ok(Map.of(
                "fournisseurId", fournisseurId,
                "soldeDisponible", solde,
                "historique", avances.stream().map(AvanceFournisseurDto::fromEntity).collect(Collectors.toList()),
                "totalDepose", avances.stream().mapToDouble(AvanceFournisseur::getMontant).sum(),
                "totalUtilise", avances.stream().mapToDouble(AvanceFournisseur::getMontantUtilise).sum()
        ));
    }

    @GetMapping
    public ResponseEntity<List<AvanceFournisseurDto>> getToutesLesAvances() {
        return ResponseEntity.ok(
                avanceService.getToutesLesAvances().stream()
                        .map(AvanceFournisseurDto::fromEntity)
                        .collect(Collectors.toList())
        );
    }
}
