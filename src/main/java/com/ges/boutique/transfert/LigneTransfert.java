package com.ges.boutique.transfert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lignes_transfert")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneTransfert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfert_id", nullable = false)
    @JsonIgnore
    private TransfertStock transfert;

    @Column(name = "produit_id", nullable = false)
    private Long produitId;

    @Column(name = "produit_nom", nullable = false)
    private String produitNom;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "prix_unitaire")
    private Double prixUnitaire;

    public Double getMontantTotal() {
        if (prixUnitaire == null || quantite == null) return 0.0;
        return prixUnitaire * quantite;
    }
}
