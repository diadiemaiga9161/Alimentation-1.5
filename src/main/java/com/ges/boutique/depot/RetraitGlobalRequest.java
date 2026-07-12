package com.ges.boutique.depot;

import lombok.Data;

@Data
public class RetraitGlobalRequest {
    private String numero;
    private Double montant; // null ou 0 = retrait total de tous les dépôts actifs du client
    private String observation;
}
