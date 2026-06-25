package com.ges.boutique.commande;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
public class CommandeController {

    private final CommandeService commandeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<List<Commande>> getAll() {
        return ResponseEntity.ok(commandeService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Commande> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.findById(id));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<List<Commande>> getByStatut(@PathVariable StatutCommande statut) {
        return ResponseEntity.ok(commandeService.findByStatut(statut));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> creer(@RequestBody CommandeRequest request) {
        Commande commande = commandeService.creer(request);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Commande créée");
        resp.put("commande", commande);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> modifier(@PathVariable Long id, @RequestBody CommandeRequest request) {
        Commande commande = commandeService.modifier(id, request);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Commande modifiée");
        resp.put("commande", commande);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> valider(@PathVariable Long id) {
        Commande commande = commandeService.valider(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Commande validée — vente créée N°" + commande.getVenteId());
        resp.put("commande", commande);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable Long id) {
        commandeService.supprimer(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Commande supprimée");
        return ResponseEntity.ok(resp);
    }
}
