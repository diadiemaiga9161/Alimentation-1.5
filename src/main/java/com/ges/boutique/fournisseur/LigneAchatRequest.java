package com.ges.boutique.fournisseur;

import lombok.Data;

@Data
public class LigneAchatRequest {
    // Pour produit existant
    private Long produitId;

    // Pour nouveau produit
    private String nouveauProduitNom;
    private Long nouvelleCategorieId;
    private Double prixVente; // facultatif, sinon calcul auto
    private String description;
    private String codeBarre;
    private Integer seuilAlerte;
    private String uniteMesure;
    private boolean bio;
    private String origine;
    private String typeVente;

    // Champs communs obligatoires
    private Integer quantite;
    private Double prixAchatUnitaire;
}