package com.ges.boutique.fidelite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ges.boutique.client.Client;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Historique des mouvements de points fidélité — même principe que MouvementStock : on
 * ne modifie jamais le solde (Client.pointsFidelite) sans laisser une trace expliquant
 * pourquoi. points est signé (positif = gagné, négatif = utilisé) pour que la somme des
 * mouvements d'un client corresponde toujours à son solde actuel.
 */
@Entity
@Table(name = "mouvement_fidelite")
@Getter
@Setter
@NoArgsConstructor
public class MouvementFidelite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnoreProperties({"ventes", "hibernateLazyInitializer", "handler"})
    private Client client;

    // columnDefinition forcé en VARCHAR : même précaution que PermissionVendeur.cle et
    // FonctionnaliteBoutique.cle — évite qu'Hibernate crée un ENUM MySQL natif qui
    // planterait dès qu'une nouvelle valeur serait ajoutée à TypeMouvementFidelite.
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "VARCHAR(20)")
    private TypeMouvementFidelite type;

    /** Signé : positif pour GAGNE/AJUSTEMENT positif, négatif pour UTILISE/AJUSTEMENT négatif. */
    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "vente_id")
    private Long venteId;

    @Column(name = "motif", length = 255)
    private String motif;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;

    /** Mis à true si la vente référencée (venteId) est annulée — le mouvement reste visible dans l'historique mais n'est plus recomptabilisé. */
    @Column(name = "annule", columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean annule = false;

    @PrePersist
    protected void onCreate() {
        if (dateMouvement == null) dateMouvement = LocalDateTime.now();
    }
}
