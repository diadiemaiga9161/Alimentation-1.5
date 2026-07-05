package com.ges.boutique.depense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeDepenseRepository extends JpaRepository<TypeDepense, Long> {
    boolean existsByNom(String nom);
}
