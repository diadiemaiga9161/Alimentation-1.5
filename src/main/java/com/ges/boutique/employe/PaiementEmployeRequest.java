package com.ges.boutique.employe;

import lombok.Data;

@Data
public class PaiementEmployeRequest {
    private Long employeId;
    private Integer nombreMois;   // 1, 2 ou 3
    private String periodeDebut;  // ex: "Janvier 2025"
    private String periodeFin;    // ex: "Mars 2025" (si nombreMois > 1)
    private String observation;
    private Long utilisateurId;
}
