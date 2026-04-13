package com.ges.boutique.caisse;

import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.vente.Vente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "operations_caisse")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationCaisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeOperationCaisse type;

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private Double soldeAvant;

    @Column(nullable = false)
    private Double soldeApres;

    private String motif;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vente_id")
    private Vente vente;

    @Enumerated(EnumType.STRING)
    private ModePaiementCaisse modePaiement;

    @Column(name = "reference_paiement")
    private String referencePaiement;

    @Column(name = "client_nom")
    private String clientNom;

    @Column(name = "client_telephone")
    private String clientTelephone;

    @Column(name = "date_operation")
    private LocalDateTime dateOperation;

    @Column(name = "est_reglee", nullable = false)
    private boolean estReglee = true;

    @Column(name = "date_echeance")
    private LocalDateTime dateEcheance;

    @Column(name = "montant_verse")
    private Double montantVerse = 0.0;

    @Column(name = "montant_restant")
    private Double montantRestant = 0.0;

    @Column(name = "vente_credit_id")
    private Long venteCreditId;

    @Column(name = "numero_credit")
    private String numeroCredit;

    @Column(name = "periode")
    private String periode;

    @PrePersist
    protected void onCreate() {
        dateOperation = LocalDateTime.now();
        if (type == TypeOperationCaisse.VENTE_CREDIT) {
            montantRestant = montant;
            montantVerse = 0.0;
        } else if (type == TypeOperationCaisse.REGLEMENT_CREDIT) {
            if (montantRestant != null) {
                montantRestant -= montant;
            }
        }
    }
}