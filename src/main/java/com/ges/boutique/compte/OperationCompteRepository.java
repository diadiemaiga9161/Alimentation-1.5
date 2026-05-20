package com.ges.boutique.compte;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OperationCompteRepository extends JpaRepository<OperationCompte, Long> {
    List<OperationCompte> findByCompteIdOrderByDateOperationDesc(Long compteId);
}
