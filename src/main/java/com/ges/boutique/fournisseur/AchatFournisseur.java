package com.ges.boutique.fournisseur;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "achats_fournisseur")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchatFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_achat", nullable = false)
    private LocalDateTime dateAchat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    @JsonBackReference
    private Fournisseur fournisseur;

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal;

    @Column(name = "montant_paye", nullable = false)
    private Double montantPaye = 0.0;

    @Column(name = "montant_restant", nullable = false)
    private Double montantRestant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAchat statut = StatutAchat.EN_COURS;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @OneToMany(mappedBy = "achat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<LigneAchatFournisseur> lignes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        if (dateAchat == null) dateAchat = LocalDateTime.now();
        if (montantPaye == null) montantPaye = 0.0;
        if (montantTotal == null) montantTotal = 0.0;
        montantRestant = montantTotal - montantPaye;
        if (montantRestant <= 0.01) {
            statut = StatutAchat.PAYE;
        } else {
            statut = StatutAchat.EN_COURS;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (montantTotal == null) montantTotal = 0.0;
        if (montantPaye == null) montantPaye = 0.0;
        montantRestant = montantTotal - montantPaye;
        if (montantRestant <= 0.01) {
            statut = StatutAchat.PAYE;
        } else if (montantPaye > 0 && montantRestant > 0.01) {
            statut = StatutAchat.EN_COURS;
        } else {
            statut = StatutAchat.EN_COURS;
        }
    }
}