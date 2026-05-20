package com.ges.boutique.avance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "avances_clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvanceClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_nom", nullable = false)
    private String clientNom;

    @Column(name = "client_telephone")
    private String clientTelephone;

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

    @Column(name = "reference_caisse_operation")
    private Long referenceCaisseOperation;

    @PrePersist
    protected void onCreate() {
        if (dateDepot == null) dateDepot = LocalDateTime.now();
        if (montantUtilise == null) montantUtilise = 0.0;
        montantDisponible = montant - montantUtilise;
        if (montantDisponible <= 0) statut = StatutAvance.EPUISE;
        else if (montantUtilise > 0) statut = StatutAvance.UTILISE_PARTIELLEMENT;
        else statut = StatutAvance.DISPONIBLE;
    }

    @PreUpdate
    protected void onUpdate() {
        montantDisponible = montant - montantUtilise;
        if (montantDisponible <= 0) statut = StatutAvance.EPUISE;
        else if (montantUtilise > 0) statut = StatutAvance.UTILISE_PARTIELLEMENT;
        else statut = StatutAvance.DISPONIBLE;
    }
}
