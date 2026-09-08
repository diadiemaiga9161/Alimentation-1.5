package com.ges.boutique.permission;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une ligne par permission accordable au vendeur (voir CleVendeur). Absence de ligne
 * pour une clé = permission INACTIVE par défaut (contraire du système de fonctionnalités
 * avancées) : le vendeur n'a accès à rien de plus tant que l'admin de la boutique n'a
 * rien accordé explicitement.
 */
@Entity
@Table(name = "permission_vendeur")
@Getter
@Setter
@NoArgsConstructor
public class PermissionVendeur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // columnDefinition forcé en VARCHAR (pas juste length=40) : sans ça, Hibernate crée un
    // ENUM MySQL natif figé sur les valeurs de CleVendeur au moment de la création de la
    // table, que ddl-auto=update n'élargit ensuite JAMAIS — toute nouvelle clé ajoutée
    // plus tard planterait en écriture ("Data truncated for column 'cle'"), bug réel
    // rencontré sur fonctionnalite_boutique/CleFonctionnalite (voir migration V106).
    @Enumerated(EnumType.STRING)
    @Column(name = "cle", nullable = false, unique = true, columnDefinition = "VARCHAR(40)")
    private CleVendeur cle;

    @Column(name = "actif", columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean actif = false;

    public PermissionVendeur(CleVendeur cle, boolean actif) {
        this.cle = cle;
        this.actif = actif;
    }
}
