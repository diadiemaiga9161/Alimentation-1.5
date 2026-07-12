package com.ges.boutique.depot;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "retraits_depot")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetraitDepot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private DepotGarde depot;

    @Column(nullable = false)
    private Double montant;

    @Column(name = "date_retrait", nullable = false)
    private LocalDateTime dateRetrait;

    @Column(columnDefinition = "TEXT")
    private String observation;

    @PrePersist
    protected void onCreate() {
        if (dateRetrait == null) dateRetrait = LocalDateTime.now();
    }
}
