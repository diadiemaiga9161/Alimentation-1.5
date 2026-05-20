package com.ges.boutique.fournisseur;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ges.boutique.produit.Produit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fournisseurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String code;

    private String adresse;

    private String telephone;

    private String email;

    private String siteWeb;

    @Column(name = "contact_nom")
    private String contactNom;

    @Column(name = "contact_telephone")
    private String contactTelephone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "type_produits")
    private String typeProduits;

    @Column(name = "conditions_paiement")
    private String conditionsPaiement;

    @Column(name = "delai_livraison")
    private Integer delaiLivraison;

    @Column(name = "note")
    private Integer note;

    @Column(name = "actif")
    private boolean actif = true;

    @Column(name = "date_ajout")
    private LocalDateTime dateAjout;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "total_achats", nullable = false)
    private Double totalAchats = 0.0;

    @Column(name = "total_paye", nullable = false)
    private Double totalPaye = 0.0;

    @Column(name = "solde", nullable = false)
    private Double solde = 0.0;

    @OneToMany(mappedBy = "fournisseur", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Produit> produits = new ArrayList<>();

    @OneToMany(mappedBy = "fournisseur", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<AchatFournisseur> achats = new ArrayList<>();

    @OneToMany(mappedBy = "fournisseur", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PaiementFournisseur> paiements = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateAjout = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}