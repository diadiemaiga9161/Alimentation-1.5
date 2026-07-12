package com.ges.boutique.caisse;

import com.ges.boutique.compte.Compte;
import com.ges.boutique.compte.OperationCompte;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "transferts_caisse_banque")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransfertCaisseBanque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operation_caisse_id", nullable = false)
    private OperationCaisse operationCaisse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operation_compte_id", nullable = false)
    private OperationCompte operationCompte;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @Column(nullable = false)
    private Double montant;

    private String motif;
    private String reference;

    @Column(name = "date_transfert", nullable = false)
    private LocalDateTime dateTransfert;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @PrePersist
    protected void onCreate() {
        dateTransfert = LocalDateTime.now();
    }
}