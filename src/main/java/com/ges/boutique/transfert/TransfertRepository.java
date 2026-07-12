package com.ges.boutique.transfert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransfertRepository extends JpaRepository<TransfertStock, Long> {
    List<TransfertStock> findAllByOrderByDateCreationDesc();
    Optional<TransfertStock> findByNumeroTransfert(String numero);
    List<TransfertStock> findByStatutOrderByDateCreationDesc(StatutTransfert statut);
}
