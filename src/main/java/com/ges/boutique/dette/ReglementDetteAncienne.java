package com.ges.boutique.dette;

import com.ges.boutique.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reglements_dettes_anciennes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReglementDetteAncienne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dette_id", nullable = false)
    private DetteAncienne dette;

    @Column(nullable = false)
    private Double montantPaye;

    @Column(nullable = false)
    private Double montantRestantApres;

    @Column(nullable = false)
    private LocalDateTime dateReglement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String modePaiement;

    @Column(name = "reference_paiement")
    private String referencePaiement;

    @Column(length = 500)
    private String observations;

    @PrePersist
    protected void onCreate() {
        dateReglement = LocalDateTime.now();
    }
}