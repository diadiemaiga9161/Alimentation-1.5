package com.ges.boutique.promo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "type_reduction", nullable = false)
    private String typeReduction; // POURCENTAGE ou MONTANT_FIXE

    @Column(name = "valeur_reduction", nullable = false)
    private Double valeurReduction;

    @Column(nullable = false)
    private Boolean active = true;

    // true = s'applique à tous les produits, false = produits spécifiques
    @Column(nullable = false)
    private Boolean globale = false;

    // IDs des produits concernés (vide si globale = true)
    @ElementCollection
    @CollectionTable(name = "promotion_produits", joinColumns = @JoinColumn(name = "promotion_id"))
    @Column(name = "produit_id")
    private List<Long> produitIds = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
