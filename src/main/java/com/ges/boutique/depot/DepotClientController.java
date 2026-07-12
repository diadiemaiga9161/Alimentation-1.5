package com.ges.boutique.depot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depot-clients")
@RequiredArgsConstructor
public class DepotClientController {

    private final DepotClientService service;

    @GetMapping
    public ResponseEntity<List<DepotClientDto>> getTous() {
        return ResponseEntity.ok(service.getTous());
    }

    @GetMapping("/search")
    public ResponseEntity<List<DepotClientDto>> rechercher(@RequestParam String q) {
        return ResponseEntity.ok(service.rechercher(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepotClientDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<DepotClientDto> creer(@RequestBody DepotClientRequest request) {
        return ResponseEntity.ok(service.creer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepotClientDto> modifier(@PathVariable Long id, @RequestBody DepotClientRequest request) {
        return ResponseEntity.ok(service.modifier(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
