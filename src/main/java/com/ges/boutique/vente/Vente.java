package com.ges.boutique.vente;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ges.boutique.client.Client;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id")
    private Client client;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JsonManagedReference
    private List<LigneVente> lignes = new ArrayList<>();

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal = 0.0;

    @Column(name = "montant_remise_total")
    private Double montantRemiseTotal = 0.0;

    @Column(name = "montant_apres_remise")
    private Double montantApresRemise = 0.0;

    @Column(name = "benefice_total", nullable = false)
    private Double beneficeTotal = 0.0;

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

    @Column(name = "est_credit")
    private Boolean estCredit = false;

    @Column(name = "client_nom")
    private String clientNom;

    @Column(name = "client_prenom")
    private String clientPrenom;

    @Column(name = "client_telephone")
    private String clientTelephone;

    @Column(name = "client_divers")
    private Boolean clientDivers = true;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "montant_verse")
    private Double montantVerse = 0.0;

    @Column(name = "montant_avance_utilise")
    private Double montantAvanceUtilise = 0.0;

    @Column(name = "montant_restant")
    private Double montantRestant = 0.0;

    @Column(name = "date_reglement")
    private LocalDate dateReglement;

    @Column(name = "credit_regle")
    private Boolean creditRegle = false;

    @Column(name = "annulee")
    private Boolean annulee = false;

    @Column(name = "est_retourne")
    private Boolean estRetourne = false;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "utilisateur_annulation")
    private Long utilisateurAnnulation;

    @Column(name = "client_request_id", unique = true)
    private String clientRequestId;

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
        Double sousTotalLignes = 0.0;
        Double totalRemisesLignes = 0.0;
        Double totalBeneficeLignes = 0.0;

        if (lignes != null && !lignes.isEmpty()) {
            for (LigneVente ligne : lignes) {
                if (ligne.getSousTotal() == null || ligne.getBenefice() == null) {
                    ligne.calculerSousTotal();
                }
                sousTotalLignes += ligne.getSousTotal();
                totalRemisesLignes += ligne.getMontantRemise();
                totalBeneficeLignes += ligne.getBenefice() != null ? ligne.getBenefice() : 0.0;
            }
        }

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

        montantRemiseTotal = totalRemisesLignes + reductionGlobale;
        montantApresRemise = montantApresRemiseGlobale;
        montantTotal = montantApresRemise;
        beneficeTotal = totalBeneficeLignes - reductionGlobale;

        montantTotal = BigDecimal.valueOf(montantTotal)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        montantRemiseTotal = BigDecimal.valueOf(montantRemiseTotal)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        montantApresRemise = BigDecimal.valueOf(montantApresRemise)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        beneficeTotal = BigDecimal.valueOf(beneficeTotal)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        if (Boolean.TRUE.equals(estCredit)) {
            montantRestant = montantApresRemise - montantVerse;
            if (montantRestant <= 0) {
                creditRegle = true;
                dateReglement = LocalDate.now();
            }
        }
    }

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

    @Transient
    public Long getVendeurId() {
        return vendeur != null ? vendeur.getId() : null;
    }

    @Transient
    public Long getClientId() {
        return client != null ? client.getId() : null;
    }
}