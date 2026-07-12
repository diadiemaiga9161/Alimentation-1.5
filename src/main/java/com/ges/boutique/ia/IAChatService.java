package com.ges.boutique.ia;

import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.vente.ModePaiement;
import com.ges.boutique.vente.Vente;
import com.ges.boutique.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IAChatService {

    private final VenteRepository venteRepository;
    private final ProduitRepository produitRepository;

    @Value("${anthropic.api.key:}")
    private String anthropicKey;

    private static final List<ModePaiement> MOBILE_MONEY = List.of(ModePaiement.ORANGE_MONEY, ModePaiement.MOOV_MONEY);

    public String repondre(String question) {
        String contexte = construireContexte(question);
        return appellerClaude(question, contexte);
    }

    private String construireContexte(String question) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("Tu es un assistant de gestion de boutique. Tu réponds en français, de façon concise et claire.\n");
        ctx.append("Voici les données actuelles de la boutique :\n\n");

        try {
            LocalDateTime debutJour = LocalDate.now().atStartOfDay();
            LocalDateTime finJour = LocalDate.now().atTime(LocalTime.MAX);
            LocalDateTime debutMois = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime finMois = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX);
            LocalDateTime debutSemaine = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
            LocalDateTime finSemaine = LocalDate.now().with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);

            // Ventes du jour
            List<Vente> ventesJour = venteRepository.findByDateRange(debutJour, finJour);
            double caJour = ventesJour.stream().filter(v -> Boolean.FALSE.equals(v.getAnnulee()))
                    .mapToDouble(v -> v.getMontantTotal() != null ? v.getMontantTotal() : 0).sum();
            long nbVentesJour = ventesJour.stream().filter(v -> Boolean.FALSE.equals(v.getAnnulee())).count();
            long ventesComptantJour = ventesJour.stream()
                    .filter(v -> Boolean.FALSE.equals(v.getAnnulee()) && Boolean.FALSE.equals(v.getEstCredit())).count();
            long ventesCreditJour = ventesJour.stream()
                    .filter(v -> Boolean.FALSE.equals(v.getAnnulee()) && Boolean.TRUE.equals(v.getEstCredit())).count();

            ctx.append("=== AUJOURD'HUI (").append(LocalDate.now()).append(") ===\n");
            ctx.append("- Nombre de ventes : ").append(nbVentesJour).append("\n");
            ctx.append("- Chiffre d'affaires : ").append((long) caJour).append(" F CFA\n");
            ctx.append("- Ventes comptant : ").append(ventesComptantJour).append("\n");
            ctx.append("- Ventes crédit : ").append(ventesCreditJour).append("\n");

            double orangeJour = venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.ORANGE_MONEY, debutJour, finJour);
            double moovJour = venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.MOOV_MONEY, debutJour, finJour);
            ctx.append("- Orange Money : ").append((long) orangeJour).append(" F CFA\n");
            ctx.append("- Moov Money : ").append((long) moovJour).append(" F CFA\n");

            // Semaine
            double caSemaine = venteRepository.getCAByDateRange(debutSemaine, finSemaine);
            ctx.append("\n=== CETTE SEMAINE ===\n");
            ctx.append("- Chiffre d'affaires : ").append((long) caSemaine).append(" F CFA\n");

            // Mois
            double caMois = venteRepository.getCAByDateRange(debutMois, finMois);
            long nbVentesMois = venteRepository.countByDateRange(debutMois, finMois);
            double orangeMois = venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.ORANGE_MONEY, debutMois, finMois);
            double moovMois = venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.MOOV_MONEY, debutMois, finMois);
            ctx.append("\n=== CE MOIS ===\n");
            ctx.append("- Nombre de ventes : ").append(nbVentesMois).append("\n");
            ctx.append("- Chiffre d'affaires : ").append((long) caMois).append(" F CFA\n");
            ctx.append("- Orange Money : ").append((long) orangeMois).append(" F CFA\n");
            ctx.append("- Moov Money : ").append((long) moovMois).append(" F CFA\n");

            // Crédits non réglés
            double totalCreditsNonRegles = venteRepository.getTotalCreditsNonRegles();
            List<Vente> creditsNonRegles = venteRepository.findCreditsNonRegles();
            ctx.append("\n=== CRÉDITS NON RÉGLÉS ===\n");
            ctx.append("- Nombre : ").append(creditsNonRegles.size()).append("\n");
            ctx.append("- Montant total dû : ").append((long) totalCreditsNonRegles).append(" F CFA\n");

            // Produits stock faible
            long produitsRupture = produitRepository.compterProduitsStockFaible();
            ctx.append("\n=== STOCK ===\n");
            ctx.append("- Produits en alerte stock : ").append(produitsRupture).append("\n");

        } catch (Exception e) {
            log.error("Erreur construction contexte IA: {}", e.getMessage());
            ctx.append("(Données partiellement disponibles)\n");
        }

        return ctx.toString();
    }

    private String appellerClaude(String question, String contexte) {
        if (anthropicKey == null || anthropicKey.isBlank()) {
            return "L'assistant IA n'est pas configuré (clé API manquante).";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", anthropicKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "claude-haiku-4-5-20251001");
            body.put("max_tokens", 512);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", contexte + "\n\nQuestion : " + question));
            body.put("messages", messages);

            body.put("system", "Tu es un assistant de gestion de boutique en Afrique de l'Ouest. " +
                    "Tu réponds toujours en français, de façon concise, amicale et pratique. " +
                    "Tu utilises les vraies données fournies pour répondre avec précision. " +
                    "Tu donnes des conseils adaptés au contexte africain (commerce de détail, alimentation).");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.anthropic.com/v1/messages", request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
                if (content != null && !content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
            }
            return "Je n'ai pas pu obtenir une réponse. Réessaie dans un moment.";

        } catch (Exception e) {
            log.error("Erreur appel Claude API: {}", e.getMessage());
            return "Une erreur s'est produite lors de la communication avec l'assistant. Vérifie ta connexion.";
        }
    }
}
