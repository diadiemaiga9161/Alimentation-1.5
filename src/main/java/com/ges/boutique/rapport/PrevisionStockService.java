package com.ges.boutique.rapport;

import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.vente.Vente;
import com.ges.boutique.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrevisionStockService {

    private final ProduitRepository produitRepository;
    private final VenteRepository venteRepository;

    public List<PrevisionStockDTO> calculerPrevisions() {
        LocalDateTime debut = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDateTime fin = LocalDateTime.now();
        List<Vente> ventesRecentes = venteRepository.findByDateRange(debut, fin);

        // Calculer vélocité par produitId
        Map<Long, Integer> quantitesVendues = new HashMap<>();
        for (Vente v : ventesRecentes) {
            if (v.getAnnulee() != null && v.getAnnulee()) continue;
            if (v.getLignes() == null) continue;
            v.getLignes().forEach(l -> {
                if (l.getProduit() != null && l.getQuantite() != null) {
                    quantitesVendues.merge(l.getProduit().getId(), l.getQuantite(), Integer::sum);
                }
            });
        }

        List<Produit> produits = produitRepository.findAll();
        List<PrevisionStockDTO> previsions = new ArrayList<>();

        for (Produit p : produits) {
            int qteVendue = quantitesVendues.getOrDefault(p.getId(), 0);
            double velocite = qteVendue / 30.0;

            int joursAvantRupture;
            LocalDate datePrevueRupture;
            if (velocite > 0) {
                joursAvantRupture = (int) Math.floor(p.getQuantite() / velocite);
                datePrevueRupture = LocalDate.now().plusDays(joursAvantRupture);
            } else {
                joursAvantRupture = 999;
                datePrevueRupture = LocalDate.now().plusDays(999);
            }

            int qteRecommandee = (int) Math.ceil(velocite * 30 * 1.2);
            String urgence;
            if (joursAvantRupture < 7) urgence = "CRITIQUE";
            else if (joursAvantRupture < 15) urgence = "ATTENTION";
            else urgence = "OK";

            PrevisionStockDTO dto = new PrevisionStockDTO(
                p.getId(),
                p.getNom(),
                p.getQuantite(),
                Math.round(velocite * 100.0) / 100.0,
                Math.min(joursAvantRupture, 999),
                qteRecommandee,
                joursAvantRupture < 999 ? datePrevueRupture : null,
                urgence
            );
            previsions.add(dto);
        }

        // Trier par joursAvantRupture croissant (CRITIQUE en premier)
        previsions.sort(Comparator.comparingInt(PrevisionStockDTO::getJoursAvantRupture));
        return previsions;
    }
}
