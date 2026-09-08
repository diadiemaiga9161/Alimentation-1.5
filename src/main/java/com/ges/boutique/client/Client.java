package com.ges.boutique.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ges.boutique.vente.Vente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String numeroTelephone;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "email")
    private String email;

    @Column(nullable = false)
    private boolean partenaire = false;

    // Solde de points fidélité (voir com.ges.boutique.fidelite) — colonne avec DEFAULT
    // pour que ddl-auto=update l'ajoute proprement sur des tables clients déjà peuplées.
    @Column(name = "points_fidelite", columnDefinition = "INT DEFAULT 0")
    private Integer pointsFidelite = 0;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @JsonIgnore
    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    private List<Vente> ventes;

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (prenom == null) {
            prenom = "";
        }
    }
}