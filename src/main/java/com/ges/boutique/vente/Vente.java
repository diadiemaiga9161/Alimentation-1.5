package com.ges.boutique.vente;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ges.boutique.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroVente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private Utilisateur vendeur;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JsonManagedReference
    private List<LigneVente> lignes = new ArrayList<>();

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal = 0.0;

    @Column(name = "montant_remise_total")
    private Double montantRemiseTotal = 0.0;

    @Column(name = "montant_apres_remise")
    private Double montantApresRemise = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false)
    private ModePaiement modePaiement;

    @Column(name = "reference_paiement")
    private String referencePaiement;

    @Column(name = "date_vente")
    private LocalDateTime dateVente;

    @Column(name = "remise_globale")
    private Double remiseGlobale = 0.0;

    @Column(name = "type_remise_globale")
    @Enumerated(EnumType.STRING)
    private RemiseType typeRemiseGlobale;

    // Nouveaux champs pour les crédits
    @Column(name = "est_credit")
    private Boolean estCredit = false;

    @Column(name = "client_nom")
    private String clientNom;

    @Column(name = "client_telephone")
    private String clientTelephone;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "montant_verse")
    private Double montantVerse = 0.0;

    @Column(name = "montant_restant")
    private Double montantRestant = 0.0;

    @Column(name = "date_reglement")
    private LocalDate dateReglement;

    @Column(name = "credit_regle")
    private Boolean creditRegle = false;

    // NOUVEAUX CHAMPS POUR L'ANNULATION (SOFT DELETE)
    @Column(name = "annulee")
    private Boolean annulee = false;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "utilisateur_annulation")
    private Long utilisateurAnnulation;

    @PrePersist
    protected void onCreate() {
        dateVente = LocalDateTime.now();
        if (numeroVente == null) {
            numeroVente = "VT-" + System.currentTimeMillis();
        }
        if (lignes != null && !lignes.isEmpty()) {
            calculerTotal();
        }
        if (Boolean.TRUE.equals(estCredit)) {
            montantRestant = montantApresRemise - montantVerse;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (lignes != null && !lignes.isEmpty()) {
            calculerTotal();
        }
        if (Boolean.TRUE.equals(estCredit)) {
            montantRestant = montantApresRemise - montantVerse;
            if (montantRestant <= 0) {
                creditRegle = true;
                dateReglement = LocalDate.now();
            }
        }
    }

    public void ajouterLigne(LigneVente ligne) {
        if (lignes == null) {
            lignes = new ArrayList<>();
        }
        ligne.setVente(this);
        lignes.add(ligne);
        calculerTotal();
    }

    public void supprimerLigne(LigneVente ligne) {
        if (lignes != null) {
            lignes.remove(ligne);
            ligne.setVente(null);
            calculerTotal();
        }
    }

    public void calculerTotal() {
        // Initialiser les totaux
        Double sousTotalLignes = 0.0;
        Double totalRemisesLignes = 0.0;

        if (lignes != null && !lignes.isEmpty()) {
            for (LigneVente ligne : lignes) {
                // S'assurer que le sous-total est calculé
                if (ligne.getSousTotal() == null) {
                    ligne.calculerSousTotal();
                }
                sousTotalLignes += ligne.getSousTotal();
                totalRemisesLignes += ligne.getMontantRemise();
            }
        }

        // Calculer la remise globale
        Double montantApresRemiseGlobale = sousTotalLignes;
        Double reductionGlobale = 0.0;

        if (remiseGlobale != null && remiseGlobale > 0 && typeRemiseGlobale != null) {
            switch (typeRemiseGlobale) {
                case POURCENTAGE:
                    reductionGlobale = sousTotalLignes * (remiseGlobale / 100);
                    montantApresRemiseGlobale = Math.max(0, sousTotalLignes - reductionGlobale);
                    break;
                case MONTANT_FIXE:
                    reductionGlobale = Math.min(remiseGlobale, sousTotalLignes);
                    montantApresRemiseGlobale = Math.max(0, sousTotalLignes - remiseGlobale);
                    break;
            }
        }

        // Calculer les totaux finaux
        montantRemiseTotal = totalRemisesLignes + reductionGlobale;
        montantApresRemise = montantApresRemiseGlobale;
        montantTotal = montantApresRemise;

        // Arrondir les valeurs
        montantTotal = BigDecimal.valueOf(montantTotal)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        montantRemiseTotal = BigDecimal.valueOf(montantRemiseTotal)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        montantApresRemise = BigDecimal.valueOf(montantApresRemise)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        // Mettre à jour le montant restant pour les crédits
        if (Boolean.TRUE.equals(estCredit)) {
            montantRestant = montantApresRemise - montantVerse;
            if (montantRestant <= 0) {
                creditRegle = true;
                dateReglement = LocalDate.now();
            }
        }
    }

    // Méthodes pour appliquer une remise globale
    public void appliquerRemiseGlobalePourcentage(Double pourcentage) {
        if (pourcentage != null && pourcentage >= 0 && pourcentage <= 100) {
            this.remiseGlobale = pourcentage;
            this.typeRemiseGlobale = RemiseType.POURCENTAGE;
            calculerTotal();
        }
    }

    public void appliquerRemiseGlobaleMontant(Double montant) {
        if (montant != null && montant >= 0) {
            this.remiseGlobale = montant;
            this.typeRemiseGlobale = RemiseType.MONTANT_FIXE;
            calculerTotal();
        }
    }

    // Méthode pour enregistrer un règlement
    public void enregistrerReglement(Double montant, LocalDate dateReglement) {
        if (Boolean.TRUE.equals(estCredit) && !Boolean.TRUE.equals(creditRegle)) {
            if (montant == null || montant <= 0) {
                throw new IllegalArgumentException("Le montant du règlement doit être supérieur à 0");
            }
            if (montant > montantRestant) {
                throw new IllegalArgumentException("Le montant réglé ne peut pas dépasser le montant restant");
            }
            this.montantVerse = (this.montantVerse != null ? this.montantVerse : 0) + montant;
            this.montantRestant = montantApresRemise - this.montantVerse;
            if (this.montantRestant <= 0) {
                this.creditRegle = true;
                this.dateReglement = dateReglement;
            }
            calculerTotal();
        }
    }

    // Méthode utilitaire pour obtenir l'ID du vendeur
    @Transient
    public Long getVendeurId() {
        return vendeur != null ? vendeur.getId() : null;
    }
}