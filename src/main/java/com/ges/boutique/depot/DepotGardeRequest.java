package com.ges.boutique.depot;

import lombok.Data;

@Data
public class DepotGardeRequest {
    private Long depotClientId;
    private String nom;
    private String prenom;
    private String numero;
    private Double montant;
    private String observation;
}
