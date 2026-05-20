package com.ges.boutique.facture;

import lombok.Data;
import java.util.List;

@Data
public class FactureRequest {
    private Long clientId;
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;
    private String clientAdresse;
    private Boolean creerClient;
    private String notes;
    private Long utilisateurId;
    private Double remiseGlobale;
    private String typeRemiseGlobale;
    private List<LigneFactureRequest> lignes;
}