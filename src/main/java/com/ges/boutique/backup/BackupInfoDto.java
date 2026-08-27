package com.ges.boutique.backup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Représente un fichier de sauvegarde présent dans le dossier backups/, tel que
 * renvoyé par GET /api/backup/liste.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupInfoDto {

    private String nomFichier;
    private LocalDateTime dateCreation;
    private long tailleOctets;
}
