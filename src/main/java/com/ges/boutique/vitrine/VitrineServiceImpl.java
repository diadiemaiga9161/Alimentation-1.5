package com.ges.boutique.vitrine;

import com.ges.boutique.boutique.Boutique;
import com.ges.boutique.boutique.BoutiqueService;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.promo.Promotion;
import com.ges.boutique.promo.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service dédié au mini-site vitrine PUBLIC (sans authentification).
 * Ne réimplémente aucune logique métier existante : réutilise
 * PromotionService (promos actives) et BoutiqueService (infos boutique),
 * et se contente de mapper vers des DTOs minimaux sans champs internes.
 */
@Service
@RequiredArgsConstructor
public class VitrineServiceImpl implements VitrineService {

    private final ProduitRepository produitRepository;
    private final PromotionService promotionService;
    private final BoutiqueService boutiqueService;

    @Override
    public List<VitrineProduitDto> obtenirProduitsVitrine() {
        List<Produit> produits = produitRepository.findAll();
        return produits.stream()
                .map(this::versDto)
                .collect(Collectors.toList());
    }

    @Override
    public VitrineInfoDto obtenirInfosVitrine() {
        Boutique boutique = boutiqueService.obtenirBoutique();
        VitrineInfoDto dto = new VitrineInfoDto();
        dto.setNom(boutique.getNom());
        dto.setAdresse(boutique.getAdresse());
        dto.setTelephone(boutique.getTelephone());
        dto.setHorairesOuverture(boutique.getHorairesOuverture());
        dto.setLogoPath(boutique.getLogoPath());
        return dto;
    }

    private VitrineProduitDto versDto(Produit produit) {
        VitrineProduitDto dto = new VitrineProduitDto();
        dto.setId(produit.getId());
        dto.setNom(produit.getNom());
        dto.setCategorieNom(produit.getCategorie() != null ? produit.getCategorie().getNom() : null);
        dto.setPrixVente(produit.getPrixVente());
        int quantite = produit.getQuantite() != null ? produit.getQuantite() : 0;
        dto.setDisponible(quantite > 0);
        if (quantite <= 0) {
            dto.setStatutStock("RUPTURE");
        } else if (produit.estStockFaible()) {
            dto.setStatutStock("STOCK_FAIBLE");
        } else {
            dto.setStatutStock("DISPONIBLE");
        }

        Promotion promo = meilleurePromo(produit);
        if (promo != null) {
            dto.setEnPromotion(true);
            dto.setPromotionTitre(promo.getTitre());
            dto.setPromotionReduction(formatReduction(promo));
        } else {
            dto.setEnPromotion(false);
            dto.setPromotionTitre(null);
            dto.setPromotionReduction(null);
        }

        return dto;
    }

    /** Parmi les promos applicables au produit, retient la plus avantageuse (réduction équivalente la plus élevée). */
    private Promotion meilleurePromo(Produit produit) {
        List<Promotion> promos = promotionService.obtenirPromosPourProduit(produit.getId());
        if (promos == null || promos.isEmpty()) {
            return null;
        }
        return promos.stream()
                .max(Comparator.comparingDouble(p -> montantReduction(p, produit.getPrixVente())))
                .orElse(null);
    }

    private double montantReduction(Promotion promo, Double prixVente) {
        if (promo.getValeurReduction() == null) {
            return 0d;
        }
        if ("POURCENTAGE".equals(promo.getTypeReduction())) {
            double prix = prixVente != null ? prixVente : 0d;
            return prix * promo.getValeurReduction() / 100d;
        }
        return promo.getValeurReduction();
    }

    private String formatReduction(Promotion promo) {
        if (promo.getValeurReduction() == null) {
            return null;
        }
        return "POURCENTAGE".equals(promo.getTypeReduction())
                ? promo.getValeurReduction().intValue() + "%"
                : promo.getValeurReduction().intValue() + " FCFA";
    }
}
