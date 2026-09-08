package com.ges.boutique.objectif;

import com.ges.boutique.feature.CleFonctionnalite;
import com.ges.boutique.feature.RequireFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/objectifs-fournisseur")
@RequiredArgsConstructor
@RequireFeature(CleFonctionnalite.OBJECTIFS_FOURNISSEUR)
public class ObjectifFournisseurController {

    private final ObjectifFournisseurService service;

    @PostMapping
    public ResponseEntity<ObjectifFournisseurDto> creer(@RequestBody ObjectifFournisseurRequest request) {
        return ResponseEntity.ok(service.creer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObjectifFournisseurDto> modifier(@PathVariable Long id,
                                                           @RequestBody ObjectifFournisseurRequest request) {
        return ResponseEntity.ok(service.modifier(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjectifFournisseurDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ObjectifFournisseurDto>> getTous() {
        return ResponseEntity.ok(service.getTous());
    }

    @GetMapping("/mois")
    public ResponseEntity<List<ObjectifFournisseurDto>> getParMoisAnnee(
            @RequestParam int mois, @RequestParam int annee) {
        return ResponseEntity.ok(service.getParMoisAnnee(mois, annee));
    }

    @GetMapping("/fournisseur/{fournisseurId}")
    public ResponseEntity<List<ObjectifFournisseurDto>> getParFournisseur(@PathVariable Long fournisseurId) {
        return ResponseEntity.ok(service.getParFournisseur(fournisseurId));
    }

    @GetMapping("/annee")
    public ResponseEntity<List<ObjectifFournisseurDto>> getParAnnee(@RequestParam int annee) {
        return ResponseEntity.ok(service.getParAnnee(annee));
    }

    @PatchMapping("/{id}/valider")
    public ResponseEntity<ObjectifFournisseurDto> valider(@PathVariable Long id) {
        return ResponseEntity.ok(service.valider(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistiques")
    public ResponseEntity<StatsObjectifDto> getStatistiques(
            @RequestParam int mois, @RequestParam int annee) {
        return ResponseEntity.ok(service.getStatistiques(mois, annee));
    }

    @GetMapping("/rapport/mensuel")
    public ResponseEntity<Map<String, Object>> getRapportMensuel(
            @RequestParam int mois, @RequestParam int annee) {
        return ResponseEntity.ok(service.getRapportMensuel(mois, annee));
    }

    @GetMapping("/rapport/annuel")
    public ResponseEntity<Map<String, Object>> getRapportAnnuel(@RequestParam int annee) {
        return ResponseEntity.ok(service.getRapportAnnuel(annee));
    }
}
