package com.ges.boutique.rapport;

import com.ges.boutique.vente.LigneVenteRepository;
import com.ges.boutique.vente.Vente;
import com.ges.boutique.vente.VenteRepository;
import com.ges.boutique.produit.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
public class RapportAnalytiqueController {

    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final ProduitRepository produitRepository;

    /**
     * GET /api/rapports/ca-30-jours
     * CA groupé par date sur les 30 derniers jours
     */
    @GetMapping("/ca-30-jours")
    public ResponseEntity<List<Map<String, Object>>> ca30Jours() {
        LocalDateTime debut = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDateTime fin = LocalDateTime.now();
        List<Vente> ventes = venteRepository.findByDateRange(debut, fin);

        // Grouper par date et sommer montantTotal
        Map<String, Double> parDate = new TreeMap<>();
        // Initialiser les 30 derniers jours à 0
        for (int i = 29; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            parDate.put(date, 0.0);
        }
        for (Vente v : ventes) {
            if (v.getAnnulee() == null || !v.getAnnulee()) {
                String date = v.getDateVente().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                parDate.merge(date, v.getMontantTotal() != null ? v.getMontantTotal() : 0.0, Double::sum);
            }
        }

        List<Map<String, Object>> result = parDate.entrySet().stream()
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", e.getKey());
                m.put("ca", e.getValue());
                return m;
            }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rapports/top-produits
     * Top 10 produits les plus vendus sur 30 jours
     */
    @GetMapping("/top-produits")
    public ResponseEntity<List<Map<String, Object>>> topProduits() {
        LocalDateTime debut = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDateTime fin = LocalDateTime.now();
        List<Vente> ventes = venteRepository.findByDateRange(debut, fin);

        // Agréger par nom de produit
        Map<String, double[]> agreg = new LinkedHashMap<>(); // [quantite, ca, prixAchat]
        for (Vente v : ventes) {
            if (v.getAnnulee() != null && v.getAnnulee()) continue;
            if (v.getLignes() == null) continue;
            v.getLignes().forEach(l -> {
                String nom = l.getProduitNom() != null ? l.getProduitNom() :
                    (l.getProduit() != null ? l.getProduit().getNom() : "?");
                agreg.computeIfAbsent(nom, k -> new double[3]);
                agreg.get(nom)[0] += l.getQuantite() != null ? l.getQuantite() : 0;
                agreg.get(nom)[1] += l.getSousTotal() != null ? l.getSousTotal() : 0;
                agreg.get(nom)[2] += (l.getPrixAchat() != null ? l.getPrixAchat() : 0)
                    * (l.getQuantite() != null ? l.getQuantite() : 0);
            });
        }

        List<Map<String, Object>> result = agreg.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
            .limit(10)
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("produitNom", e.getKey());
                m.put("quantiteVendue", (long) e.getValue()[0]);
                m.put("ca", e.getValue()[1]);
                return m;
            }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rapports/ventes-par-heure
     * Ventes groupées par heure sur 30 jours
     */
    @GetMapping("/ventes-par-heure")
    public ResponseEntity<List<Map<String, Object>>> ventesParHeure() {
        LocalDateTime debut = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDateTime fin = LocalDateTime.now();
        List<Vente> ventes = venteRepository.findByDateRange(debut, fin);

        Map<Integer, Long> parHeure = new TreeMap<>();
        for (int h = 0; h < 24; h++) parHeure.put(h, 0L);
        for (Vente v : ventes) {
            if (v.getAnnulee() != null && v.getAnnulee()) continue;
            if (v.getDateVente() != null) {
                int h = v.getDateVente().getHour();
                parHeure.merge(h, 1L, Long::sum);
            }
        }

        List<Map<String, Object>> result = parHeure.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("heure", e.getKey());
                m.put("nbVentes", e.getValue());
                return m;
            }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rapports/marges
     * Marges par produit sur 30 jours
     */
    @GetMapping("/marges")
    public ResponseEntity<List<Map<String, Object>>> marges() {
        LocalDateTime debut = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDateTime fin = LocalDateTime.now();
        List<Vente> ventes = venteRepository.findByDateRange(debut, fin);

        Map<String, double[]> agreg = new LinkedHashMap<>();
        for (Vente v : ventes) {
            if (v.getAnnulee() != null && v.getAnnulee()) continue;
            if (v.getLignes() == null) continue;
            v.getLignes().forEach(l -> {
                String nom = l.getProduitNom() != null ? l.getProduitNom() :
                    (l.getProduit() != null ? l.getProduit().getNom() : "?");
                agreg.computeIfAbsent(nom, k -> new double[2]); // [ca, coutAchat]
                agreg.get(nom)[0] += l.getSousTotal() != null ? l.getSousTotal() : 0;
                agreg.get(nom)[1] += (l.getPrixAchat() != null ? l.getPrixAchat() : 0)
                    * (l.getQuantite() != null ? l.getQuantite() : 0);
            });
        }

        List<Map<String, Object>> result = agreg.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
            .map(e -> {
                double ca = e.getValue()[0];
                double cout = e.getValue()[1];
                double marge = ca - cout;
                double taux = ca > 0 ? Math.round(marge / ca * 100) : 0;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("produitNom", e.getKey());
                m.put("ca", ca);
                m.put("coutAchat", cout);
                m.put("marge", marge);
                m.put("tauxMarge", taux);
                return m;
            }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
