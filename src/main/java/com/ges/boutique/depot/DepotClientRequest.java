package com.ges.boutique.depot;

import lombok.Data;

@Data
public class DepotClientRequest {
    private String nom;
    private String prenom;
    private String numero;
    private String adresse;
    private String observation;
}
