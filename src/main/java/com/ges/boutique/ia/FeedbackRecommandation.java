package com.ges.boutique.ia;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_recommandation")
@Data
public class FeedbackRecommandation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UUID de la RecommandationIA correspondante — sert de clé externe logique
     * pour retrouver ce feedback depuis le client via /api/ia/feedback/{referenceId}
     */
    @Column(name = "reference_id", unique = true)
    private String referenceId;

    @Column(nullable = false)
    private String typeRecommandation; // REAPPRO, CONTACT_CLIENT, PRIX, PLANNING, STOCK

    @Column(columnDefinition = "TEXT")
    private String payload; // JSON des données de la recommandation

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatutFeedback statut = StatutFeedback.EN_ATTENTE;

    @Column(name = "impact_mesure")
    private Double impactMesure; // % amélioration mesurée après suivi

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_retour")
    private LocalDateTime dateRetour;
}
