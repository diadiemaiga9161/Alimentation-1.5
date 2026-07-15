package com.ges.boutique.ia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlerteIA {

    private String type;                  // STOCK_RUPTURE, PEREMPTION, CREDIT_RETARD, CA_BAISSE
    private String severite;              // CRITIQUE, ATTENTION, INFO
    private String message;
    private Map<String, Object> donnees;
}
