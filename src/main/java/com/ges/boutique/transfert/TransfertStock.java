package com.ges.boutique.transfert;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transferts_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransfertStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_transfert", unique = true)
    private String numeroTransfert;

    @Column(name = "boutique_source_nom", nullable = false)
    private String boutiqueSourceNom;

    @Column(name = "boutique_source_url", nullable = false)
    private String boutiqueSourceUrl;

    @Column(name = "boutique_dest_nom", nullable = false)
    private String boutiqueDestNom;

    @Column(name = "boutique_dest_url", nullable = false)
    private String boutiqueDestUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTransfert statut = StatutTransfert.CREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_paiement", nullable = false)
    private TypePaiementTransfert typePaiement = TypePaiementTransfert.SANS_PAIEMENT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_confirmation")
    private LocalDateTime dateConfirmation;

    @Column(name = "confirme_par")
    private String confirmeParUser;

    @Column(name = "cree_par")
    private String creePar;

    @OneToMany(mappedBy = "transfert", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneTransfert> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "transfert", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateAction DESC")
    private List<HistoriqueTransfert> historique = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        if (numeroTransfert == null) {
            numeroTransfert = "TRF-" + System.currentTimeMillis();
        }
    }
}
