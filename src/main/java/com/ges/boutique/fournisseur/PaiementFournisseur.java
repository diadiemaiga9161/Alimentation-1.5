package com.ges.boutique.fournisseur;

import com.ges.boutique.caisse.OperationCaisse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiements_fournisseur")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaiementFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @Column(nullable = false)
    private Double montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false)
    private ModePaiementFournisseur modePaiement;

    private String reference;

    @Column(columnDefinition = "TEXT")
    private String observation;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_caisse_id")
    private OperationCaisse operationCaisse;

    @Column(name = "compte_id")
    private Long compteId;

    @PrePersist
    protected void onCreate() {
        if (datePaiement == null) datePaiement = LocalDateTime.now();
    }
}