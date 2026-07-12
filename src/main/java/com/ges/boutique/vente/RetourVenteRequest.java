package com.ges.boutique.vente;

import lombok.Data;
import java.util.List;

@Data
public class RetourVenteRequest {
    private Long venteId;
    private String motif;
    private Long utilisateurId;
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;
    private List<LigneRetourVenteRequest> lignes;

    @Data
    public static class LigneRetourVenteRequest {
        private Long ligneVenteId;
        private Long produitId;
        private Integer quantiteRetournee;
        private Double prixUnitaire;
    }
}