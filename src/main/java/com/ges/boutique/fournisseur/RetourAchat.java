package com.ges.boutique.fournisseur;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "retours_achat")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetourAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_retour", unique = true)
    private String numeroRetour;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "achat_id", nullable = false)
    private AchatFournisseur achat;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @OneToMany(mappedBy = "retour", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<LigneRetourAchat> lignes = new ArrayList<>();

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal = 0.0;

    @Column(name = "montant_rembourse", nullable = false)
    private Double montantRembourse = 0.0;

    @Column(name = "montant_dette_reduit", nullable = false)
    private Double montantDetteReduit = 0.0;

    @Column(name = "date_retour", nullable = false)
    private LocalDateTime dateRetour;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "mode_remboursement", nullable = false)
    private String modeRemboursement;

    @Column(name = "compte_id")
    private Long compteId;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @PrePersist
    protected void onCreate() {
        dateRetour = LocalDateTime.now();
        if (numeroRetour == null) {
            numeroRetour = "RTA-" + System.currentTimeMillis();
        }
    }
}
