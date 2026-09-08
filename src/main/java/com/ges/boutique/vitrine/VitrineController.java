package com.ges.boutique.vitrine;

import com.ges.boutique.commande.Commande;
import com.ges.boutique.commande.CommandeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Manifeste PWA de la vitrine, au nom/logo de LA BOUTIQUE — servi comme une vraie
     * ressource HTTP (pas un blob: généré en JS) pour que l'installation ("Ajouter à
     * l'écran d'accueil") ouvre bien le catalogue (start_url=/vitrine) et non l'appli de
     * gestion du personnel. Un manifeste swappé dynamiquement via un blob: n'est pas
     * traité de façon fiable par le moteur d'installation de tous les navigateurs — une
     * vraie URL fetchable règle ce problème. Voir index.html (bascule au chargement pour
     * une entrée directe sur /vitrine) et vitrine.component.ts (bascule en navigation SPA).
     */
    @GetMapping(value = "/manifest.webmanifest", produces = "application/manifest+json")
    public Map<String, Object> obtenirManifeste() {
        VitrineInfoDto infos = vitrineService.obtenirInfosVitrine();
        String nom = (infos.getNom() == null || infos.getNom().isBlank()) ? "Boutique" : infos.getNom().trim();

        List<Map<String, Object>> icones = new ArrayList<>();
        if (infos.getLogoPath() != null && !infos.getLogoPath().isBlank()) {
            String type = "image/png";
            Matcher m = Pattern.compile("^data:([^;]+);").matcher(infos.getLogoPath());
            if (m.find()) type = m.group(1);
            for (int taille : new int[]{192, 512}) {
                Map<String, Object> icone = new HashMap<>();
                icone.put("src", infos.getLogoPath());
                icone.put("sizes", taille + "x" + taille);
                icone.put("type", type);
                icones.add(icone);
            }
        } else {
            for (int taille : new int[]{192, 512}) {
                Map<String, Object> icone = new HashMap<>();
                icone.put("src", "/assets/icons/icon-" + taille + "x" + taille + ".png");
                icone.put("sizes", taille + "x" + taille);
                icone.put("type", "image/png");
                icones.add(icone);
            }
        }

        Map<String, Object> manifeste = new HashMap<>();
        manifeste.put("name", nom + " — Boutique en ligne");
        manifeste.put("short_name", nom.length() > 30 ? nom.substring(0, 30) : nom);
        manifeste.put("start_url", "/vitrine");
        manifeste.put("scope", "/vitrine");
        manifeste.put("display", "standalone");
        manifeste.put("background_color", "#081648");
        manifeste.put("theme_color", "#1447c0");
        manifeste.put("orientation", "portrait");
        manifeste.put("lang", "fr");
        manifeste.put("icons", icones);
        return manifeste;
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
