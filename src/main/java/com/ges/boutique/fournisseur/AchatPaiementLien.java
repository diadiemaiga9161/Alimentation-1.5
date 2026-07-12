package com.ges.boutique.fournisseur;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "achat_paiements_lien")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchatPaiementLien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "achat_id", nullable = false)
    private Long achatId;

    @Column(name = "paiement_id", nullable = false)
    private Long paiementId;

    @Column(name = "montant_applique", nullable = false)
    private Double montantApplique;

    @Column(name = "date_lien", nullable = false)
    private LocalDateTime dateLien;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @PrePersist
    protected void onCreate() {
        dateLien = LocalDateTime.now();
    }
}