package com.ges.boutique.vitrine;

import com.ges.boutique.commande.Commande;
import com.ges.boutique.commande.CommandeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mini-site vitrine automatique — endpoints PUBLICS (sans authentification).
 * Permet à un client, sans se connecter, de consulter le catalogue produits,
 * les infos de la boutique, et de passer commande (récupérée/validée en interne
 * via la page Commandes existante — pas de paiement en ligne pour l'instant).
 *
 * Sécurité : voir SecurityConfig (/api/vitrine/** est permitAll) et les DTOs
 * VitrineProduitDto / VitrineInfoDto qui excluent volontairement tout champ
 * interne (prix d'achat, fournisseur, quantité exacte, seuil d'alerte, etc.).
 */
@Slf4j
@RestController
@RequestMapping("/api/vitrine")
@RequiredArgsConstructor
public class VitrineController {

    private final VitrineService vitrineService;
    private final CommandeService commandeService;

    @GetMapping("/produits")
    public List<VitrineProduitDto> obtenirProduits() {
        return vitrineService.obtenirProduitsVitrine();
    }

    @GetMapping("/infos")
    public VitrineInfoDto obtenirInfos() {
        return vitrineService.obtenirInfosVitrine();
    }

    @PostMapping("/commande")
    public ResponseEntity<Map<String, Object>> passerCommande(@RequestBody VitrineCommandeRequest request) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Commande commande = commandeService.creerDepuisVitrine(request);
            resp.put("success", true);
            resp.put("numeroCommande", commande.getNumeroCommande());
            resp.put("message", "Commande envoyée, la boutique va la préparer");
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException | IllegalStateException e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        } catch (Exception e) {
            log.error("Erreur commande vitrine: {}", e.getMessage());
            resp.put("success", false);
            resp.put("message", "Impossible d'enregistrer la commande, réessayez.");
            return ResponseEntity.internalServerError().body(resp);
        }
    }
}
