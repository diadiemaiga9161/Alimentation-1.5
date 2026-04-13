package com.ges.boutique.rapport;

import lombok.Data;

import java.util.Map;

@Data
public class Statistiques {
    private Map<String, Double> chiffreAffaire;
    private Map<String, Object> inventaire;
    private Map<String, Object> mouvementsStock;
}