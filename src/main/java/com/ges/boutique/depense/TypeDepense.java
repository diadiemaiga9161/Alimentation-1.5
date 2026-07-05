package com.ges.boutique.depense;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "type_depenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypeDepense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    public TypeDepense(String nom) {
        this.nom = nom;
    }
}
