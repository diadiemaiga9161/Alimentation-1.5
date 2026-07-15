package com.ges.boutique.ia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRecommandationRepository extends JpaRepository<FeedbackRecommandation, Long> {

    Optional<FeedbackRecommandation> findByReferenceId(String referenceId);

    List<FeedbackRecommandation> findByTypeRecommandationAndStatut(String typeRecommandation, StatutFeedback statut);

    long countByStatut(StatutFeedback statut);

    /**
     * Moyenne de l'impact mesuré pour un type donné — utilisée pour l'auto-apprentissage.
     */
    @Query("SELECT AVG(f.impactMesure) FROM FeedbackRecommandation f " +
           "WHERE f.typeRecommandation = :type AND f.impactMesure IS NOT NULL")
    Double avgImpactMesureByTypeRecommandation(@Param("type") String type);
}
