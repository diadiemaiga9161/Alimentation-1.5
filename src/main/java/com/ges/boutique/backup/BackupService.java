package com.ges.boutique.backup;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Sauvegarde technique complète de la base MySQL (dump via mysqldump + compression
 * gzip + rotation). Voir BackupServiceImpl pour le détail. Utilisé par :
 * - le scheduler interne (sauvegarde automatique quotidienne, voir backup.cron) ;
 * - BackupController (déclenchement manuel + listing + téléchargement).
 */
public interface BackupService {

    /**
     * Pattern strict des noms de fichiers de sauvegarde générés par ce service
     * (ex: backup-alimentation-2026-08-27-030000.sql.gz). Réutilisé par le contrôleur
     * pour valider le paramètre nomFichier de GET /api/backup/telecharger/{nomFichier}
     * avant toute résolution de chemin sur le disque.
     */
    Pattern NOM_FICHIER_PATTERN = Pattern.compile("^backup-[a-zA-Z0-9_\\-]+\\.sql(\\.gz)?$");

    /**
     * Déclenche une sauvegarde complète de la base (dump + compression + rotation).
     * Ne lance JAMAIS d'exception : toute erreur technique (mysqldump introuvable,
     * échec de connexion, etc.) est capturée et reflétée dans le DTO retourné
     * (success = false, message explicite).
     */
    ResultatSauvegardeDto effectuerSauvegarde();

    /**
     * Liste les fichiers de sauvegarde présents dans backups/, du plus récent au
     * plus ancien. Ne lance jamais d'exception (dossier absent ou illisible -> liste vide).
     */
    List<BackupInfoDto> listerSauvegardes();
}
