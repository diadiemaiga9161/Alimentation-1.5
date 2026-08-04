package com.ges.boutique.ia;

import com.ges.boutique.inventaire.MouvementStockRepository;
import com.ges.boutique.produit.Produit;
import com.ges.boutique.produit.ProduitRepository;
import com.ges.boutique.vente.ModePaiement;
import com.ges.boutique.vente.Vente;
import com.ges.boutique.vente.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Assistant IA de la boutique — répond avec des algorithmes locaux basés sur les
 * vraies données (aucune API externe, aucune clé requise).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IAChatService {

    private final VenteRepository venteRepository;
    private final ProduitRepository produitRepository;
    private final MouvementStockRepository mouvementStockRepository;

    private static final List<ModePaiement> MOBILE_MONEY = List.of(ModePaiement.ORANGE_MONEY, ModePaiement.MOOV_MONEY);

    // Correspondance exacte question prédéfinie (texte envoyé par les chips) -> intention
    private static final Map<String, String> PHRASE_TO_INTENT = new LinkedHashMap<>();
    static {
        PHRASE_TO_INTENT.put("j'ai vendu combien aujourd'hui ?", "ventes_jour");
        PHRASE_TO_INTENT.put("quelle est la difference entre mes ventes comptant et credit ce mois ?", "comptant_credit");
        PHRASE_TO_INTENT.put("quel est le total de mes paiements orange money et moov money ?", "mobile_money");
        PHRASE_TO_INTENT.put("quels clients ont encore des dettes non reglees ?", "credits_dus");
        PHRASE_TO_INTENT.put("quels produits sont en alerte de stock ?", "stock_faible");
        PHRASE_TO_INTENT.put("donne-moi des conseils pour ameliorer ma gestion de boutique.", "conseils");
        PHRASE_TO_INTENT.put("fais-moi des propositions concretes pour augmenter mes benefices.", "augmenter_benefices");
        PHRASE_TO_INTENT.put("quels sont mes produits les plus rentables ce mois-ci ?", "produits_rentables");
        PHRASE_TO_INTENT.put("quel est mon bilan pour cette semaine ?", "bilan_semaine");
        PHRASE_TO_INTENT.put("quel est mon bilan pour ce mois ?", "bilan_mois");
        PHRASE_TO_INTENT.put("combien de sorties de stock ce mois-ci ?", "sorties_stock");
        PHRASE_TO_INTENT.put("combien de ventes annulees ce mois-ci ?", "ventes_annulees");
        PHRASE_TO_INTENT.put("montre-moi l'etat de mon stock de produits.", "stock_produits");
        PHRASE_TO_INTENT.put("fais-moi un rapport complet de ma boutique.", "rapport_complet");
        PHRASE_TO_INTENT.put("comment fonctionne ges boutique ?", "comment_ca_marche");
        PHRASE_TO_INTENT.put("qui a fait les ventes ?", "ventes_vendeur");
    }

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");

    public String repondre(String question) {
        try {
            var datesTrouvees = DATE_PATTERN.matcher(question);
            if (datesTrouvees.find()) {
                String premiereDate = datesTrouvees.group(1);
                if (datesTrouvees.find()) {
                    String secondeDate = datesTrouvees.group(1);
                    return periodePersonnalisee(premiereDate, secondeDate);
                }
            }

            String normalisee = normaliser(question);
            String intent = PHRASE_TO_INTENT.get(normalisee);
            if (intent == null) {
                intent = detecterIntentionParMotsCles(normalisee);
            }
            return traiter(intent);
        } catch (Exception e) {
            log.error("Erreur assistant IA local: {}", e.getMessage(), e);
            return "Une erreur s'est produite pendant le calcul. Réessaie dans un instant.";
        }
    }

    // =========================================================
    // Détection par mots-clés (question libre, sans accents)
    // =========================================================

    private String detecterIntentionParMotsCles(String q) {
        boolean parleAujourdhui = q.contains("aujourd'hui") || q.contains("aujourdhui");
        boolean parleSemaine = q.contains("semaine");
        boolean parleMois = q.contains("mois");

        if (q.contains("comment") && (q.contains("fonctionne") || q.contains("marche") || q.contains("utilise"))) {
            return "comment_ca_marche";
        }
        if (q.contains("rapport") && q.contains("complet")) return "rapport_complet";
        if (q.contains("sortie")) return "sorties_stock";
        if ((q.contains("annule") || q.contains("annulation")) && q.contains("vente")) return "ventes_annulees";
        if (q.contains("stock") && (q.contains("faible") || q.contains("alerte") || q.contains("rupture"))) return "stock_faible";
        if (q.contains("stock")) return "stock_produits";
        if (q.contains("comptant") && q.contains("credit")) return "comptant_credit";
        if (q.contains("orange") || q.contains("moov") || q.contains("mobile money")) return "mobile_money";
        if (q.contains("dette") || q.contains("credit") && (q.contains("client") || q.contains("du") || q.contains("regle"))) return "credits_dus";
        if (q.contains("rentable") || q.contains("profitable")) return "produits_rentables";
        if (q.contains("augmenter") && (q.contains("benefice") || q.contains("marge") || q.contains("profit"))) return "augmenter_benefices";
        if (q.contains("conseil")) return "conseils";
        if (q.contains("vendeur") || (q.contains("qui") && (q.contains("vente") || q.contains("vendu")))) return "ventes_vendeur";
        if (parleSemaine && (q.contains("bilan") || q.contains("resume"))) return "bilan_semaine";
        if (parleMois && (q.contains("bilan") || q.contains("resume"))) return "bilan_mois";
        if (parleAujourdhui && (q.contains("vendu") || q.contains("vente"))) return "ventes_jour";

        return "inconnu";
    }

    private String normaliser(String texte) {
        String sansAccents = Normalizer.normalize(texte.trim().toLowerCase(Locale.FRENCH), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.replaceAll("\\s+", " ");
    }

    // =========================================================
    // Routage vers les algorithmes
    // =========================================================

    private String traiter(String intent) {
        return switch (intent) {
            case "ventes_jour" -> ventesJour();
            case "comptant_credit" -> comptantCredit();
            case "mobile_money" -> mobileMoney();
            case "credits_dus" -> creditsDus();
            case "stock_faible" -> stockFaible();
            case "conseils" -> conseils();
            case "augmenter_benefices" -> augmenterBenefices();
            case "produits_rentables" -> produitsRentables();
            case "bilan_semaine" -> bilan(debutSemaine(), finSemaine(), "cette semaine");
            case "bilan_mois" -> bilan(debutMois(), finMois(), "ce mois");
            case "sorties_stock" -> sortiesStock();
            case "ventes_annulees" -> ventesAnnulees();
            case "stock_produits" -> stockProduits();
            case "rapport_complet" -> rapportComplet();
            case "comment_ca_marche" -> COMMENT_CA_MARCHE;
            case "ventes_vendeur" -> ventesParVendeur(debutMois(), finMois(), "ce mois");
            default -> reponseParDefaut();
        };
    }

    // =========================================================
    // Bornes de dates
    // =========================================================

    private LocalDateTime debutJour() { return LocalDate.now().atStartOfDay(); }
    private LocalDateTime finJour() { return LocalDate.now().atTime(LocalTime.MAX); }
    private LocalDateTime debutSemaine() { return LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(); }
    private LocalDateTime finSemaine() { return LocalDate.now().with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX); }
    private LocalDateTime debutMois() { return LocalDate.now().withDayOfMonth(1).atStartOfDay(); }
    private LocalDateTime finMois() { return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX); }

    // =========================================================
    // Algorithmes de réponse
    // =========================================================

    private String ventesJour() {
        long nb = orZero(venteRepository.countByDateRange(debutJour(), finJour()));
        double ca = orZero(venteRepository.getCAByDateRange(debutJour(), finJour()));
        double benefice = orZero(venteRepository.getBeneficeTotalByDateRange(debutJour(), finJour()));
        long comptant = orZero(venteRepository.countVentesComptantByDateRange(debutJour(), finJour()));
        long credit = orZero(venteRepository.countVentesCreditByDateRange(debutJour(), finJour()));

        StringBuilder r = new StringBuilder();
        r.append("📊 Aujourd'hui (").append(LocalDate.now()).append(") :\n\n");
        r.append("• Nombre de ventes : ").append(nb).append("\n");
        r.append("• Chiffre d'affaires : ").append(fcfa(ca)).append("\n");
        r.append("• Bénéfice réalisé : ").append(fcfa(benefice)).append("\n");
        r.append("• Comptant : ").append(comptant).append(" — Crédit : ").append(credit);
        return r.toString();
    }

    private String comptantCredit() {
        long comptant = orZero(venteRepository.countVentesComptantByDateRange(debutMois(), finMois()));
        long credit = orZero(venteRepository.countVentesCreditByDateRange(debutMois(), finMois()));
        long total = comptant + credit;
        double partComptant = total > 0 ? (comptant * 100.0 / total) : 0;
        double partCredit = total > 0 ? (credit * 100.0 / total) : 0;

        StringBuilder r = new StringBuilder();
        r.append("💳 Comptant vs Crédit — ce mois :\n\n");
        r.append("• Ventes comptant : ").append(comptant).append(" (").append(Math.round(partComptant)).append("%)\n");
        r.append("• Ventes crédit : ").append(credit).append(" (").append(Math.round(partCredit)).append("%)\n\n");
        if (partCredit > 50) {
            r.append("⚠️ Plus de la moitié de tes ventes sont à crédit. Pense à relancer les clients pour récupérer ta trésorerie.");
        } else {
            r.append("✅ Ta part de ventes comptant est saine, ça sécurise ta trésorerie.");
        }
        return r.toString();
    }

    private String mobileMoney() {
        double orangeJour = orZero(venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.ORANGE_MONEY, debutJour(), finJour()));
        double moovJour = orZero(venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.MOOV_MONEY, debutJour(), finJour()));
        double orangeMois = orZero(venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.ORANGE_MONEY, debutMois(), finMois()));
        double moovMois = orZero(venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.MOOV_MONEY, debutMois(), finMois()));

        StringBuilder r = new StringBuilder();
        r.append("📱 Mobile Money :\n\n");
        r.append("Aujourd'hui — Orange : ").append(fcfa(orangeJour)).append(" | Moov : ").append(fcfa(moovJour)).append("\n");
        r.append("Ce mois — Orange : ").append(fcfa(orangeMois)).append(" | Moov : ").append(fcfa(moovMois));
        return r.toString();
    }

    private String creditsDus() {
        List<Vente> credits = venteRepository.findCreditsNonRegles();
        double total = orZero(venteRepository.getTotalCreditsNonRegles());

        StringBuilder r = new StringBuilder();
        r.append("💰 Crédits non réglés :\n\n");
        r.append("• Nombre de ventes à crédit dues : ").append(credits.size()).append("\n");
        r.append("• Montant total dû : ").append(fcfa(total)).append("\n");

        if (!credits.isEmpty()) {
            r.append("\nTop clients à relancer :\n");
            credits.stream()
                    .sorted(Comparator.comparingDouble((Vente v) -> orZero(v.getMontantRestant())).reversed())
                    .limit(5)
                    .forEach(v -> r.append("• ").append(v.getClientNom() != null ? v.getClientNom() : "Client divers")
                            .append(" : ").append(fcfa(orZero(v.getMontantRestant()))).append("\n"));
        } else {
            r.append("\n✅ Aucun crédit en attente, bien joué !");
        }
        return r.toString().stripTrailing();
    }

    private String stockFaible() {
        List<Produit> produits = produitRepository.trouverProduitsStockFaible();
        StringBuilder r = new StringBuilder();
        r.append("⚠️ Produits en alerte de stock (").append(produits.size()).append(") :\n\n");
        if (produits.isEmpty()) {
            r.append("✅ Aucun produit en alerte de stock actuellement.");
        } else {
            produits.stream().limit(10).forEach(p ->
                    r.append("• ").append(p.getNom()).append(" : ").append(p.getQuantite())
                            .append(" restant(s) (seuil ").append(p.getSeuilAlerte()).append(")\n"));
            if (produits.size() > 10) r.append("… et ").append(produits.size() - 10).append(" autre(s).");
        }
        return r.toString().stripTrailing();
    }

    private String stockProduits() {
        long total = produitRepository.count();
        long faible = orZero(produitRepository.compterProduitsStockFaible());
        long rupture = produitRepository.trouverProduitsEnRupture().size();
        double valeurStock = orZero(produitRepository.getValeurTotaleStock());

        StringBuilder r = new StringBuilder();
        r.append("📦 État du stock :\n\n");
        r.append("• Produits référencés : ").append(total).append("\n");
        r.append("• En alerte de stock : ").append(faible).append("\n");
        r.append("• En rupture totale : ").append(rupture).append("\n");
        r.append("• Valeur totale du stock (prix d'achat) : ").append(fcfa(valeurStock));
        return r.toString();
    }

    private String sortiesStock() {
        var sorties = mouvementStockRepository.findSorties(null, null, null, debutMois(), finMois());
        int quantiteTotale = sorties.stream().mapToInt(m -> m.getQuantite() != null ? m.getQuantite() : 0).sum();

        Map<String, Integer> parProduit = new LinkedHashMap<>();
        for (var m : sorties) {
            String nom = m.getProduit() != null ? m.getProduit().getNom() : "Inconnu";
            parProduit.merge(nom, m.getQuantite() != null ? m.getQuantite() : 0, Integer::sum);
        }

        StringBuilder r = new StringBuilder();
        r.append("📤 Sorties de stock — ce mois :\n\n");
        r.append("• Nombre de mouvements de sortie : ").append(sorties.size()).append("\n");
        r.append("• Quantité totale sortie : ").append(quantiteTotale).append(" unité(s)\n");

        if (!parProduit.isEmpty()) {
            r.append("\nTop produits sortis :\n");
            parProduit.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(e -> r.append("• ").append(e.getKey()).append(" : ").append(e.getValue()).append(" unité(s)\n"));
        }
        return r.toString().stripTrailing();
    }

    private String ventesAnnulees() {
        List<Vente> annulees = venteRepository.findVentesAnnuleesByDateRange(debutMois(), finMois());
        double montantPerdu = annulees.stream().mapToDouble(v -> orZero(v.getMontantTotal())).sum();

        StringBuilder r = new StringBuilder();
        r.append("🚫 Ventes annulées — ce mois :\n\n");
        r.append("• Nombre : ").append(annulees.size()).append("\n");
        r.append("• Montant concerné : ").append(fcfa(montantPerdu));
        if (annulees.size() > 3) {
            r.append("\n\n⚠️ C'est beaucoup d'annulations ce mois-ci, regarde s'il y a une cause récurrente (erreur de saisie, rupture après-vente...).");
        }
        return r.toString();
    }

    private String produitsRentables() {
        List<Object[]> ventes = venteRepository.findTopProduits(debutMois());
        List<Map.Entry<String, Double>> classement = new ArrayList<>();

        for (Object[] row : ventes) {
            String nom = (String) row[0];
            long quantite = ((Number) row[1]).longValue();
            List<Produit> matches = produitRepository.findByNomIgnoreCase(nom);
            if (matches.isEmpty()) continue;
            Produit p = matches.get(0);
            double margeUnitaire = orZero(p.getPrixVente()) - orZero(p.getPrixAchat());
            classement.add(Map.entry(nom, margeUnitaire * quantite));
        }

        classement.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        StringBuilder r = new StringBuilder();
        r.append("🏆 Produits les plus rentables — ce mois :\n\n");
        if (classement.isEmpty()) {
            r.append("Pas encore assez de ventes ce mois-ci pour établir un classement.");
        } else {
            int rang = 1;
            for (var e : classement.subList(0, Math.min(5, classement.size()))) {
                r.append(rang++).append(". ").append(e.getKey()).append(" : bénéfice estimé ").append(fcfa(e.getValue())).append("\n");
            }
        }
        return r.toString().stripTrailing();
    }

    private String conseils() {
        List<String> conseils = new ArrayList<>();
        long stockFaible = orZero(produitRepository.compterProduitsStockFaible());
        double creditsDus = orZero(venteRepository.getTotalCreditsNonRegles());
        long ventesCreditMois = orZero(venteRepository.countVentesCreditByDateRange(debutMois(), finMois()));
        long ventesComptantMois = orZero(venteRepository.countVentesComptantByDateRange(debutMois(), finMois()));

        if (stockFaible > 0) {
            conseils.add("📦 Tu as " + stockFaible + " produit(s) en alerte de stock — réapprovisionne-les avant la rupture.");
        }
        if (creditsDus > 0) {
            conseils.add("💰 " + fcfa(creditsDus) + " de crédits sont encore dus — planifie des relances clients cette semaine.");
        }
        long totalMois = ventesCreditMois + ventesComptantMois;
        if (totalMois > 0 && ventesCreditMois * 100.0 / totalMois > 50) {
            conseils.add("⚠️ Plus de la moitié de tes ventes ce mois sont à crédit — essaie d'encourager le paiement comptant (petite remise, mobile money).");
        }
        conseils.add("📊 Consulte régulièrement tes produits les plus rentables pour mieux gérer tes réapprovisionnements.");
        conseils.add("🗂️ Fais un inventaire physique de temps en temps pour vérifier que le stock informatique correspond au stock réel.");

        return "💡 Conseils pour ta boutique :\n\n" + String.join("\n\n", conseils);
    }

    private String augmenterBenefices() {
        double beneficeMois = orZero(venteRepository.getBeneficeTotalByDateRange(debutMois(), finMois()));
        double caMois = orZero(venteRepository.getCAByDateRange(debutMois(), finMois()));
        double margeMoyenne = caMois > 0 ? (beneficeMois * 100.0 / caMois) : 0;

        StringBuilder r = new StringBuilder();
        r.append("🚀 Propositions pour augmenter tes bénéfices :\n\n");
        r.append("Ta marge moyenne ce mois est d'environ ").append(Math.round(margeMoyenne)).append("%.\n\n");
        r.append("1. Mets en avant tes produits les plus rentables (demande-moi \"produits les plus rentables\").\n");
        r.append("2. Réduis les ventes à crédit non réglées : elles bloquent ta trésorerie sans générer de bénéfice immédiat.\n");
        r.append("3. Évite les ruptures de stock sur les produits qui se vendent bien — une vente ratée est un bénéfice perdu.\n");
        r.append("4. Surveille les annulations de ventes : chaque annulation est un bénéfice qui disparaît.\n");
        r.append("5. Négocie de meilleurs prix d'achat auprès de tes fournisseurs sur les produits à fort volume.");
        return r.toString();
    }

    private String bilan(LocalDateTime debut, LocalDateTime fin, String periode) {
        long nb = orZero(venteRepository.countByDateRange(debut, fin));
        double ca = orZero(venteRepository.getCAByDateRange(debut, fin));
        double benefice = orZero(venteRepository.getBeneficeTotalByDateRange(debut, fin));
        long comptant = orZero(venteRepository.countVentesComptantByDateRange(debut, fin));
        long credit = orZero(venteRepository.countVentesCreditByDateRange(debut, fin));

        StringBuilder r = new StringBuilder();
        r.append("🗓️ Bilan pour ").append(periode).append(" :\n\n");
        r.append("• Ventes : ").append(nb).append(" (comptant ").append(comptant).append(" / crédit ").append(credit).append(")\n");
        r.append("• Chiffre d'affaires : ").append(fcfa(ca)).append("\n");
        r.append("• Bénéfice : ").append(fcfa(benefice));
        return r.toString();
    }

    private String ventesParVendeur(LocalDateTime debut, LocalDateTime fin, String periode) {
        List<Object[]> lignes = venteRepository.findVentesParVendeur(debut, fin);

        StringBuilder r = new StringBuilder();
        r.append("🧑‍💼 Ventes par vendeur — ").append(periode).append(" :\n\n");
        if (lignes.isEmpty()) {
            r.append("Aucune vente enregistrée sur cette période.");
        } else {
            for (Object[] row : lignes) {
                String nom = (String) row[0];
                long nb = ((Number) row[1]).longValue();
                double ca = ((Number) row[2]).doubleValue();
                r.append("• ").append(nom).append(" : ").append(nb).append(" vente(s), ").append(fcfa(ca)).append("\n");
            }
        }
        return r.toString().stripTrailing();
    }

    private String periodePersonnalisee(String dateDebutStr, String dateFinStr) {
        var formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate d1 = LocalDate.parse(dateDebutStr, formatter);
        LocalDate d2 = LocalDate.parse(dateFinStr, formatter);
        LocalDate debut = d1.isBefore(d2) ? d1 : d2;
        LocalDate fin = d1.isBefore(d2) ? d2 : d1;

        String periodeLabel = "du " + debut.format(formatter) + " au " + fin.format(formatter);
        String bilanTexte = bilan(debut.atStartOfDay(), fin.atTime(LocalTime.MAX), periodeLabel);
        String vendeurTexte = ventesParVendeur(debut.atStartOfDay(), fin.atTime(LocalTime.MAX), periodeLabel);
        return bilanTexte + "\n\n" + vendeurTexte;
    }

    private String rapportComplet() {
        long nbJour = orZero(venteRepository.countByDateRange(debutJour(), finJour()));
        double caJour = orZero(venteRepository.getCAByDateRange(debutJour(), finJour()));
        double caMois = orZero(venteRepository.getCAByDateRange(debutMois(), finMois()));
        double beneficeMois = orZero(venteRepository.getBeneficeTotalByDateRange(debutMois(), finMois()));
        long nbMois = orZero(venteRepository.countByDateRange(debutMois(), finMois()));

        double creditsDus = orZero(venteRepository.getTotalCreditsNonRegles());
        int nbCreditsDus = venteRepository.findCreditsNonRegles().size();

        long stockFaible = orZero(produitRepository.compterProduitsStockFaible());
        long rupture = produitRepository.trouverProduitsEnRupture().size();
        double valeurStock = orZero(produitRepository.getValeurTotaleStock());

        var sorties = mouvementStockRepository.findSorties(null, null, null, debutMois(), finMois());
        List<Vente> annulees = venteRepository.findVentesAnnuleesByDateRange(debutMois(), finMois());

        double orangeMois = orZero(venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.ORANGE_MONEY, debutMois(), finMois()));
        double moovMois = orZero(venteRepository.getTotalByModePaiementAndDateRange(ModePaiement.MOOV_MONEY, debutMois(), finMois()));

        StringBuilder r = new StringBuilder();
        r.append("📋 RAPPORT COMPLET — ").append(LocalDate.now()).append("\n\n");
        r.append("=== Ventes ===\n");
        r.append("• Aujourd'hui : ").append(nbJour).append(" vente(s), ").append(fcfa(caJour)).append("\n");
        r.append("• Ce mois : ").append(nbMois).append(" vente(s), ").append(fcfa(caMois)).append(", bénéfice ").append(fcfa(beneficeMois)).append("\n\n");
        r.append("=== Paiements mobiles (ce mois) ===\n");
        r.append("• Orange Money : ").append(fcfa(orangeMois)).append(" — Moov Money : ").append(fcfa(moovMois)).append("\n\n");
        r.append("=== Crédits ===\n");
        r.append("• ").append(nbCreditsDus).append(" vente(s) à crédit non réglée(s), total dû ").append(fcfa(creditsDus)).append("\n\n");
        r.append("=== Stock ===\n");
        r.append("• ").append(stockFaible).append(" produit(s) en alerte, ").append(rupture).append(" en rupture totale\n");
        r.append("• Valeur totale du stock : ").append(fcfa(valeurStock)).append("\n\n");
        r.append("=== Mouvements (ce mois) ===\n");
        r.append("• Sorties de stock : ").append(sorties.size()).append(" mouvement(s)\n");
        r.append("• Ventes annulées : ").append(annulees.size());

        return r.toString();
    }

    private String reponseParDefaut() {
        return "Je n'ai pas bien compris ta question. Voici ce que je peux te dire :\n\n" +
                "• Tes ventes du jour, de la semaine ou du mois\n" +
                "• Comptant vs crédit, Orange/Moov Money\n" +
                "• Les clients qui te doivent de l'argent\n" +
                "• Ton stock (alertes, ruptures, valeur totale)\n" +
                "• Les sorties de stock et les ventes annulées\n" +
                "• Tes produits les plus rentables\n" +
                "• Des conseils ou propositions pour augmenter tes bénéfices\n" +
                "• Un rapport complet de ta boutique\n" +
                "• Comment fonctionne Ges Boutique\n\n" +
                "Choisis une suggestion ci-dessous ou reformule ta question.";
    }

    private static final String COMMENT_CA_MARCHE =
            "🏪 Comment fonctionne Ges Boutique :\n\n" +
            "• Caisse : encaisse tes ventes du jour, suis le solde de ta caisse en temps réel.\n" +
            "• Ventes : enregistre les ventes comptant ou à crédit, gère les retours produit.\n" +
            "• Clients : suis les crédits, avances et fidélité de chaque client.\n" +
            "• Stock / Inventaire : suis les entrées, sorties et alertes de rupture de stock.\n" +
            "• Fournisseurs : gère tes achats, paiements et bonus fournisseurs.\n" +
            "• Rapports : consulte ton chiffre d'affaires, tes bénéfices et tes statistiques.\n" +
            "• Employés : gère les vendeurs, leurs accès et leurs paiements.\n" +
            "• Assistant IA (ici) : pose-moi des questions sur tes chiffres, je réponds avec tes vraies données, sans connexion internet requise.\n\n" +
            "Besoin d'aide sur un module précis ? Demande-le-moi directement.";

    // =========================================================
    // Utilitaires
    // =========================================================

    private double orZero(Double d) { return d != null ? d : 0.0; }
    private int orZero(Integer i) { return i != null ? i : 0; }
    private long orZero(Long l) { return l != null ? l : 0L; }

    private String fcfa(double valeur) {
        return String.format(Locale.FRANCE, "%,.0f", valeur).replace(',', ' ').replace(' ', ' ') + " F CFA";
    }
}
