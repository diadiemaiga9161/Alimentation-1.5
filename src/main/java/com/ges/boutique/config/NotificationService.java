package com.ges.boutique.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // ==================== VENTES ====================

    public void notifierNouvelleVente(Object venteInfo) {
        envoyer("/topic/ventes", buildEvent("NOUVELLE_VENTE", venteInfo));
    }

    public void notifierVenteAnnulee(Object venteInfo) {
        envoyer("/topic/ventes", buildEvent("VENTE_ANNULEE", venteInfo));
    }

    public void notifierReglementCredit(Object creditInfo) {
        envoyer("/topic/ventes", buildEvent("REGLEMENT_CREDIT", creditInfo));
        envoyer("/topic/caisse", buildEvent("REGLEMENT_CREDIT", creditInfo));
    }

    // ==================== CAISSE ====================

    public void notifierOperationCaisse(String typeOperation, Object operationInfo) {
        envoyer("/topic/caisse", buildEvent(typeOperation, operationInfo));
    }

    public void notifierOuvertureCaisse(Object caisseInfo) {
        envoyer("/topic/caisse", buildEvent("OUVERTURE_CAISSE", caisseInfo));
    }

    public void notifierFermetureCaisse(Object caisseInfo) {
        envoyer("/topic/caisse", buildEvent("FERMETURE_CAISSE", caisseInfo));
    }

    // ==================== STOCK / PRODUITS ====================

    public void notifierMiseAJourStock(Long produitId, String produitNom, int nouvelleQuantite) {
        Map<String, Object> data = new HashMap<>();
        data.put("produitId", produitId);
        data.put("produitNom", produitNom);
        data.put("quantite", nouvelleQuantite);
        envoyer("/topic/stock", buildEvent("STOCK_MISE_A_JOUR", data));

        if (nouvelleQuantite == 0) {
            envoyer("/topic/stock", buildEvent("RUPTURE_STOCK", data));
            notifierAlerte("RUPTURE_STOCK", "Rupture de stock : " + produitNom);
        }
    }

    // ==================== ALERTES GÉNÉRALES ====================

    public void notifierAlerte(String type, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("message", message);
        envoyer("/topic/notifications", buildEvent("ALERTE", data));
    }

    // ==================== NOTIFICATIONS PERSISTÉES ====================

    public void notifierNouvelleNotification(Object data) {
        envoyer("/topic/notifications/badge", buildEvent("BADGE_UPDATE", data));
    }

    // ==================== DASHBOARD ====================

    public void notifierMiseAJourDashboard() {
        envoyer("/topic/dashboard", buildEvent("REFRESH", null));
    }

    // ==================== UTILITAIRE ====================

    private Map<String, Object> buildEvent(String type, Object data) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("data", data);
        return event;
    }

    private void envoyer(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.warn("Impossible d'envoyer notification WebSocket vers {}: {}", destination, e.getMessage());
        }
    }
}
