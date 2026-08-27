package com.ges.boutique.vitrine;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Mini-site vitrine automatique — endpoints PUBLICS (sans authentification).
 * Permet à un client, sans se connecter, de consulter le catalogue produits
 * et les infos de la boutique avant de se déplacer.
 *
 * Sécurité : voir SecurityConfig (/api/vitrine/** est permitAll) et les DTOs
 * VitrineProduitDto / VitrineInfoDto qui excluent volontairement tout champ
 * interne (prix d'achat, fournisseur, quantité exacte, seuil d'alerte, etc.).
 */
@RestController
@RequestMapping("/api/vitrine")
@RequiredArgsConstructor
public class VitrineController {

    private final VitrineService vitrineService;

    @GetMapping("/produits")
    public List<VitrineProduitDto> obtenirProduits() {
        return vitrineService.obtenirProduitsVitrine();
    }

    @GetMapping("/infos")
    public VitrineInfoDto obtenirInfos() {
        return vitrineService.obtenirInfosVitrine();
    }
}
