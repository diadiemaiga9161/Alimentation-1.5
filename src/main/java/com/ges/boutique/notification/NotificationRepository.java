package com.ges.boutique.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByLuFalseOrderByDateCreationDesc();

    List<Notification> findAllByOrderByDateCreationDesc();

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.lu = false")
    long countNonLues();

    boolean existsByTypeAndReferenceIdAndReferenceTypeAndLuFalse(String type, Long referenceId, String referenceType);
}
