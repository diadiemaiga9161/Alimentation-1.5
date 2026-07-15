package com.ges.boutique.ia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommandationIA {

    private String id;           // UUID — référence pour le feedback
    private String type;         // REAPPRO, CONTACT_CLIENT, PRIX, PLANNING, STOCK
    private String priorite;     // CRITIQUE, HAUTE, MOYENNE, BASSE
    private String titre;
    private String description;
    private String actionLabel;  // "Commander maintenant", "Contacter", etc.
    private Map<String, Object> donnees; // données spécifiques (produitId, clientId...)
    private double scoreConfiance; // 0-100
}
