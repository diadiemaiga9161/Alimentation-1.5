package com.ges.boutique.fournisseur;

import lombok.Data;
import java.util.List;

@Data
public class RetourAchatRequest {
    private Long achatId;
    private String motif;
    private String modeRemboursement; // CAISSE ou BANQUE
    private Long compteId;            // requis si BANQUE
    private Long utilisateurId;
    private List<LigneRetourAchatRequest> lignes;

    @Data
    public static class LigneRetourAchatRequest {
        private Long ligneAchatId;
        private Long produitId;
        private Integer quantiteRetournee;
        private Double prixUnitaire;
    }
}
