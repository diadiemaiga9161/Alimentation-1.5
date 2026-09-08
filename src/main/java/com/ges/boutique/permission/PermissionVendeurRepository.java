package com.ges.boutique.permission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionVendeurRepository extends JpaRepository<PermissionVendeur, Long> {
    Optional<PermissionVendeur> findByCle(CleVendeur cle);
}
