package com.ges.boutique.bonus;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bonus-fournisseurs")
@RequiredArgsConstructor
public class BonusFournisseurController {

    private final BonusFournisseurService service;

    @GetMapping
    public ResponseEntity<List<BonusFournisseurDto>> listerTous() {
        return ResponseEntity.ok(service.listerTous());
    }

    @GetMapping("/fournisseur/{fournisseurId}")
    public ResponseEntity<List<BonusFournisseurDto>> listerParFournisseur(@PathVariable Long fournisseurId) {
        return ResponseEntity.ok(service.listerParFournisseur(fournisseurId));
    }

    @GetMapping("/periode")
    public ResponseEntity<List<BonusFournisseurDto>> listerParPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(service.listerParPeriode(debut, fin));
    }

    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> statistiques(
            @RequestParam(defaultValue = "0") int mois,
            @RequestParam(defaultValue = "0") int annee) {
        int m = mois > 0 ? mois : LocalDate.now().getMonthValue();
        int a = annee > 0 ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(service.statistiques(m, a));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BonusFournisseurDto> creer(@RequestBody BonusFournisseurRequest request) {
        return ResponseEntity.ok(service.creer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BonusFournisseurDto> modifier(@PathVariable Long id,
                                                         @RequestBody BonusFournisseurRequest request) {
        return ResponseEntity.ok(service.modifier(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
