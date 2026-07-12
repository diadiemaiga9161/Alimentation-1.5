package com.ges.boutique.depot;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "depots_garde")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepotGarde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String prenom;

    @Column(nullable = false)
    private String numero;

    @Column(name = "montant_initial", nullable = false)
    private Double montantInitial;

    @Column(name = "montant_restant", nullable = false)
    private Double montantRestant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private StatutDepot statut = StatutDepot.ACTIF;

    @Column(name = "date_depot", nullable = false)
    private LocalDateTime dateDepot;

    @Column(columnDefinition = "TEXT")
    private String observation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_client_id")
    private DepotClient depotClient;

    @OneToMany(mappedBy = "depot", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("dateRetrait DESC")
    private List<RetraitDepot> retraits = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (dateDepot == null) dateDepot = LocalDateTime.now();
        if (statut == null) statut = StatutDepot.ACTIF;
        if (montantRestant == null) montantRestant = montantInitial;
    }
}
