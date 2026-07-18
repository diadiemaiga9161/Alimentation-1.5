package com.ges.boutique.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoint batch sync pour les clients mobiles en mode offline.
 * Reçoit une liste d'opérations accumulées hors-ligne et les traite en lot.
 * L'intégration métier complète est ajoutée progressivement (actuellement : accusé de réception).
 */
@Slf4j
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    /**
     * Reçoit un batch d'opérations offline depuis un client mobile.
     * Body : { "operations": [ { "id": "uuid", "type": "VENTE|CREDIT|...", "payload": {...} } ] }
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> syncBatch(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> operations =
                (List<Map<String, Object>>) payload.get("operations");

        if (operations == null || operations.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erreur", "Aucune opération fournie"));
        }

        List<Map<String, Object>> resultats = new ArrayList<>();
        int succes = 0;
        int erreurs = 0;

        for (Map<String, Object> op : operations) {
            String id   = (String) op.get("id");
            String type = (String) op.get("type");

            log.info("[SYNC] Opération reçue — id={}, type={}", id, type);

            Map<String, Object> resultat = new HashMap<>();
            resultat.put("id",      id);
            resultat.put("type",    type);
            resultat.put("statut",  "RECU");
            resultat.put("message", "Opération enregistrée — traitement en cours");
            resultats.add(resultat);
            succes++;
        }

        return ResponseEntity.ok(Map.of(
                "succes",    succes,
                "erreurs",   erreurs,
                "total",     operations.size(),
                "resultats", resultats
        ));
    }

    /**
     * Endpoint de vérification de connectivité pour les clients offline.
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "statut",    "OK",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
