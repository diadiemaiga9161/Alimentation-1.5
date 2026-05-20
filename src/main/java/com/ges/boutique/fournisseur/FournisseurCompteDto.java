package com.ges.boutique.fournisseur;

import lombok.Data;
import java.util.List;

@Data
public class FournisseurCompteDto {
    private FournisseurDto fournisseur;
    private Double totalAchats;
    private Double totalPaye;
    private Double solde;
    private List<AchatFournisseur> achatsRecents;
    private List<PaiementFournisseur> paiementsRecents;
}