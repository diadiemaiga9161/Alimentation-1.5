package com.ges.boutique.transfert;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "boutiques_partenaires")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoutiquePartenaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String url;

    @Column
    private String description;

    @Column(nullable = false)
    private boolean actif = true;
}
