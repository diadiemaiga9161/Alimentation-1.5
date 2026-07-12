package com.ges.boutique.caisse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransfertCaisseBanqueRepository extends JpaRepository<TransfertCaisseBanque, Long> {
    List<TransfertCaisseBanque> findByCompteIdOrderByDateTransfertDesc(Long compteId);
    List<TransfertCaisseBanque> findByDateTransfertBetween(LocalDateTime debut, LocalDateTime fin);
    List<TransfertCaisseBanque> findByOperationCaisseId(Long operationCaisseId);
}