package com.ges.boutique.avance;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/avances")
@RequiredArgsConstructor
public class AvanceClientController {

    private final AvanceClientService avanceService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> enregistrerAvance(@RequestBody AvanceClientRequest request) {
        AvanceClient avance = avanceService.enregistrerAvance(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Avance de " + avance.getMontant() + " F enregistrée pour " + avance.getClientNom(),
                "avance", AvanceClientDto.fromEntity(avance)
        ));
    }

    @GetMapping("/solde")
    public ResponseEntity<Map<String, Object>> getSolde(
            @RequestParam String clientNom,
            @RequestParam(required = false) String clientTelephone) {
        Double solde = avanceService.getSoldeDisponible(clientNom, clientTelephone);
        return ResponseEntity.ok(Map.of(
                "clientNom", clientNom,
                "soldeDisponible", solde
        ));
    }

    @GetMapping("/historique")
    public ResponseEntity<Map<String, Object>> getHistorique(
            @RequestParam String clientNom,
            @RequestParam(required = false) String clientTelephone) {
        List<AvanceClient> avances = avanceService.getHistoriqueParClient(clientNom, clientTelephone);
        Double solde = avanceService.getSoldeDisponible(clientNom, clientTelephone);
        List<AvanceClientDto> dtos = avances.stream().map(AvanceClientDto::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "clientNom", clientNom,
                "soldeDisponible", solde,
                "historique", dtos,
                "totalDepose", avances.stream().mapToDouble(AvanceClient::getMontant).sum(),
                "totalUtilise", avances.stream().mapToDouble(AvanceClient::getMontantUtilise).sum()
        ));
    }

    @GetMapping
    public ResponseEntity<List<AvanceClientDto>> getToutesLesAvances() {
        return ResponseEntity.ok(
                avanceService.getToutesLesAvances().stream()
                        .map(AvanceClientDto::fromEntity)
                        .collect(Collectors.toList())
        );
    }
}
