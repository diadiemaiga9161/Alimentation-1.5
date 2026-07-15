package com.ges.boutique.ia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyseIAResult {

    private int scoreGlobal;                  // 0-100
    private String tendanceCA;                // FORTE_HAUSSE, HAUSSE, STABLE, BAISSE, FORTE_BAISSE
    private double tauxCroissanceMensuel;     // %
    private double previsionCA7Jours;
    private double previsionCA30Jours;
    private double caHier;
    private double caCetteSemaine;
    private double caCeMois;
    private List<RecommandationIA> recommandations;
    private List<AlerteIA> alertes;
    private Map<String, Integer> segmentsClients; // RFM: CHAMPIONS, LOYAUX, POTENTIELS, A_RISQUE, ENDORMIS, NOUVEAUX
    private List<Map<String, Object>> previsionCA30JoursDetail; // [{date, prevision}]
    private double precisionModele;           // % précision du modèle actuel
}
