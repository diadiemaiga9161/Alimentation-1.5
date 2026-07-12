package com.ges.boutique.vente;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "retours_vente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetourVente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_retour", unique = true)
    private String numeroRetour;

    @ManyToOne(fetch = FetchType.EAGER)  // ← CHANGEMENT: EAGER
    @JoinColumn(name = "vente_id", nullable = false)
    private Vente vente;

    @OneToMany(mappedBy = "retour", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)  // ← CHANGEMENT: EAGER
    @JsonManagedReference
    private List<LigneRetourVente> lignes = new ArrayList<>();

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal = 0.0;

    @Column(name = "date_retour", nullable = false)
    private LocalDateTime dateRetour;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(name = "client_nom")
    private String clientNom;

    @PrePersist
    protected void onCreate() {
        dateRetour = LocalDateTime.now();
        if (numeroRetour == null) {
            numeroRetour = "RTV-" + System.currentTimeMillis();
        }
    }
}