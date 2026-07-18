package com.ges.boutique.ia;

import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de prévisions basé sur les données de vente réelles (90 jours).
 * Calcule : vélocité journalière, rupture estimée, stock dormant, quantité réappro.
 */
@Service
@RequiredArgsConstructor
public class PrevisionService {

    private final VenteRepository venteRepository;
    private final ProduitRepository produitRepository;

    public List<Map<String, Object>> genererPrevisions() {
        List<Produit> produits = produitRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate aujourdhui = LocalDate.now();
        LocalDateTime debut90j = aujourdhui.minusDays(90).atStartOfDay();

        // Charger la vélocité une seule fois pour tous les produits
        List<Object[]> velociteData = venteRepository.findVelociteProduits(debut90j);

        // Construire une map produitId → totalVendu pour accès rapide
        Map<Long, Double> velociteParProduit = new HashMap<>();
        for (Object[] row : velociteData) {
            Long produitId = ((Number) row[0]).longValue();
            Double totalVendu = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            velociteParProduit.put(produitId, totalVendu);
        }

        for (Produit p : produits) {
            double totalVendu = velociteParProduit.getOrDefault(p.getId(), 0.0);
            double ventesMoyennesParJour = totalVendu / 90.0;

            Map<String, Object> prevision = new HashMap<>();
            prevision.put("produitId", p.getId());
            prevision.put("produitNom", p.getNom());
            prevision.put("stockActuel", p.getQuantite());
            prevision.put("ventesMoyennesParJour",
                    Math.round(ventesMoyennesParJour * 100.0) / 100.0);

            // Prévisions ventes J+7 et J+30
            prevision.put("previsionVentes7j", Math.round(ventesMoyennesParJour * 7));
            prevision.put("previsionVentes30j", Math.round(ventesMoyennesParJour * 30));

            // Rupture prévue dans N jours
            if (ventesMoyennesParJour > 0 && p.getQuantite() > 0) {
                double joursAvantRupture = p.getQuantite() / ventesMoyennesParJour;
                prevision.put("joursAvantRupture", Math.round(joursAvantRupture));
                prevision.put("dateRuptureEstimee",
                        aujourdhui.plusDays(Math.round(joursAvantRupture)).toString());
                prevision.put("alerteRupture", joursAvantRupture <= 7);
                prevision.put("ruptureImminente", joursAvantRupture <= 3);
            } else {
                // Pas de ventes : indiquer -1 (jamais de rupture prévue) ou 0 si stock vide
                prevision.put("joursAvantRupture", ventesMoyennesParJour == 0 ? -1 : 0);
                prevision.put("dateRuptureEstimee", null);
                prevision.put("alerteRupture", p.getQuantite() == 0);
                prevision.put("ruptureImminente", p.getQuantite() == 0);
            }

            // Stock dormant : aucune vente depuis 90 jours mais stock > 0
            prevision.put("stockDormant", ventesMoyennesParJour == 0 && p.getQuantite() > 0);

            // Recommandation quantité réappro pour couvrir 30 jours
            long qteReappro = Math.round(ventesMoyennesParJour * 30 - p.getQuantite());
            prevision.put("quantiteReappro", Math.max(0, qteReappro));

            result.add(prevision);
        }

        // Trier : rupture imminente → alertes → dormants → reste
        result.sort((a, b) -> {
            boolean aImm = Boolean.TRUE.equals(a.get("ruptureImminente"));
            boolean bImm = Boolean.TRUE.equals(b.get("ruptureImminente"));
            if (aImm != bImm) return bImm ? 1 : -1;

            boolean aAlerte = Boolean.TRUE.equals(a.get("alerteRupture"));
            boolean bAlerte = Boolean.TRUE.equals(b.get("alerteRupture"));
            if (aAlerte != bAlerte) return bAlerte ? 1 : -1;

            boolean aDormant = Boolean.TRUE.equals(a.get("stockDormant"));
            boolean bDormant = Boolean.TRUE.equals(b.get("stockDormant"));
            if (aDormant != bDormant) return bDormant ? 1 : -1;

            return 0;
        });

        return result;
    }
}
