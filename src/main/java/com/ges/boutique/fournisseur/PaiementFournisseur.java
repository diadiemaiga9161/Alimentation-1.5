package com.ges.boutique.fournisseur;

import com.ges.boutique.caisse.OperationCaisse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @Column(name = "mode_paiement", nullable = false, columnDefinition = "VARCHAR(20)")
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

    // Ajouter ce champ
    @Column(name = "achat_cible_id")
    private Long achatCibleId;  // L'achat spécifique auquel ce paiement est destiné (optionnel)

    // Ajouter cette relation
    @OneToMany(mappedBy = "paiementId", fetch = FetchType.LAZY)
    private List<AchatPaiementLien> liensAchats = new ArrayList<>();

    @Column(name = "annule", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean annule = false;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "motif_annulation")
    private String motifAnnulation;
}