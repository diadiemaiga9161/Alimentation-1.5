package com.ges.boutique.dette;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReglementDetteRequest {
    private Long detteId;
    private Double montantPaye;
    private Long utilisateurId;
    private String modePaiement;
    private String referencePaiement;
    private String observations;
}