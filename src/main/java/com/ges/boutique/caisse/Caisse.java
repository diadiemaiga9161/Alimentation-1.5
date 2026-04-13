package com.ges.boutique.caisse;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "caisses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Caisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solde_actuel", nullable = false)
    private Double soldeActuel = 0.0;

    @Column(name = "solde_initial", nullable = false)
    private Double soldeInitial = 0.0;

    @Column(name = "solde_systeme", nullable = false)
    private Double soldeSysteme = 0.0;

    @Column(name = "solde_reel", nullable = false)
    private Double soldeReel = 0.0;

    @Column(name = "ecart", nullable = false)
    private Double ecart = 0.0;

    @Column(name = "total_entrees", nullable = false)
    private Double totalEntrees = 0.0;

    @Column(name = "total_sorties", nullable = false)
    private Double totalSorties = 0.0;

    @Column(name = "derniere_operation")
    private LocalDateTime derniereOperation;

    @Column(name = "date_ouverture")
    private LocalDateTime dateOuverture;

    @Column(name = "date_fermeture")
    private LocalDateTime dateFermeture;

    @Column(name = "est_ouverte", nullable = false)
    private boolean estOuverte = true;

    @Column(name = "nombre_operations")
    private Integer nombreOperations = 0;

    @Column(name = "verifiee", nullable = false)
    private boolean verifiee = false;

    @Column(name = "date_verification")
    private LocalDateTime dateVerification;

    @Column(name = "utilisateur_verification")
    private String utilisateurVerification;

    @PrePersist
    protected void onCreate() {
        dateOuverture = LocalDateTime.now();
        derniereOperation = LocalDateTime.now();
        soldeInitial = soldeActuel;
        soldeSysteme = soldeActuel;
        soldeReel = soldeActuel;
    }

    @PreUpdate
    protected void onUpdate() {
        derniereOperation = LocalDateTime.now();
        calculerEcart();
    }

    public void calculerEcart() {
        this.ecart = this.soldeReel - this.soldeSysteme;
    }

    public void mettreAJourSoldeSysteme() {
        this.soldeSysteme = this.soldeActuel;
        calculerEcart();
    }

    public void verifierCaisse(Double soldeReelSaisi, String utilisateur) {
        this.soldeReel = soldeReelSaisi;
        this.verifiee = true;
        this.dateVerification = LocalDateTime.now();
        this.utilisateurVerification = utilisateur;
        calculerEcart();
    }
}