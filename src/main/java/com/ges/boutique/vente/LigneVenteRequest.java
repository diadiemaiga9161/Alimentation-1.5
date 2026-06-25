package com.ges.boutique.vente;

import lombok.Data;

@Data
public class LigneVenteRequest {
    private Long produitId;
    private Integer quantite;
    private Double prixUnitaire;        // Prix modifiable à la volée
    private Double remisePourcentage;
    private Double remiseMontant;
    private Double prixAchat;           // Prix achat du niveau (conditionnement) si applicable
    private String niveauNom;           // Nom du niveau vendu (ex: "Cartouche") pour historique
    private Integer niveauFacteur;      // Facteur de conversion vers l'unité de base (ex: 200 si 1 Carton = 200 Pièces)
    private Long niveauId;              // ID du ProduitNiveau vendu (null = vente au niveau produit)
}