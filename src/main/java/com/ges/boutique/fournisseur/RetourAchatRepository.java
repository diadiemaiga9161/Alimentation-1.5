package com.ges.boutique.fournisseur;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RetourAchatRepository extends JpaRepository<RetourAchat, Long> {
    List<RetourAchat> findByFournisseurIdOrderByDateRetourDesc(Long fournisseurId);
    List<RetourAchat> findByAchatIdOrderByDateRetourDesc(Long achatId);
}
