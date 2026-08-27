package com.ges.boutique.journalaudit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Journal d'audit des actions sensibles (suppression de vente, modification de prix,
 * annulation/suppression de transfert, suppression client/fournisseur, changement de rôle,
 * suppression de crédit...). Consultation réservée aux admins (voir SecurityConfig).
 *
 * utilisateurNom est volontairement dénormalisé (pas de jointure vers Utilisateur) pour que
 * l'historique reste lisible même si l'utilisateur est ensuite supprimé/désactivé.
 */
@Entity
@Table(name = "journal_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(name = "utilisateur_nom")
    private String utilisateurNom;

    // ENUM stocké en VARCHAR (convention du projet — évite les migrations de type)
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private TypeActionAudit action;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;

    @PrePersist
    protected void onCreate() {
        if (dateAction == null) {
            dateAction = LocalDateTime.now();
        }
    }
}
