package com.ges.boutique.utilisateur;

import lombok.Data;

/** Requête de la case à cocher "Ce vendeur est aussi employé" du formulaire vendeur. */
@Data
public class LienEmployeRequest {
    private boolean estEmploye;
    private String poste;
    private Double salaireMensuel;
}
