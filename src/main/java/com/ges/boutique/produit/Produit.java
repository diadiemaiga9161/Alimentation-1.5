package com.ges.boutique.produit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ges.boutique.fournisseur.Fournisseur;
import com.ges.boutique.vente.LigneVente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Ignorer les proxies Hibernate
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @Column(name = "prix_achat", nullable = false)
    private Double prixAchat;

    @Column(name = "prix_vente", nullable = false)
    private Double prixVente;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "seuil_alerte")
    private Integer seuilAlerte = 10;

    @Column(name = "code_barre")
    private String codeBarre;

    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation;

    @Column(name = "date_peremption")
    private LocalDate datePeremption;

    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "conditions_stockage")
    private String conditionsStockage;

    @Column(name = "poids_volume")
    private Double poidsVolume;

    @Column(name = "unite_mesure")
    private String uniteMesure;

    @Column(name = "bio")
    private boolean bio = false;

    @Column(name = "origine")
    private String origine;

    @Column(name = "date_ajout")
    private LocalDateTime dateAjout;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "type_vente")
    private String typeVente; // "DETAIL" ou "ENGROS"

    @OneToMany(mappedBy = "produit", fetch = FetchType.LAZY)
    @JsonIgnore // Ignorer complètement la collection lors de la sérialisation JSON
    private List<LigneVente> lignesVente = new ArrayList<>();

    @OneToMany(mappedBy = "produit", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProduitNiveau> niveaux = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateAjout = LocalDateTime.now();
        dateModification = LocalDateTime.now();
        if (dateCreation == null) {
            dateCreation = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }

    public boolean estStockFaible() {
        return quantite <= seuilAlerte;
    }

    public boolean estPerime() {
        return datePeremption != null && datePeremption.isBefore(LocalDate.now());
    }

    public boolean estProchePeremption(int joursAlerte) {
        if (datePeremption == null) return false;
        LocalDate dateAlerte = LocalDate.now().plusDays(joursAlerte);
        return !datePeremption.isAfter(dateAlerte) && !estPerime();
    }

    public Double getMarge() {
        return prixVente - prixAchat;
    }

    public Double getTauxMarge() {
        return prixAchat > 0 ? ((prixVente - prixAchat) / prixAchat) * 100 : 0;
    }

    public Long getJoursAvantPeremption() {
        if (datePeremption == null) return null;
        return ChronoUnit.DAYS.between(LocalDate.now(), datePeremption);
    }
}
