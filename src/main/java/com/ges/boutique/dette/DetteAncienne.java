package com.ges.boutique.dette;

import com.ges.boutique.client.Client;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dettes_anciennes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetteAncienne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private Double montantInitial;

    @Column(nullable = false)
    private Double montantRestant;

    @Column(nullable = false)
    private LocalDate dateCredit;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean estReglee = false;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_dernier_reglement")
    private LocalDateTime dateDernierReglement;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        if (montantRestant == null) {
            montantRestant = montantInitial;
        }
        if (estReglee == null) {
            estReglee = false;
        }
    }

    public Double getMontantPaye() {
        return montantInitial - montantRestant;
    }
}