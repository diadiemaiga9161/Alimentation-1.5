package com.ges.boutique.vente.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenteAnnuleeDTO {

    private Long id;
    private String numeroVente;
    private LocalDateTime dateVente;
    private LocalDateTime dateAnnulation;
    private Double montantTotal;
    private String motifAnnulation;
    /** Nom du client (peut être null si vente anonyme ou client divers) */
    private String clientNom;
    /** Nom complet de l'utilisateur qui a effectué la vente */
    private String vendeurNom;
    /** Nom complet de l'utilisateur qui a annulé la vente */
    private String annuleurNom;
}
