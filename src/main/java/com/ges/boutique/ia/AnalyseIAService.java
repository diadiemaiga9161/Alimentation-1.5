package com.ges.boutique.ia;

import com.ges.boutique.caisse.CaisseRepository;
import com.ges.boutique.ia.dto.AlerteIA;
import com.ges.boutique.ia.dto.AnalyseIAResult;
import com.ges.boutique.ia.dto.RecommandationIA;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.vente.Vente;
import com.ges.boutique.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyseIAService {

    private final VenteRepository venteRepository;
    private final ProduitRepository produitRepository;
    private final CaisseRepository caisseRepository;
    private final FeedbackRecommandationRepository feedbackRepository;
    private final ParametreModeleIARepository parametreRepository;

    /** Mapping DayOfWeek anglais → nom français stocké dans ProfilIA */
    private static final Map<DayOfWeek, String> JOURS_FR = new EnumMap<>(DayOfWeek.class);

    static {
        JOURS_FR.put(DayOfWeek.MONDAY,    "LUNDI");
        JOURS_FR.put(DayOfWeek.TUESDAY,   "MARDI");
        JOURS_FR.put(DayOfWeek.WEDNESDAY, "MERCREDI");
        JOURS_FR.put(DayOfWeek.THURSDAY,  "JEUDI");
        JOURS_FR.put(DayOfWeek.FRIDAY,    "VENDREDI");
        JOURS_FR.put(DayOfWeek.SATURDAY,  "SAMEDI");
        JOURS_FR.put(DayOfWeek.SUNDAY,    "DIMANCHE");
    }

    // ===================================================================
    // ALGORITHMES MATHÉMATIQUES
    // ===================================================================

    /**
     * Régression linéaire (moindres carrés).
     * Retourne [a (pente), b (intercept)] tel que y = a*x + b.
     */
    private double[] regressionLineaire(List<Double> valeurs) {
        int n = valeurs.size();
        if (n < 2) return new double[]{0, valeurs.isEmpty() ? 0 : valeurs.get(0)};

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double y = valeurs.get(i);
            sumX  += i;
            sumY  += y;
            sumXY += (double) i * y;
            sumX2 += (double) i * i;
        }

        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 1e-10) {
            return new double[]{0, n > 0 ? sumY / n : 0};
        }

        double a = (n * sumXY - sumX * sumY) / denom; // pente
        double b = (sumY - a * sumX) / n;              // intercept
        return new double[]{a, b};
    }

    /**
     * Moyenne Mobile Exponentielle (EMA).
     * alpha proche de 1 → très réactif aux données récentes.
     * alpha proche de 0 → très lissé (favorise l'historique).
     */
    private double ema(List<Double> valeurs, double alpha) {
        if (valeurs.isEmpty()) return 0;
        double result = valeurs.get(0);
        for (int i = 1; i < valeurs.size(); i++) {
            result = alpha * valeurs.get(i) + (1 - alpha) * result;
        }
        return result;
    }

    // ===================================================================
    // LECTURE DES PARAMÈTRES DU MODÈLE
    // ===================================================================

    private double getParametre(String cle, double defaut) {
        return parametreRepository.findById(cle)
                .map(ParametreModeleIA::getValeur)
                .orElse(defaut);
    }

    private void sauvegarderParametre(String cle, double valeur, double precision) {
        ParametreModeleIA param = parametreRepository.findById(cle)
                .orElseGet(() -> new ParametreModeleIA(cle, valeur));
        param.setValeur(valeur);
        param.setPrecisionModele(precision);
        param.setVersion(param.getVersion() + 1);
        param.setDateMiseAJour(LocalDateTime.now());
        parametreRepository.save(param);
    }

    // ===================================================================
    // SEGMENTATION RFM
    // ===================================================================

    /**
     * Classifie chaque client en 6 segments RFM.
     * Recency  (R): jours depuis le dernier achat (score 1-5, 5 = le plus récent)
     * Frequency(F): nombre d'achats sur 90 jours   (score 1-5)
     * Monetary (M): montant total sur 90 jours      (score 1-5)
     */
    private Map<String, Integer> segmenterClients() {
        Map<String, Integer> segments = new LinkedHashMap<>();
        segments.put("CHAMPIONS", 0);
        segments.put("LOYAUX", 0);
        segments.put("POTENTIELS", 0);
        segments.put("A_RISQUE", 0);
        segments.put("ENDORMIS", 0);
        segments.put("NOUVEAUX", 0);

        try {
            LocalDateTime debut90j  = LocalDateTime.now().minusDays(90);
            LocalDateTime debut30j  = LocalDateTime.now().minusDays(30);

            List<Object[]> rfmData      = venteRepository.findRFMData(debut90j);
            List<Object[]> premiersAchats = venteRepository.findPremierAchatParClient();

            if (rfmData.isEmpty()) return segments;

            // Map clientId → date du premier achat
            Map<Long, LocalDateTime> premierAchatMap = new HashMap<>();
            for (Object[] row : premiersAchats) {
                if (row[0] == null || row[1] == null) continue;
                Long cid = ((Number) row[0]).longValue();
                premierAchatMap.put(cid, toLocalDateTime(row[1]));
            }

            // Trouver les valeurs max pour normaliser les scores F et M
            long maxFrequence  = rfmData.stream()
                    .mapToLong(r -> r[2] != null ? ((Number) r[2]).longValue() : 0)
                    .max().orElse(1);
            double maxMonetaire = rfmData.stream()
                    .mapToDouble(r -> r[3] != null ? ((Number) r[3]).doubleValue() : 0)
                    .max().orElse(1);
            if (maxFrequence  < 1) maxFrequence  = 1;
            if (maxMonetaire  < 1) maxMonetaire  = 1;

            for (Object[] row : rfmData) {
                if (row[0] == null) continue;
                Long clientId = ((Number) row[0]).longValue();
                if (row[1] == null) continue;

                LocalDateTime dernierAchat = toLocalDateTime(row[1]);
                long frequence   = row[2] != null ? ((Number) row[2]).longValue()   : 0;
                double monetaire = row[3] != null ? ((Number) row[3]).doubleValue() : 0;

                long joursDepuis = ChronoUnit.DAYS.between(dernierAchat, LocalDateTime.now());

                // Score Recency (5 = achat très récent)
                int scoreR;
                if      (joursDepuis <=  7) scoreR = 5;
                else if (joursDepuis <= 14) scoreR = 4;
                else if (joursDepuis <= 30) scoreR = 3;
                else if (joursDepuis <= 60) scoreR = 2;
                else                        scoreR = 1;

                // Score Frequency (1-5, normalisé)
                int scoreF = normaliserScore(frequence, maxFrequence);

                // Score Monetary (1-5, normalisé)
                int scoreM = normaliserScore((long) monetaire, (long) maxMonetaire);

                // Nouveau client : premier achat dans les 30 derniers jours
                LocalDateTime premiere = premierAchatMap.get(clientId);
                boolean estNouveau = premiere != null && premiere.isAfter(debut30j);

                String segment;
                if (estNouveau) {
                    segment = "NOUVEAUX";
                } else if (scoreR >= 4 && scoreF >= 4 && scoreM >= 4) {
                    segment = "CHAMPIONS";
                } else if (scoreF >= 3 && scoreM >= 3) {
                    segment = "LOYAUX";
                } else if (scoreR >= 3 && scoreF <= 2) {
                    segment = "POTENTIELS";
                } else if (scoreR <= 2 && scoreF >= 3) {
                    segment = "A_RISQUE";
                } else {
                    segment = "ENDORMIS";
                }

                segments.merge(segment, 1, Integer::sum);
            }
        } catch (Exception e) {
            // Stacktrace complète conservée (pas seulement le message) pour pouvoir
            // diagnostiquer une régression future sans avoir à retirer ce try/catch.
            log.warn("Erreur lors de la segmentation RFM (segments retournés vides): {}", e.getMessage(), e);
        }

        return segments;
    }

    // ===================================================================
    // SCORE DE SANTÉ BOUTIQUE (0-100)
    // ===================================================================

    /**
     * Pondération : Trésorerie 30% | Stock 25% | Ventes 25% | Crédits 20%
     */
    private int calculerScoreGlobal(List<Double> valeurs60j, double[] coeffs) {
        // --- Trésorerie (30%) ---
        double soldeCaisse = 0;
        try {
            Double solde = caisseRepository.getSoldeTotalCaisse();
            soldeCaisse = solde != null ? solde : 0;
        } catch (Exception e) {
            log.debug("Impossible de lire le solde caisse: {}", e.getMessage());
        }

        double moyenneJournaliere = valeurs60j.stream()
                .mapToDouble(Double::doubleValue).average().orElse(1);
        if (moyenneJournaliere <= 0) moyenneJournaliere = 1;

        // Nombre de jours de CA couverts par la trésorerie (objectif = 30 jours)
        double joursEnCaisse = soldeCaisse / moyenneJournaliere;
        int scoreTresorerie = (int) Math.min(100, Math.max(0, (joursEnCaisse / 30.0) * 100));

        // --- Stock (25%) ---
        long totalProduits       = produitRepository.count();
        long produitsStockFaible = produitRepository.compterProduitsStockFaible();
        int scoreStock;
        if (totalProduits == 0) {
            scoreStock = 50;
        } else {
            double tauxOK = 1.0 - ((double) produitsStockFaible / totalProduits);
            scoreStock = (int) (tauxOK * 100);
        }

        // --- Ventes (25%) ---
        // Une pente positive = bonne santé commerciale
        double slope = coeffs[0];
        double slopeRatio = moyenneJournaliere != 0 ? slope / moyenneJournaliere : 0;
        int scoreVentes;
        if      (slopeRatio >=  0.05) scoreVentes = 100;
        else if (slopeRatio >=  0.01) scoreVentes = 75;
        else if (slopeRatio >= -0.01) scoreVentes = 50;
        else if (slopeRatio >= -0.05) scoreVentes = 25;
        else                          scoreVentes = 10;

        // --- Crédits (20%) ---
        List<Vente> tousCredits = venteRepository.findAllCredits();
        int scoreCredits;
        if (tousCredits.isEmpty()) {
            scoreCredits = 100; // pas de crédit = pas de risque
        } else {
            long nbRegles = tousCredits.stream()
                    .filter(v -> Boolean.TRUE.equals(v.getCreditRegle()))
                    .count();
            scoreCredits = (int) ((double) nbRegles / tousCredits.size() * 100);
        }

        int scoreGlobal = (int) (
                scoreTresorerie * 0.30 +
                scoreStock      * 0.25 +
                scoreVentes     * 0.25 +
                scoreCredits    * 0.20
        );
        return Math.min(100, Math.max(0, scoreGlobal));
    }

    // ===================================================================
    // GÉNÉRATION DES RECOMMANDATIONS
    // ===================================================================

    private List<RecommandationIA> genererRecommandations(
            ProfilIA profil,
            List<Double> valeurs60j,
            double[] coeffs,
            Map<String, Integer> segments) {

        List<RecommandationIA> recs = new ArrayList<>();

        List<Produit> produitsRupture = produitRepository.trouverProduitsEnRupture();
        List<Produit> produitsAlertes = produitRepository.trouverProduitsStockFaible();

        // 1. Ruptures de stock — CRITIQUE
        for (Produit p : produitsRupture) {
            recs.add(buildRec(
                    "REAPPRO", "CRITIQUE",
                    "Rupture de stock: " + p.getNom(),
                    "Le produit " + p.getNom() + " est en rupture de stock (0 unité). Commande urgente requise.",
                    "Commander maintenant",
                    Map.of(
                        "produitId",       p.getId(),
                        "produitNom",      p.getNom(),
                        "quantiteActuelle", p.getQuantite(),
                        "seuilAlerte",     p.getSeuilAlerte()
                    ),
                    calculerConfiance("REAPPRO", 92)
            ));
        }

        // 2. Stock faible (hors rupture) — HAUTE
        Set<Long> rupturIds = produitsRupture.stream().map(Produit::getId).collect(Collectors.toSet());
        for (Produit p : produitsAlertes) {
            if (rupturIds.contains(p.getId())) continue;
            recs.add(buildRec(
                    "REAPPRO", "HAUTE",
                    "Stock faible: " + p.getNom(),
                    "Le stock de " + p.getNom() + " (" + p.getQuantite() + " unités) est sous le seuil d'alerte (" + p.getSeuilAlerte() + ").",
                    "Planifier commande",
                    Map.of(
                        "produitId",       p.getId(),
                        "produitNom",      p.getNom(),
                        "quantiteActuelle", p.getQuantite(),
                        "seuilAlerte",     p.getSeuilAlerte()
                    ),
                    calculerConfiance("REAPPRO", 75)
            ));
        }

        // 3. Clients à risque (RFM)
        int clientsARisque = segments.getOrDefault("A_RISQUE", 0);
        if (clientsARisque > 0) {
            recs.add(buildRec(
                    "CONTACT_CLIENT", "HAUTE",
                    clientsARisque + " client(s) à risque de churn",
                    "Ces clients achetaient régulièrement mais n'ont plus acheté depuis " +
                    (int) getParametre("seuil_client_risque", 21) + " jours. Une relance peut les fidéliser.",
                    "Contacter",
                    Map.of("nombreClients", clientsARisque, "segment", "A_RISQUE"),
                    calculerConfiance("CONTACT_CLIENT", 70)
            ));
        }

        // 4. Crédits en retard
        List<Vente> creditsRetard = venteRepository.findCreditsEnRetard();
        if (!creditsRetard.isEmpty()) {
            double totalRetard = creditsRetard.stream()
                    .mapToDouble(v -> v.getMontantRestant() != null ? v.getMontantRestant() : 0)
                    .sum();
            recs.add(buildRec(
                    "CONTACT_CLIENT", "CRITIQUE",
                    creditsRetard.size() + " crédit(s) en retard de paiement",
                    "Des crédits ont dépassé leur date d'échéance. Montant total à recouvrer: " +
                    String.format("%.0f", totalRetard) + ".",
                    "Voir les crédits",
                    Map.of("nombreCredits", creditsRetard.size(), "montantTotal", totalRetard),
                    calculerConfiance("CONTACT_CLIENT", 95)
            ));
        }

        // 5. Produits sous-performants → suggestion promo
        try {
            LocalDateTime debut30j = LocalDateTime.now().minusDays(30);
            List<Object[]> velocite = venteRepository.findVelociteProduits(debut30j);

            if (velocite.size() > 1) {
                double moyVelocite = velocite.stream()
                        .mapToLong(r -> r[1] != null ? ((Number) r[1]).longValue() : 0)
                        .average().orElse(0);

                List<Map<String, Object>> sousPerformants = new ArrayList<>();
                for (Object[] row : velocite) {
                    if (row[0] == null) continue;
                    long qte = row[1] != null ? ((Number) row[1]).longValue() : 0;
                    if (qte < moyVelocite * 0.20 && sousPerformants.size() < 5) {
                        Long produitId = ((Number) row[0]).longValue();
                        final long qteFinale = qte;
                        produitRepository.findById(produitId).ifPresent(p -> {
                            Map<String, Object> info = new HashMap<>();
                            info.put("produitId",    p.getId());
                            info.put("produitNom",   p.getNom());
                            info.put("quantiteVendue30j", qteFinale);
                            sousPerformants.add(info);
                        });
                    }
                }

                if (!sousPerformants.isEmpty()) {
                    recs.add(buildRec(
                            "PRIX", "MOYENNE",
                            sousPerformants.size() + " produit(s) sous-performant(s)",
                            "Ces produits se vendent nettement moins que la moyenne. " +
                            "Envisagez une promotion ou un réajustement de prix.",
                            "Créer promotion",
                            Map.of("produits", sousPerformants),
                            calculerConfiance("PRIX", 65)
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Erreur analyse vélocité produits: {}", e.getMessage());
        }

        // 6. Planning approvisionnement (basé sur profil.joursApprovisionnement)
        String joursJson = profil.getJoursApprovisionnement();
        if (joursJson != null && !joursJson.isBlank()) {
            for (int i = 1; i <= 2; i++) {
                LocalDate prochainJour = LocalDate.now().plusDays(i);
                String nomFr = JOURS_FR.getOrDefault(prochainJour.getDayOfWeek(), "");
                if (joursJson.contains(nomFr)) {
                    recs.add(buildRec(
                            "PLANNING", "MOYENNE",
                            "Jour d'approvisionnement dans " + i + " jour(s)",
                            "Préparez votre liste de commandes pour " + nomFr + ". " +
                            produitsAlertes.size() + " produit(s) à réapprovisionner.",
                            "Préparer commande",
                            Map.of(
                                "jour",              nomFr,
                                "joursAvant",        i,
                                "nbProduitsAReappro", produitsAlertes.size()
                            ),
                            calculerConfiance("PLANNING", 80)
                    ));
                    break;
                }
            }
        }

        // Trier par scoreConfiance décroissant
        return recs.stream()
                .sorted(Comparator.comparingDouble(RecommandationIA::getScoreConfiance).reversed())
                .collect(Collectors.toList());
    }

    // ===================================================================
    // GÉNÉRATION DES ALERTES
    // ===================================================================

    private List<AlerteIA> genererAlertes() {
        List<AlerteIA> alertes = new ArrayList<>();

        // Ruptures de stock
        List<Produit> ruptures = produitRepository.trouverProduitsEnRupture();
        if (!ruptures.isEmpty()) {
            alertes.add(new AlerteIA(
                    "STOCK_RUPTURE", "CRITIQUE",
                    ruptures.size() + " produit(s) en rupture de stock",
                    Map.of(
                        "nombreProduits", ruptures.size(),
                        "produits", ruptures.stream().map(Produit::getNom).limit(5).collect(Collectors.toList())
                    )
            ));
        }

        // Produits à péremption imminente (< 7 jours)
        try {
            List<Produit> peremption = produitRepository.trouverProduitsProchePeremption(LocalDate.now().plusDays(7));
            if (!peremption.isEmpty()) {
                alertes.add(new AlerteIA(
                        "PEREMPTION", "ATTENTION",
                        peremption.size() + " produit(s) à péremption dans moins de 7 jours",
                        Map.of(
                            "nombreProduits", peremption.size(),
                            "produits", peremption.stream().map(Produit::getNom).limit(5).collect(Collectors.toList())
                        )
                ));
            }
        } catch (Exception e) {
            log.debug("Vérification péremption: {}", e.getMessage());
        }

        // Crédits en retard
        List<Vente> creditsRetard = venteRepository.findCreditsEnRetard();
        if (!creditsRetard.isEmpty()) {
            double totalRetard = creditsRetard.stream()
                    .mapToDouble(v -> v.getMontantRestant() != null ? v.getMontantRestant() : 0)
                    .sum();
            alertes.add(new AlerteIA(
                    "CREDIT_RETARD", "CRITIQUE",
                    creditsRetard.size() + " crédit(s) en retard (total: " + String.format("%.2f", totalRetard) + ")",
                    Map.of("nombreCredits", creditsRetard.size(), "montantTotal", totalRetard)
            ));
        }

        return alertes;
    }

    // ===================================================================
    // AUTO-APPRENTISSAGE
    // ===================================================================

    /**
     * Recalcule les paramètres du modèle en fonction des feedbacks reçus.
     * Appelé après chaque feedback pour adapter les seuils progressivement.
     */
    @Transactional
    public void mettreAJourModele() {
        String[] types = {"REAPPRO", "CONTACT_CLIENT", "PRIX", "PLANNING", "STOCK"};

        for (String type : types) {
            List<FeedbackRecommandation> suivies  = feedbackRepository.findByTypeRecommandationAndStatut(type, StatutFeedback.SUIVIE);
            List<FeedbackRecommandation> ignorees = feedbackRepository.findByTypeRecommandationAndStatut(type, StatutFeedback.IGNOREE);
            int total = suivies.size() + ignorees.size();

            if (total < 3) continue; // pas assez de signal pour ajuster

            double tauxSuivi = (double) suivies.size() / total;

            // Ajuster le seuil d'alerte stock si type REAPPRO
            if ("REAPPRO".equals(type)) {
                ParametreModeleIA param = parametreRepository.findById("seuil_alerte_stock")
                        .orElseGet(() -> new ParametreModeleIA("seuil_alerte_stock", 0.30));

                double seuilActuel = param.getValeur();
                if (tauxSuivi < 0.40) {
                    // Trop peu suivies → recommandations peu pertinentes → augmenter seuil (être plus sélectif)
                    param.setValeur(Math.min(0.80, seuilActuel + 0.05));
                } else if (tauxSuivi > 0.80) {
                    // Très bien suivies → on peut être plus agressif
                    param.setValeur(Math.max(0.10, seuilActuel - 0.05));
                }
                param.setPrecisionModele(tauxSuivi);
                param.setVersion(param.getVersion() + 1);
                param.setDateMiseAJour(LocalDateTime.now());
                parametreRepository.save(param);
                log.info("Paramètre seuil_alerte_stock ajusté: {}", param.getValeur());
            }

            // Ajuster seuil client à risque si type CONTACT_CLIENT
            if ("CONTACT_CLIENT".equals(type)) {
                ParametreModeleIA param = parametreRepository.findById("seuil_client_risque")
                        .orElseGet(() -> new ParametreModeleIA("seuil_client_risque", 21.0));

                double seuil = param.getValeur();
                if (tauxSuivi < 0.40) {
                    // Peu pertinentes → augmenter seuil (seulement les vrais cas)
                    param.setValeur(Math.min(60, seuil + 3));
                } else if (tauxSuivi > 0.80) {
                    // Très bien suivies → diminuer le seuil
                    param.setValeur(Math.max(7, seuil - 3));
                }
                param.setPrecisionModele(tauxSuivi);
                param.setVersion(param.getVersion() + 1);
                param.setDateMiseAJour(LocalDateTime.now());
                parametreRepository.save(param);
            }
        }

        // Ajuster alpha EMA si la précision actuelle est faible
        parametreRepository.findById("alpha_ema").ifPresent(alpha -> {
            double precisionActuelle = alpha.getPrecisionModele();
            if (precisionActuelle > 0 && precisionActuelle < 0.60) {
                // Mauvaises prévisions → rendre l'EMA plus réactif aux données récentes
                double nouvelAlpha = Math.min(0.70, alpha.getValeur() + 0.05);
                alpha.setValeur(nouvelAlpha);
                alpha.setVersion(alpha.getVersion() + 1);
                alpha.setDateMiseAJour(LocalDateTime.now());
                parametreRepository.save(alpha);
                log.info("Alpha EMA ajusté: {} (précision modèle: {})", nouvelAlpha, precisionActuelle);
            }
        });
    }

    // ===================================================================
    // MÉTHODE PRINCIPALE
    // ===================================================================

    @Transactional(readOnly = true)
    public AnalyseIAResult analyser(ProfilIA profil) {
        LocalDateTime fin       = LocalDateTime.now();
        LocalDateTime debut60j  = fin.minusDays(60);

        // 1. CA par jour sur 60 jours (données brutes)
        List<Object[]> caParJourData = venteRepository.findCAParJour(debut60j, fin);

        // Construire une série temporelle complète de 60 jours (0 si aucune vente ce jour)
        Map<String, Double> caParDate = new LinkedHashMap<>();
        for (Object[] row : caParJourData) {
            if (row[0] != null && row[1] != null) {
                caParDate.put(row[0].toString(), ((Number) row[1]).doubleValue());
            }
        }

        List<Double> valeurs60j = new ArrayList<>(60);
        LocalDate dateDebut = LocalDate.now().minusDays(59);
        for (int i = 0; i < 60; i++) {
            valeurs60j.add(caParDate.getOrDefault(dateDebut.plusDays(i).toString(), 0.0));
        }

        // 2. Régression linéaire + EMA
        double[] coeffs   = regressionLineaire(valeurs60j);
        double   alpha    = getParametre("alpha_ema", 0.3);
        double   emaValue = ema(valeurs60j, alpha);

        // Prévisions combinées (70% régression + 30% EMA pour plus de robustesse)
        double prevision7j = 0;
        for (int i = 60; i < 67; i++) {
            prevision7j += Math.max(0, coeffs[0] * i + coeffs[1]);
        }
        prevision7j = prevision7j * 0.70 + emaValue * 7 * 0.30;

        double prevision30j = 0;
        for (int i = 60; i < 90; i++) {
            prevision30j += Math.max(0, coeffs[0] * i + coeffs[1]);
        }
        prevision30j = prevision30j * 0.70 + emaValue * 30 * 0.30;

        // 3. Segmentation RFM
        log.debug("Analyse IA - étape 3/6: segmentation RFM");
        Map<String, Integer> segments = segmenterClients();

        // 4. Score santé global
        log.debug("Analyse IA - étape 4/6: score santé global");
        int scoreGlobal = calculerScoreGlobal(valeurs60j, coeffs);

        // 5. Recommandations personnalisées
        log.debug("Analyse IA - étape 5/6: recommandations");
        List<RecommandationIA> recommandations = genererRecommandations(profil, valeurs60j, coeffs, segments);

        // 6. Alertes
        log.debug("Analyse IA - étape 6/6: alertes");
        List<AlerteIA> alertes = genererAlertes();

        // 7. Prévision détaillée 30 jours suivants
        List<Map<String, Object>> previsionDetail = new ArrayList<>(30);
        for (int i = 0; i < 30; i++) {
            double predRegression = Math.max(0, coeffs[0] * (60 + i) + coeffs[1]);
            double predCombinee   = predRegression * 0.70 + emaValue * 0.30;
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("date",      LocalDate.now().plusDays(i + 1).toString());
            pt.put("prevision", arrondir(predCombinee));
            previsionDetail.add(pt);
        }

        // 8. Précision du modèle
        double precision = calculerPrecisionModele(caParJourData, coeffs);

        // Mettre à jour la précision EMA dans les paramètres
        sauvegarderParametresPrecision(precision, alpha);

        // CA référence
        LocalDateTime debutHier    = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime finHier      = LocalDate.now().atStartOfDay().minusSeconds(1);
        LocalDateTime debutSemaine = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime debutMois    = LocalDate.now().minusDays(30).atStartOfDay();

        Double caHier    = venteRepository.getCAByDateRange(debutHier, finHier);
        Double caSemaine = venteRepository.getCAByDateRange(debutSemaine, fin);
        Double caMois    = venteRepository.getCAByDateRange(debutMois, fin);

        double moyenneCA   = valeurs60j.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        String tendanceCA  = calculerTendanceCA(coeffs, moyenneCA);
        double tauxCroissance = moyenneCA != 0 ? (coeffs[0] / moyenneCA) * 100 : 0;

        AnalyseIAResult result = new AnalyseIAResult();
        result.setScoreGlobal(scoreGlobal);
        result.setTendanceCA(tendanceCA);
        result.setTauxCroissanceMensuel(arrondir(tauxCroissance));
        result.setPrevisionCA7Jours(arrondir(prevision7j));
        result.setPrevisionCA30Jours(arrondir(prevision30j));
        result.setCaHier(caHier != null ? caHier : 0);
        result.setCaCetteSemaine(caSemaine != null ? caSemaine : 0);
        result.setCaCeMois(caMois != null ? caMois : 0);
        result.setRecommandations(recommandations);
        result.setAlertes(alertes);
        result.setSegmentsClients(segments);
        result.setPrevisionCA30JoursDetail(previsionDetail);
        result.setPrecisionModele(arrondir(precision));

        return result;
    }

    // ===================================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ===================================================================

    /** Convertit le score de confiance en tenant compte de l'historique de feedback. */
    private double calculerConfiance(String typeRec, double baseConfiance) {
        try {
            List<FeedbackRecommandation> suivies  = feedbackRepository.findByTypeRecommandationAndStatut(typeRec, StatutFeedback.SUIVIE);
            List<FeedbackRecommandation> ignorees = feedbackRepository.findByTypeRecommandationAndStatut(typeRec, StatutFeedback.IGNOREE);
            int total = suivies.size() + ignorees.size();
            if (total < 3) return baseConfiance;
            double tauxSuiviPct = (double) suivies.size() / total * 100;
            // Moyenne entre la confiance de base et le taux historique de suivi
            return Math.round((baseConfiance + tauxSuiviPct) / 2.0 * 10) / 10.0;
        } catch (Exception e) {
            return baseConfiance;
        }
    }

    private String calculerTendanceCA(double[] coeffs, double moyenneCA) {
        if (moyenneCA <= 0) return "STABLE";
        double slopeRatio = coeffs[0] / moyenneCA;
        if      (slopeRatio >=  0.05) return "FORTE_HAUSSE";
        else if (slopeRatio >=  0.01) return "HAUSSE";
        else if (slopeRatio >= -0.01) return "STABLE";
        else if (slopeRatio >= -0.05) return "BAISSE";
        else                          return "FORTE_BAISSE";
    }

    /**
     * MAPE (Mean Absolute Percentage Error) inversé : precision = (1 - MAPE) * 100.
     * Utilise les 10 derniers jours de données réelles pour valider la régression.
     */
    private double calculerPrecisionModele(List<Object[]> caParJourData, double[] coeffs) {
        if (caParJourData.size() < 5) return 50.0;

        int n      = caParJourData.size();
        int debut  = Math.max(0, n - 10);
        double totalErreurRelative = 0;
        int    count = 0;

        for (int i = debut; i < n; i++) {
            Object[] row  = caParJourData.get(i);
            double   reel = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
            double   pred = Math.max(0, coeffs[0] * i + coeffs[1]);
            if (reel > 0) {
                totalErreurRelative += Math.abs(reel - pred) / reel;
                count++;
            }
        }

        if (count == 0) return 50.0;
        double mape = totalErreurRelative / count;
        return Math.max(0, Math.min(100, (1 - mape) * 100));
    }

    /** Sauvegarde la précision courante dans le paramètre alpha_ema (sans modifier la valeur). */
    private void sauvegarderParametresPrecision(double precision, double alpha) {
        parametreRepository.findById("alpha_ema").ifPresent(p -> {
            p.setPrecisionModele(precision / 100.0);
            p.setDateMiseAJour(LocalDateTime.now());
            parametreRepository.save(p);
        });
    }

    /** Construit un objet RecommandationIA de façon concise. */
    private RecommandationIA buildRec(String type, String priorite, String titre,
                                       String desc, String actionLabel,
                                       Map<String, Object> donnees, double confiance) {
        RecommandationIA rec = new RecommandationIA();
        rec.setId(UUID.randomUUID().toString());
        rec.setType(type);
        rec.setPriorite(priorite);
        rec.setTitre(titre);
        rec.setDescription(desc);
        rec.setActionLabel(actionLabel);
        rec.setDonnees(donnees);
        rec.setScoreConfiance(confiance);
        return rec;
    }

    /** Normalise une valeur longue en score 1-5. */
    private int normaliserScore(long valeur, long max) {
        if (max <= 0) return 1;
        int score = (int) Math.ceil((double) valeur / max * 5);
        return Math.min(5, Math.max(1, score));
    }

    /** Arrondit à 2 décimales. */
    private double arrondir(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    /** Convertit java.sql.Timestamp ou java.sql.Date en LocalDateTime. */
    private LocalDateTime toLocalDateTime(Object sqlDate) {
        if (sqlDate instanceof Timestamp) {
            return ((Timestamp) sqlDate).toLocalDateTime();
        }
        if (sqlDate instanceof java.sql.Date) {
            return ((java.sql.Date) sqlDate).toLocalDate().atStartOfDay();
        }
        // Fallback si le driver retourne un LocalDateTime directement
        if (sqlDate instanceof LocalDateTime) {
            return (LocalDateTime) sqlDate;
        }
        return LocalDateTime.now();
    }
}
