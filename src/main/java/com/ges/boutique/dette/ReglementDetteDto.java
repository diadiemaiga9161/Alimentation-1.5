package com.ges.boutique.dette;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReglementDetteDto {
    private Long id;
    private Long detteId;
    private Double montantPaye;
    private Double montantRestantApres;
    private LocalDateTime dateReglement;
    private Long utilisateurId;
    private String utilisateurNom;
    private String modePaiement;
    private String referencePaiement;
    private String observations;
}