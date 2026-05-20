package com.ges.boutique.compte;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "comptes_bancaires")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_banque", nullable = false)
    private String nomBanque;

    @Column(name = "numero_compte")
    private String numeroCompte;

    @Column(name = "agence")
    private String agence;

    @Column(name = "titulaire")
    private String titulaire;

    @Column(name = "solde_initial", nullable = false)
    private Double soldeInitial = 0.0;

    @Column(name = "total_versements", nullable = false)
    private Double totalVersements = 0.0;

    @Column(name = "total_retraits", nullable = false)
    private Double totalRetraits = 0.0;

    @Column(name = "total_cheques", nullable = false)
    private Double totalCheques = 0.0;

    @Column(name = "total_frais", nullable = false)
    private Double totalFrais = 0.0;

    @Column(name = "total_bons_caisse", nullable = false)
    private Double totalBonsCaisse = 0.0;

    @Column(name = "solde_actuel", nullable = false)
    private Double soldeActuel = 0.0;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
        recalculerSolde();
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
        recalculerSolde();
    }

    public void recalculerSolde() {
        this.soldeActuel = (soldeInitial != null ? soldeInitial : 0.0)
                + (totalVersements != null ? totalVersements : 0.0)
                - (totalRetraits != null ? totalRetraits : 0.0)
                - (totalCheques != null ? totalCheques : 0.0)
                - (totalFrais != null ? totalFrais : 0.0)
                - (totalBonsCaisse != null ? totalBonsCaisse : 0.0);
    }
}
