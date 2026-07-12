package com.ges.boutique.employe;

import com.ges.boutique.caisse.Caisse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiements_employe")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaiementEmploye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "caisse_id")
    private Caisse caisse;

    @Column(nullable = false)
    private Double montant;

    @Column(name = "nombre_mois", nullable = false)
    private Integer nombreMois;

    @Column(name = "periode_debut", nullable = false)
    private String periodeDebut;

    @Column(name = "periode_fin")
    private String periodeFin;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private StatutPaiementEmploye statut = StatutPaiementEmploye.PAYE;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(name = "operation_caisse_id")
    private Long operationCaisseId;

    @Column(name = "motif_annulation", columnDefinition = "TEXT")
    private String motifAnnulation;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "utilisateur_annulation_id")
    private Long utilisateurAnnulationId;

    @Column(columnDefinition = "TEXT")
    private String observation;

    @PrePersist
    protected void onCreate() {
        if (datePaiement == null) datePaiement = LocalDateTime.now();
        if (statut == null) statut = StatutPaiementEmploye.PAYE;
    }
}
