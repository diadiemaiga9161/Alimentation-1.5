package com.ges.boutique.fournisseur;

import com.ges.boutique.avance.StatutAvance;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "avances_fournisseurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvanceFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @Column(name = "montant", nullable = false)
    private Double montant;

    @Column(name = "montant_utilise", nullable = false)
    private Double montantUtilise = 0.0;

    @Column(name = "montant_disponible", nullable = false)
    private Double montantDisponible;

    @Column(name = "date_depot", nullable = false)
    private LocalDateTime dateDepot;

    @Column(name = "motif")
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutAvance statut = StatutAvance.DISPONIBLE;

    @Column(name = "source_financement", nullable = false)
    private String sourceFinancement; // CAISSE ou BANQUE

    @Column(name = "compte_id")
    private Long compteId;

    @Column(name = "reference_caisse_operation")
    private Long referenceCaisseOperation;

    @Column(name = "reference_compte_operation")
    private Long referenceCompteOperation;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @PrePersist
    protected void onCreate() {
        if (dateDepot == null) dateDepot = LocalDateTime.now();
        if (montantUtilise == null) montantUtilise = 0.0;
        montantDisponible = montant - montantUtilise;
        updateStatut();
    }

    @PreUpdate
    protected void onUpdate() {
        montantDisponible = montant - montantUtilise;
        updateStatut();
    }

    private void updateStatut() {
        if (montantDisponible <= 0) statut = StatutAvance.EPUISE;
        else if (montantUtilise > 0) statut = StatutAvance.UTILISE_PARTIELLEMENT;
        else statut = StatutAvance.DISPONIBLE;
    }
}
