package com.ges.boutique.rapport;

import com.ges.boutique.inventaire.InventaireService;
import com.ges.boutique.produit.ProduitService;
import com.ges.boutique.vente.VenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RapportServiceImpl implements RapportService {

    private final VenteService venteService;
    private final ProduitService produitService;
    private final InventaireService inventaireService;

    @Override
    public Map<String, Object> genererRapportJournalier(LocalDate date) {
        Map<String, Object> rapport = new HashMap<>();

        // Données des ventes
        Map<String, Object> statsVentes = venteService.obtenirStatistiquesJournalieres(date);
        rapport.put("statsVentes", statsVentes);

        // Statistiques d'inventaire
        Map<String, Object> statsInventaire = produitService.obtenirStatistiquesStock();
        rapport.put("statsInventaire", statsInventaire);

        // Produits en stock faible
        rapport.put("produitsStockFaible", produitService.obtenirProduitsStockFaible());

        // Date du rapport
        rapport.put("dateRapport", date);
        rapport.put("dateGeneration", LocalDateTime.now());

        return rapport;
    }

    @Override
    public Map<String, Object> genererRapportHebdomadaire() {
        Map<String, Object> rapport = new HashMap<>();

        // Données hebdomadaires
        Map<String, Object> statsHebdo = venteService.obtenirStatistiquesHebdomadaires();
        rapport.put("statsHebdomadaires", statsHebdo);

        // Comparaison avec la semaine précédente
        LocalDate finSemainePrecedente = LocalDate.now().minusWeeks(1);
        LocalDate debutSemainePrecedente = finSemainePrecedente.minusDays(6);

        Long ventesSemainePrecedente = venteService.compterVentesParDateRange(
                debutSemainePrecedente.atStartOfDay(),
                finSemainePrecedente.atTime(LocalTime.MAX)
        );

        long ventesCetteSemaine = (long) statsHebdo.get("nombreVentes");
        double croissance = ventesSemainePrecedente > 0 ?
                ((ventesCetteSemaine - ventesSemainePrecedente) * 100.0 / ventesSemainePrecedente) : 0;

        rapport.put("croissanceVentes", croissance);
        rapport.put("periodeComparaison", "Semaine précédente");

        // Meilleurs produits de la semaine
        rapport.put("meilleursProduits", obtenirMeilleursProduits());

        return rapport;
    }

    @Override
    public Map<String, Object> genererRapportMensuel() {
        Map<String, Object> rapport = new HashMap<>();

        // Données mensuelles
        Map<String, Object> statsMensuel = venteService.obtenirStatistiquesMensuelles();
        rapport.put("statsMensuel", statsMensuel);

        // Statistiques d'inventaire
        Map<String, Object> statsInventaire = inventaireService.obtenirStatistiquesInventaire();
        rapport.put("statsInventaire", statsInventaire);

        // Analyse des tendances
        rapport.put("tendances", analyserTendances());

        return rapport;
    }

    @Override
    public Map<String, Object> genererStatistiquesGenerales() {
        Map<String, Object> statistiques = new HashMap<>();

        // Chiffre d'affaires
        Map<String, Object> statsCA = venteService.obtenirStatistiquesChiffreAffaire();
        statistiques.put("chiffreAffaire", statsCA);

        // Inventaire
        Map<String, Object> statsStock = produitService.obtenirStatistiquesStock();
        statistiques.put("inventaire", statsStock);

        // Mouvements
        Map<String, Object> statsMouvements = inventaireService.obtenirStatistiquesInventaire();
        statistiques.put("mouvementsStock", statsMouvements);

        return statistiques;
    }

    private Map<String, Object> obtenirMeilleursProduits() {
        // À implémenter: logique pour obtenir les produits les plus vendus
        Map<String, Object> meilleursProduits = new HashMap<>();
        meilleursProduits.put("message", "Fonctionnalité en développement");
        return meilleursProduits;
    }

    private Map<String, Object> analyserTendances() {
        // À implémenter: analyse des tendances de vente
        Map<String, Object> tendances = new HashMap<>();
        tendances.put("message", "Fonctionnalité en développement");
        return tendances;
    }
}