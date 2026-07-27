package com.ges.boutique.fournisseur;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AchatFournisseurRepository extends JpaRepository<AchatFournisseur, Long> {
    List<AchatFournisseur> findByFournisseurIdOrderByDateAchatDesc(Long fournisseurId);
    List<AchatFournisseur> findByStatut(StatutAchat statut);

    @Query("SELECT a FROM AchatFournisseur a WHERE a.fournisseur.id = :fournisseurId AND a.statut != :statut")
    List<AchatFournisseur> findByFournisseurIdAndStatutNot(@Param("fournisseurId") Long fournisseurId, @Param("statut") StatutAchat statut);

    List<AchatFournisseur> findByFournisseurIdAndDateAchatBetweenOrderByDateAchatDesc(
            Long fournisseurId, LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT a FROM AchatFournisseur a WHERE a.fournisseur.id = :fournisseurId AND a.statut = :statut ORDER BY a.dateAchat ASC")
    List<AchatFournisseur> findAchatsNonPayesByFournisseurId(@Param("fournisseurId") Long fournisseurId,
                                                              @Param("statut") StatutAchat statut);
}