package com.ges.boutique.transfert;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiements_transfert")
@Data @NoArgsConstructor @AllArgsConstructor
public class PaiementTransfert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfert_id", nullable = false)
    private TransfertStock transfert;

    @Column(nullable = false)
    private Double montant;

    @Column(name = "mode_paiement", nullable = false)
    private String modePaiement; // ESPECES | ORANGE_MONEY | MOOV_MONEY | WAVE_MONEY | VIREMENT

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "enregistre_par")
    private String enregistrePar;

    @PrePersist
    protected void onCreate() {
        if (datePaiement == null) datePaiement = LocalDateTime.now();
    }
}
