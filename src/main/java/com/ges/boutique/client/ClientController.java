package com.ges.boutique.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> creerClient(@RequestBody Client client) {
        Client nouveauClient = clientService.creerClient(client);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Client créé avec succès");
        response.put("client", nouveauClient);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> modifierClient(@PathVariable Long id, @RequestBody Client client) {
        Client clientModifie = clientService.modifierClient(id, client);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Client modifié avec succès");
        response.put("client", clientModifie);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> supprimerClient(@PathVariable Long id) {
        clientService.supprimerClient(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Client supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> trouverParId(@PathVariable Long id) {
        Optional<Client> client = clientService.trouverParId(id);
        if (client.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("client", client.get());
            return ResponseEntity.ok(response);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Client non trouvé");
        return ResponseEntity.status(404).body(response);
    }

    @GetMapping("/telephone/{numeroTelephone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> trouverParNumeroTelephone(@PathVariable String numeroTelephone) {
        Optional<Client> client = clientService.trouverParNumeroTelephone(numeroTelephone);
        if (client.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("client", client.get());
            return ResponseEntity.ok(response);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Client non trouvé");
        return ResponseEntity.status(404).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> trouverTous() {
        List<Client> clients = clientService.trouverTous();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("clients", clients);
        response.put("nombreClients", clients.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recherche")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> rechercher(@RequestParam String query) {
        List<Client> clients = clientService.rechercherParNomOuPrenom(query, query);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("clients", clients);
        response.put("nombreResultats", clients.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    public ResponseEntity<Map<String, Object>> trouverTopClients() {
        List<Object[]> results = clientService.trouverTopClientsParMontant();
        List<Map<String, Object>> topClients = results.stream().map(result -> {
            Client client = (Client) result[0];
            Long nombreAchats = (Long) result[1];
            Double montantTotal = (Double) result[2];
            Map<String, Object> clientInfo = new HashMap<>();
            clientInfo.put("id", client.getId());
            clientInfo.put("nom", client.getNom());
            clientInfo.put("prenom", client.getPrenom());
            clientInfo.put("numeroTelephone", client.getNumeroTelephone());
            clientInfo.put("nombreAchats", nombreAchats);
            clientInfo.put("montantTotal", montantTotal);
            return clientInfo;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("topClients", topClients);
        return ResponseEntity.ok(response);
    }
}