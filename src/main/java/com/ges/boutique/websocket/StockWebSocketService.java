package com.ges.boutique.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service WebSocket dédié aux notifications de stock en temps réel.
 * Envoie sur des topics par boutiqueId pour supporter l'architecture multi-boutiques.
 * Dans l'architecture actuelle (une instance = une boutique), boutiqueId = 1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Diffuse une mise à jour de stock sur /topic/stock/{boutiqueId}.
     */
    public void diffuserMiseAJourStock(Long boutiqueId, Long produitId, String nomProduit, int nouvelleQuantite) {
        try {
            messagingTemplate.convertAndSend(
                "/topic/stock/" + boutiqueId,
                Map.of(
                    "produitId", produitId,
                    "nom", nomProduit,
                    "quantite", nouvelleQuantite,
                    "timestamp", System.currentTimeMillis()
                )
            );
        } catch (Exception e) {
            log.warn("Erreur diffusion stock WebSocket vers boutique {}: {}", boutiqueId, e.getMessage());
        }
    }

    /**
     * Diffuse une alerte de stock (rupture, stock faible) sur /topic/alertes/{boutiqueId}.
     */
    public void diffuserAlerteStock(Long boutiqueId, String message) {
        try {
            messagingTemplate.convertAndSend(
                "/topic/alertes/" + boutiqueId,
                Map.of(
                    "message", message,
                    "timestamp", System.currentTimeMillis()
                )
            );
        } catch (Exception e) {
            log.warn("Erreur diffusion alerte WebSocket vers boutique {}: {}", boutiqueId, e.getMessage());
        }
    }

    /**
     * Diffuse un événement de transfert inter-boutiques sur /topic/transferts/{boutiqueId}.
     */
    public void diffuserTransfert(Long boutiqueId, Object transfert) {
        try {
            messagingTemplate.convertAndSend("/topic/transferts/" + boutiqueId, transfert);
        } catch (Exception e) {
            log.warn("Erreur diffusion transfert WebSocket vers boutique {}: {}", boutiqueId, e.getMessage());
        }
    }
}
