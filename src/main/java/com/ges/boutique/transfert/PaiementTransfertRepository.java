package com.ges.boutique.transfert;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaiementTransfertRepository extends JpaRepository<PaiementTransfert, Long> {
    List<PaiementTransfert> findByTransfertIdOrderByDatePaiementDesc(Long transfertId);
}
