package com.ges.boutique.boutique;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "boutique")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Boutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom = "Boutique Alimentaire";

    @Column(nullable = false)
    private String adresse = "Adresse de la boutique";

    @Column(nullable = false)
    private String telephone = "+223 XX XX XX XX";

    @Column
    private String email = "contact@boutique.com";

    @Column
    private String description;

    @Column(name = "site_web")
    private String siteWeb;

    @Column(name = "horaires_ouverture")
    private String horairesOuverture;

    @Column
    private Boolean actif = true;

    @Column(name = "numero_rc")
    private String numeroRc = "RC-XXXX";

    @Column(name = "numero_ifu")
    private String numeroIfu = "IFU-XXXX";

    @Column
    private String ville = "Bamako";

    @Column
    private String pays = "Mali";

    @Column(name = "code_postal")
    private String codePostal = "00000";

    @Column(name = "logo", columnDefinition = "MEDIUMTEXT")
    private String logo;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    public enum ModeOuverture { MANUEL, AUTO }

    @Column(name = "mode_ouverture")
    @Enumerated(EnumType.STRING)
    private ModeOuverture modeOuverture = ModeOuverture.MANUEL;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();

        if (actif == null) {
            actif = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }

    // =====================================================
    // COMPATIBILITÉ FRONTEND
    // =====================================================
    // Le front utilise logoPath.
    // La base garde le vrai champ logo.
    // Cette méthode évite l'erreur "Unrecognized field logoPath"
    // sans changer Angular.

    @JsonProperty("logoPath")
    public String getLogoPath() {
        return this.logo;
    }

    @JsonProperty("logoPath")
    public void setLogoPath(String logoPath) {
        // Ne pas écraser le logo avec une valeur vide.
        if (logoPath != null && !logoPath.isBlank()) {
            this.logo = logoPath;
        }
    }

    // =====================================================
    // MÉTHODES UTILITAIRES POUR LES FACTURES
    // =====================================================

    public String getAdresseComplete() {
        return adresse + ", " + ville + " " + codePostal + ", " + pays;
    }

    public String getInformationsLegales() {
        return "RC: " + numeroRc + " - IFU: " + numeroIfu;
    }
}