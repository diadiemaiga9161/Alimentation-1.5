package com.ges.boutique.objectifvendeur;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/objectifs-vendeur")
@RequiredArgsConstructor
public class ObjectifVendeurController {

    private final ObjectifVendeurService service;

    @PostMapping
    public ResponseEntity<ObjectifVendeurDto> creer(@RequestBody ObjectifVendeurRequest request) {
        return ResponseEntity.ok(service.creer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObjectifVendeurDto> modifier(@PathVariable Long id,
                                                        @RequestBody ObjectifVendeurRequest request) {
        return ResponseEntity.ok(service.modifier(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjectifVendeurDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ObjectifVendeurDto>> getTous() {
        return ResponseEntity.ok(service.getTous());
    }

    @GetMapping("/semaine")
    public ResponseEntity<List<ObjectifVendeurDto>> getParSemaineAnnee(
            @RequestParam int semaine, @RequestParam int annee) {
        return ResponseEntity.ok(service.getParSemaineAnnee(semaine, annee));
    }

    @GetMapping("/vendeur/{vendeurId}")
    public ResponseEntity<List<ObjectifVendeurDto>> getParVendeur(@PathVariable Long vendeurId) {
        return ResponseEntity.ok(service.getParVendeur(vendeurId));
    }

    @GetMapping("/annee")
    public ResponseEntity<List<ObjectifVendeurDto>> getParAnnee(@RequestParam int annee) {
        return ResponseEntity.ok(service.getParAnnee(annee));
    }

    @PatchMapping("/{id}/valider")
    public ResponseEntity<ObjectifVendeurDto> valider(@PathVariable Long id) {
        return ResponseEntity.ok(service.valider(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
