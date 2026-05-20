package com.ges.boutique.dette;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DetteAncienneDto {
    private Long id;
    private Long clientId;
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;
    private Double montantInitial;
    private Double montantRestant;
    private Double montantPaye;
    private LocalDate dateCredit;
    private String description;
    private Boolean estReglee;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDernierReglement;
}