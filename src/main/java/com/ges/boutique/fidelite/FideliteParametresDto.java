package com.ges.boutique.fidelite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Taux configurables par l'admin de la boutique — voir Boutique.fideliteMontantParPoint/fidelitePointValeur. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FideliteParametresDto {
    /** FCFA dépensés pour gagner 1 point. */
    private Double montantParPoint;
    /** Valeur en FCFA d'1 point au moment de l'utiliser. */
    private Double pointValeur;
}
