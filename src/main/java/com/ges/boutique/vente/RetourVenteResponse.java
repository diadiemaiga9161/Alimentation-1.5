package com.ges.boutique.vente;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetourVenteResponse {
    private Long id;
    private String numeroRetour;
    private Long venteId;
    private String numeroVente;
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;
    private Double montantTotal;
    private String motif;
    private LocalDateTime dateRetour;
    private Long utilisateurId;
    private List<LigneRetourVenteResponse> lignes;
}