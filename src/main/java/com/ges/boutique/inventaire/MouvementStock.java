package com.ges.boutique.inventaire;

import com.ges.boutique.produit.Produit;
import com.ges.boutique.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mouvements_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MouvementStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private Integer quantite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMouvement typeMouvement;

    @Column(name = "quantite_avant")
    private Integer quantiteAvant;

    @Column(name = "quantite_apres")
    private Integer quantiteApres;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column
    private String motif;

    @Column(name = "date_mouvement")
    private LocalDateTime dateMouvement;

    @Column(name = "achat_id")
    private Long achatId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @PrePersist
    protected void onCreate() {
        if (dateMouvement == null) dateMouvement = LocalDateTime.now();
    }
}