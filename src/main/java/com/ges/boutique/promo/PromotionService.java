package com.ges.boutique.promo;

import java.util.List;
import java.util.Map;

public interface PromotionService {
    Promotion creer(PromotionRequest request);
    Promotion modifier(Long id, PromotionRequest request);
    Promotion obtenirParId(Long id);
    List<Promotion> obtenirToutes();
    List<Promotion> obtenirActives();
    void supprimer(Long id);
    Map<String, Object> preparerMessagesWhatsApp(Long promotionId);
    // Promotions applicables à un produit (globale OU liée au produit)
    List<Promotion> obtenirPromosPourProduit(Long produitId);
}
