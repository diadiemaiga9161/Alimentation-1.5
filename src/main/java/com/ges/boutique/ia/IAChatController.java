package com.ges.boutique.ia;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
@Tag(name = "Assistant IA", description = "Chatbot intelligent pour la gestion de boutique")
public class IAChatController {

    private final IAChatService iaChatService;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Poser une question à l'assistant IA")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("reponse", "La question est vide."));
        }
        String reponse = iaChatService.repondre(question.trim());
        return ResponseEntity.ok(Map.of("reponse", reponse));
    }

    @GetMapping("/questions-predefinies")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Liste des questions prédéfinies")
    public ResponseEntity<java.util.List<Map<String, String>>> getQuestionsPredefinies() {
        var questions = java.util.List.of(
            Map.of("id", "ventes_jour", "label", "Mes ventes d'aujourd'hui", "icone", "cart-outline"),
            Map.of("id", "comptant_credit", "label", "Comptant vs Crédit ce mois", "icone", "stats-chart-outline"),
            Map.of("id", "mobile_money", "label", "Orange & Moov Money", "icone", "phone-portrait-outline"),
            Map.of("id", "credits_dus", "label", "Clients avec des dettes", "icone", "people-outline"),
            Map.of("id", "stock_faible", "label", "Produits en rupture de stock", "icone", "warning-outline"),
            Map.of("id", "conseils", "label", "Donne-moi des conseils", "icone", "bulb-outline"),
            Map.of("id", "augmenter_benefices", "label", "Augmenter mes bénéfices", "icone", "rocket-outline"),
            Map.of("id", "produits_rentables", "label", "Produits les plus rentables", "icone", "ribbon-outline"),
            Map.of("id", "bilan_semaine", "label", "Bilan de la semaine", "icone", "calendar-outline"),
            Map.of("id", "bilan_mois", "label", "Bilan du mois", "icone", "trending-up-outline"),
            Map.of("id", "sorties_stock", "label", "Sorties de stock du mois", "icone", "arrow-up-circle-outline"),
            Map.of("id", "ventes_annulees", "label", "Ventes annulées du mois", "icone", "close-circle-outline"),
            Map.of("id", "stock_produits", "label", "État de mon stock", "icone", "cube-outline"),
            Map.of("id", "rapport_complet", "label", "Rapport complet", "icone", "document-text-outline"),
            Map.of("id", "comment_ca_marche", "label", "Comment fonctionne Ges Boutique ?", "icone", "help-circle-outline"),
            Map.of("id", "ventes_vendeur", "label", "Qui a fait les ventes ?", "icone", "person-outline")
        );
        return ResponseEntity.ok(questions);
    }
}
