package com.ges.boutique.transfert;

import lombok.Data;

import java.util.List;

@Data
public class TransfertRequest {
    private Long boutiqueDestId;
    private String typePaiement;
    private String notes;
    private List<LigneRequest> lignes;

    @Data
    public static class LigneRequest {
        private Long produitId;
        private String produitNom;
        private Integer quantite;
        private Double prixUnitaire;
    }
}
