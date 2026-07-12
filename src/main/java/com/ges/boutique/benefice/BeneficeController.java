package com.ges.boutique.benefice;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/benefices")
@RequiredArgsConstructor
public class BeneficeController {

    private final BeneficeService beneficeService;

    @GetMapping("/journalier")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> journalier(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(beneficeService.beneficeJournalier(date != null ? date : LocalDate.now()));
    }

    @GetMapping("/hebdomadaire")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> hebdomadaire() {
        return ResponseEntity.ok(beneficeService.beneficeHebdomadaire());
    }

    @GetMapping("/mensuel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> mensuel(
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) Integer annee) {
        int m = mois != null ? mois : LocalDate.now().getMonthValue();
        int a = annee != null ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(beneficeService.beneficeMensuel(m, a));
    }

    @GetMapping("/annuel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> annuel(
            @RequestParam(required = false) Integer annee) {
        int a = annee != null ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(beneficeService.beneficeAnnuel(a));
    }
}
