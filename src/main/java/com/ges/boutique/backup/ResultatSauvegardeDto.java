package com.ges.boutique.backup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Résultat d'une tentative de sauvegarde (déclenchée par le scheduler ou par
 * POST /api/backup/declencher). Ne contient jamais d'exception : en cas d'échec,
 * success = false et message décrit la cause en clair.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultatSauvegardeDto {

    private boolean success;
    private String message;
    private String nomFichier;
    private Long tailleOctets;
    private LocalDateTime dateCreation;

    public static ResultatSauvegardeDto succes(String nomFichier, long tailleOctets, LocalDateTime dateCreation) {
        return new ResultatSauvegardeDto(true, "Sauvegarde effectuée avec succès", nomFichier, tailleOctets, dateCreation);
    }

    public static ResultatSauvegardeDto echec(String message) {
        return new ResultatSauvegardeDto(false, message, null, null, null);
    }
}
