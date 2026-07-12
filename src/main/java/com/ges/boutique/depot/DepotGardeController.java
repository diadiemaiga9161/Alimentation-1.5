package com.ges.boutique.depot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/depots-garde")
@RequiredArgsConstructor
public class DepotGardeController {

    private final DepotGardeService depotService;

    @PostMapping
    public ResponseEntity<DepotGardeDto> creer(@RequestBody DepotGardeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depotService.creerDepot(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepotGardeDto> modifier(@PathVariable Long id, @RequestBody DepotGardeRequest request) {
        return ResponseEntity.ok(depotService.modifierDepot(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepotGardeDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(depotService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<DepotGardeDto>> getTous() {
        return ResponseEntity.ok(depotService.getTous());
    }

    @GetMapping("/actifs")
    public ResponseEntity<List<DepotGardeDto>> getActifs() {
        return ResponseEntity.ok(depotService.getActifs());
    }

    @GetMapping("/rechercher")
    public ResponseEntity<List<DepotGardeDto>> rechercher(@RequestParam String q) {
        return ResponseEntity.ok(depotService.rechercher(q));
    }

    @PostMapping("/{id}/retrait")
    public ResponseEntity<DepotGardeDto> retrait(
            @PathVariable Long id,
            @RequestBody RetraitDepotRequest request) {
        return ResponseEntity.ok(depotService.effectuerRetrait(id, request));
    }

    @PatchMapping("/{id}/cloturer")
    public ResponseEntity<DepotGardeDto> cloturer(@PathVariable Long id) {
        return ResponseEntity.ok(depotService.cloturerDepot(id));
    }

    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> getStatistiques() {
        return ResponseEntity.ok(depotService.getStatistiques());
    }

    @GetMapping("/groupes-client")
    public ResponseEntity<List<ClientDepotGroupeDto>> getGroupesClient() {
        return ResponseEntity.ok(depotService.getDepotsGroupesParClient());
    }

    @PostMapping("/retrait-global")
    public ResponseEntity<List<DepotGardeDto>> retraitGlobal(@RequestBody RetraitGlobalRequest request) {
        return ResponseEntity.ok(depotService.effectuerRetraitGlobal(request));
    }
}
