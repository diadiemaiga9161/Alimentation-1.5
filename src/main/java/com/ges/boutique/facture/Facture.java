package com.ges.boutique.facture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ges.boutique.boutique.Boutique;
import com.ges.boutique.client.Client;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.vente.RemiseType;
import com.ges.boutique.vente.Vente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "factures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroFacture;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id")
    @JsonIgnoreProperties({"ventes", "hibernateLazyInitializer", "handler"})
    private Client client;

    @Column(name = "client_nom")
    private String clientNom;

    @Column(name = "client_prenom")
    private String clientPrenom;

    @Column(name = "client_telephone")
    private String clientTelephone;

    @Column(name = "client_adresse")
    private String clientAdresse;

    @Column(name = "client_divers")
    private Boolean clientDivers = false;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JsonManagedReference
    private List<LigneFacture> lignes = new ArrayList<>();

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal = 0.0;

    @Column(name = "montant_remise_total")
    private Double montantRemiseTotal = 0.0;

    @Column(name = "montant_apres_remise")
    private Double montantApresRemise = 0.0;

    @Column(name = "benefice_total")
    private Double beneficeTotal = 0.0;

    @Column(name = "remise_globale")
    private Double remiseGlobale = 0.0;

    @Column(name = "type_remise_globale")
    @Enumerated(EnumType.STRING)
    private RemiseType typeRemiseGlobale;

    @Column(name = "statut", nullable = false)
    private String statut = "BROUILLON";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vente_id")
    @JsonIgnore  // ← IGNORE LA RELATION LAZY POUR ÉVITER L'EXCEPTION
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Vente vente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    @JsonIgnore  // ← IGNORE LA RELATION LAZY POUR ÉVITER L'EXCEPTION
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "boutique_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Boutique boutique;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "chauffeur")
    private String chauffeur;

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        dateModification = LocalDateTime.now();
        if (numeroFacture == null) {
            numeroFacture = "FACT-" + System.currentTimeMillis();
        }
        calculerTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
        calculerTotal();
    }

    public void ajouterLigne(LigneFacture ligne) {
        if (lignes == null) lignes = new ArrayList<>();
        ligne.setFacture(this);
        lignes.add(ligne);
        calculerTotal();
    }

    public void calculerTotal() {
        Double sousTotalLignes = 0.0;
        Double totalRemisesLignes = 0.0;
        Double totalBeneficeLignes = 0.0;

        if (lignes != null && !lignes.isEmpty()) {
            for (LigneFacture ligne : lignes) {
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

        montantTotal = BigDecimal.valueOf(montantTotal).setScale(2, RoundingMode.HALF_UP).doubleValue();
        montantRemiseTotal = BigDecimal.valueOf(montantRemiseTotal).setScale(2, RoundingMode.HALF_UP).doubleValue();
        montantApresRemise = BigDecimal.valueOf(montantApresRemise).setScale(2, RoundingMode.HALF_UP).doubleValue();
        beneficeTotal = BigDecimal.valueOf(beneficeTotal).setScale(2, RoundingMode.HALF_UP).doubleValue();
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

    public boolean isPayee() { return "PAYEE".equals(statut) || "VALIDE".equals(statut); }
    public boolean isAnnulee() { return "ANNULEE".equals(statut); }
    public void marquerPayee() { this.statut = "PAYEE"; }
    public void marquerAnnulee() { this.statut = "ANNULEE"; }
    public void valider() { this.statut = "VALIDE"; }
}