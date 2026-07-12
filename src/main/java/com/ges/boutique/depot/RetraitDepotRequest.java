package com.ges.boutique.depot;

import lombok.Data;

@Data
public class RetraitDepotRequest {
    private Double montant;
    private String observation;
}
