package com.ges.boutique.depot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepotClientRepository extends JpaRepository<DepotClient, Long> {

    List<DepotClient> findAllByOrderByNomAsc();

    Optional<DepotClient> findByNumero(String numero);

    List<DepotClient> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrNumeroContaining(
            String nom, String prenom, String numero);
}
