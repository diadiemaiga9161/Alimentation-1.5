package com.ges.boutique.depense;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "depenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Depense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double montant;

    @Column(name = "type_depense")
    private String typeDepense;

    @Column(name = "operation_caisse_id")
    private Long operationCaisseId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Traçabilité comptable ────────────────────────────────────────────────
    // Une dépense validée est "verrouillée" dans les comptes : elle ne peut plus être
    // supprimée directement, seulement annulée (comme une vente) — pour ne jamais avoir
    // une entrée/sortie d'argent qui disparaît sans laisser de trace.
    @Column(nullable = false)
    private boolean validee = false;

    @Column(name = "valide_par_id")
    private Long valideParId;

    @Column(name = "valide_le")
    private LocalDateTime valideLe;

    @Column(nullable = false)
    private boolean annulee = false;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @Column(name = "annule_par_id")
    private Long annuleParId;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (date == null) date = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
