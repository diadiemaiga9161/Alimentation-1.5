package com.ges.boutique.transfert;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transferts")
@RequiredArgsConstructor
public class TransfertController {

    private final TransfertService service;

    // ==================== PARTENAIRES ====================

    @GetMapping("/partenaires")
    public ResponseEntity<List<BoutiquePartenaire>> getPartenaires() {
        return ResponseEntity.ok(service.getPartenaires());
    }

    @PostMapping("/partenaires")
    public ResponseEntity<BoutiquePartenaire> ajouterPartenaire(@RequestBody BoutiquePartenaire p) {
        return ResponseEntity.ok(service.ajouterPartenaire(p));
    }

    @PutMapping("/partenaires/{id}")
    public ResponseEntity<BoutiquePartenaire> modifierPartenaire(@PathVariable Long id, @RequestBody BoutiquePartenaire p) {
        return ResponseEntity.ok(service.modifierPartenaire(id, p));
    }

    @DeleteMapping("/partenaires/{id}")
    public ResponseEntity<Void> supprimerPartenaire(@PathVariable Long id) {
        service.supprimerPartenaire(id);
        return ResponseEntity.ok().build();
    }

    // ==================== TRANSFERTS ====================

    @GetMapping
    public ResponseEntity<List<TransfertStock>> getTout() {
        return ResponseEntity.ok(service.getTout());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransfertStock> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<TransfertStock> creer(@RequestBody TransfertRequest req, Authentication auth) {
        return ResponseEntity.ok(service.creer(req, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransfertStock> modifier(@PathVariable Long id, @RequestBody TransfertRequest req, Authentication auth) {
        return ResponseEntity.ok(service.modifier(id, req, auth.getName()));
    }

    @PutMapping("/{id}/confirmer")
    public ResponseEntity<TransfertStock> confirmer(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.confirmer(id, auth.getName()));
    }

    @PutMapping("/{id}/annuler")
    public ResponseEntity<TransfertStock> annuler(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, String> body,
                                                   Authentication auth) {
        String motif = body != null ? body.get("motif") : null;
        return ResponseEntity.ok(service.annuler(id, motif, auth.getName()));
    }

    // Endpoint appelé par une autre boutique
    @PostMapping("/recevoir")
    public ResponseEntity<TransfertStock> recevoir(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey) {
        return ResponseEntity.ok(service.recevoir(payload, serviceKey));
    }

    @GetMapping("/recus")
    public ResponseEntity<List<TransfertStock>> getRecus() {
        return ResponseEntity.ok(service.getRecus());
    }

    @GetMapping("/envoyes")
    public ResponseEntity<List<TransfertStock>> getEnvoyes() {
        return ResponseEntity.ok(service.getEnvoyes());
    }

    @PostMapping("/{id}/accepter")
    public ResponseEntity<TransfertStock> accepter(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.accepter(id, auth.getName()));
    }

    @PostMapping("/{id}/rejeter")
    public ResponseEntity<TransfertStock> rejeter(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String motif = body != null ? body.get("motif") : null;
        return ResponseEntity.ok(service.rejeter(id, motif, auth.getName()));
    }
}
