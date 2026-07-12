package com.ges.boutique.fournisseur;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AchatPaiementLienRepository extends JpaRepository<AchatPaiementLien, Long> {

    List<AchatPaiementLien> findByAchatId(Long achatId);

    List<AchatPaiementLien> findByPaiementId(Long paiementId);

    @Query("SELECT COALESCE(SUM(l.montantApplique), 0) FROM AchatPaiementLien l WHERE l.achatId = :achatId")
    Double getTotalPayeParAchat(@Param("achatId") Long achatId);

    boolean existsByAchatIdAndPaiementId(Long achatId, Long paiementId);
}