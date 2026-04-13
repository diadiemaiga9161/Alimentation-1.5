package com.ges.boutique.caisse;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaisseDto {
    private Long id;
    private Double soldeActuel;
    private Double soldeInitial;
    private Double soldeSysteme;
    private Double soldeReel;
    private Double ecart;
    private Double totalEntrees;
    private Double totalSorties;
    private LocalDateTime derniereOperation;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateFermeture;
    private boolean estOuverte;
    private boolean verifiee;
    private LocalDateTime dateVerification;
    private String utilisateurVerification;
    private Integer nombreOperations;
}
