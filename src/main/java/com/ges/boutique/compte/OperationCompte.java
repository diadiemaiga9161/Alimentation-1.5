package com.ges.boutique.compte;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "operations_compte")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationCompte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private Double montant;

    @Column(name = "solde_avant", nullable = false)
    private Double soldeAvant;

    @Column(name = "solde_apres", nullable = false)
    private Double soldeApres;

    private String motif;
    private String reference;

    @Column(name = "date_operation")
    private LocalDateTime dateOperation;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @PrePersist
    protected void onCreate() {
        if (dateOperation == null) dateOperation = LocalDateTime.now();
    }
}