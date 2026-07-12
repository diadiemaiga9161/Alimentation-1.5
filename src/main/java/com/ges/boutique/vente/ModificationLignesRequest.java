package com.ges.boutique.vente;

import lombok.Data;
import java.util.List;

@Data
public class ModificationLignesRequest {
    private List<LigneVenteRequest> lignes;
    private Long utilisateurId;
    private String motif;
}
