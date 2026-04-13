package com.ges.boutique.inventaire;

import com.ges.boutique.produit.Produit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventaire")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id", nullable = false, unique = true)
    private Produit produit;

    @Column(name = "quantite_actuelle", nullable = false)
    private Integer quantiteActuelle;

    @Column(name = "quantite_minimale", nullable = false)
    private Integer quantiteMinimale;

    @Column(name = "date_derniere_mise_a_jour")
    private LocalDateTime dateDerniereMiseAJour;

    @PrePersist
    protected void onCreate() {
        dateDerniereMiseAJour = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dateDerniereMiseAJour = LocalDateTime.now();
    }
}