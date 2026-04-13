package com.ges.boutique.fournisseur;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @OneToMany(mappedBy = "fournisseur", fetch = FetchType.LAZY)
    @JsonIgnore // Ignorer la sérialisation de cette collection pour éviter LazyInitializationException
    private List<Produit> produits = new ArrayList<>();

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