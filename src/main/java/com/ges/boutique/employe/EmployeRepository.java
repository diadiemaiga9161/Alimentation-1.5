package com.ges.boutique.employe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {
    List<Employe> findByStatutOrderByNomAsc(StatutEmploye statut);
    List<Employe> findAllByOrderByNomAsc();
    boolean existsByNomAndPrenom(String nom, String prenom);
}
