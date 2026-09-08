package com.ges.boutique.produit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unité de vente alternative pour un produit (ex: "Cartouche", "Carton", "Palette"),
 * en plus de son unité de base (Produit.uniteBase, ex: "Pièce"). Volontairement SIMPLE
 * par rapport à ProduitNiveau (conservé dans le code mais mis de côté, trop complexe
 * pour un usage réel en boutique — pas de stock séparé par unité, pas d'étape
 * "décomposer" à faire à la main) :
 *
 * - Un SEUL stock pour le produit (Produit.quantite), toujours compté en unité de base.
 * - facteurBase = combien d'unités de base ça représente (ex: 50 si 1 Carton = 50 Pièces).
 *   Calculé UNE FOIS à la création (voir UniteVenteService, qui accepte en entrée soit
 *   ce facteur directement, soit un facteur relatif à une autre unité déjà créée — ex:
 *   "1 Carton = 5 Cartouches" — pour que l'admin n'ait jamais à calculer la conversion
 *   totale lui-même), puis stocké tel quel : aucun calcul de hiérarchie au moment de la
 *   vente, aucune étape supplémentaire pour le vendeur.
 * - La vente à une unité alternative réutilise le mécanisme déjà existant
 *   LigneVenteRequest.niveauFacteur/niveauNom/prixAchat (déduction directe du stock
 *   produit, quantité × facteurBase) — voir VenteServiceImpl.creerLigneVente/
 *   mettreAJourStockVente/retablirStockAncienneVente, qui gèrent déjà ce cas sans
 *   aucune modification nécessaire pour cette fonctionnalité.
 */
@Entity
@Table(name = "unite_vente")
@Getter
@Setter
@NoArgsConstructor
public class UniteVente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    @JsonIgnore
    private Produit produit;

    @Column(nullable = false, length = 40)
    private String nom;

    /** Combien d'unités de base (Produit.uniteBase) cette unité représente — ex: 50. */
    @Column(name = "facteur_base", nullable = false)
    private Integer facteurBase;

    @Column(name = "prix_vente", nullable = false)
    private Double prixVente;

    @Column(name = "prix_achat")
    private Double prixAchat;

    @Column(nullable = false)
    private Integer ordre = 0;
}
