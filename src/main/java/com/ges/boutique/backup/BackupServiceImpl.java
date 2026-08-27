package com.ges.boutique.backup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Sauvegarde technique complète de la base MySQL.
 *
 * Principe : appelle l'utilitaire externe `mysqldump` (aucune lib tierce, aucune
 * dépendance Maven ajoutée) via ProcessBuilder, redirige sa sortie standard vers
 * un fichier .sql dans backups/, compresse ce fichier en .gz, puis applique une
 * rotation (ne garde que les N derniers fichiers).
 *
 * IMPORTANT (limitation connue) : sur le jar Windows local
 * (application-boutique-local.properties), `mysqldump` n'est très probablement
 * PAS présent dans le PATH Windows du poste. Dans ce cas, effectuerSauvegarde()
 * échoue proprement (success=false, message explicite dans les logs et dans le
 * DTO retourné) — c'est acceptable, il ne s'agit que d'une tâche de fond et
 * l'application continue de fonctionner normalement.
 */
@Slf4j
@Service
public class BackupServiceImpl implements BackupService {

    private static final String DOSSIER_BACKUPS = "backups";
    private static final ZoneId FUSEAU = ZoneId.of("Africa/Abidjan");
    private static final DateTimeFormatter FORMAT_HORODATAGE = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    // Nom volontairement "retention-jours" (voir application.properties) mais interprété
    // ici comme "nombre de fichiers a conserver" : comme il y a au plus une sauvegarde par
    // jour, les deux notions coincident en pratique.
    @Value("${backup.retention-jours:14}")
    private int retentionFichiers;

    /**
     * Sauvegarde automatique quotidienne — voir backup.cron dans application.properties
     * (par défaut : tous les jours à 3h du matin, fuseau Africa/Abidjan).
     */
    @Scheduled(cron = "${backup.cron:0 0 3 * * *}")
    public void sauvegardeAutomatiqueProgrammee() {
        log.info("Sauvegarde automatique programmée : démarrage");
        ResultatSauvegardeDto resultat = effectuerSauvegarde();
        if (resultat.isSuccess()) {
            log.info("Sauvegarde automatique programmée réussie : fichier={}, taille={} octets",
                    resultat.getNomFichier(), resultat.getTailleOctets());
        } else {
            log.error("Sauvegarde automatique programmée échouée : {}", resultat.getMessage());
        }
    }

    @Override
    public ResultatSauvegardeDto effectuerSauvegarde() {
        // Toute la méthode est volontairement dans un unique bloc try/catch global :
        // un échec de sauvegarde (mysqldump absent, erreur de connexion MySQL, disque plein...)
        // ne doit JAMAIS faire planter l'appelant (ni le scheduler, ni la requête HTTP manuelle).
        try {
            String nomBase = extraireNomBase(datasourceUrl);
            String hote = extraireHote(datasourceUrl);

            Path dossier = Paths.get(DOSSIER_BACKUPS);
            Files.createDirectories(dossier);

            String horodatage = LocalDateTime.now(FUSEAU).format(FORMAT_HORODATAGE);
            String nomFichierSql = "backup-" + nomBase + "-" + horodatage + ".sql";
            Path cheminSql = dossier.resolve(nomFichierSql);

            boolean dumpReussi = lancerMysqldump(hote, nomBase, cheminSql);
            if (!dumpReussi) {
                supprimerSiPresent(cheminSql);
                return ResultatSauvegardeDto.echec(
                        "Échec de mysqldump : impossible de générer le dump SQL. Vérifiez que "
                                + "l'utilitaire 'mysqldump' est installé et accessible dans le PATH du "
                                + "système, et que les identifiants de connexion sont valides (voir les "
                                + "logs de l'application pour le détail de l'erreur).");
            }

            Path cheminFinal = compresser(cheminSql);
            long taille = Files.size(cheminFinal);
            LocalDateTime dateCreation = LocalDateTime.now(FUSEAU);

            appliquerRotation(dossier);

            return ResultatSauvegardeDto.succes(cheminFinal.getFileName().toString(), taille, dateCreation);
        } catch (Exception e) {
            log.error("Échec inattendu de la sauvegarde de la base : {}", e.getMessage(), e);
            return ResultatSauvegardeDto.echec("Erreur inattendue lors de la sauvegarde : " + e.getMessage());
        }
    }

    @Override
    public List<BackupInfoDto> listerSauvegardes() {
        Path dossier = Paths.get(DOSSIER_BACKUPS);
        if (!Files.isDirectory(dossier)) {
            return List.of();
        }
        try (Stream<Path> flux = Files.list(dossier)) {
            return flux
                    .filter(Files::isRegularFile)
                    .filter(p -> NOM_FICHIER_PATTERN.matcher(p.getFileName().toString()).matches())
                    .map(this::versBackupInfoDto)
                    .sorted(Comparator.comparing(BackupInfoDto::getDateCreation,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Impossible de lister le contenu du dossier {} : {}", dossier, e.getMessage());
            return List.of();
        }
    }

    // ==================== Détail technique ====================

    /**
     * Lance mysqldump en redirigeant sa sortie standard directement vers le fichier .sql
     * cible, et capture stderr pour le loguer en cas d'échec. Le mot de passe n'est
     * JAMAIS passé en argument de ligne de commande (ce qui le rendrait visible dans la
     * liste des processus du système) : il est transmis via la variable d'environnement
     * MYSQL_PWD, lue nativement par mysqldump. Si le mot de passe est vide (ex: root local
     * sans mot de passe sur ce PC Windows), MYSQL_PWD n'est simplement pas définie, ce qui
     * évite tout prompt interactif bloquant.
     */
    private boolean lancerMysqldump(String hote, String nomBase, Path cheminSortie) {
        List<String> commande = new ArrayList<>();
        commande.add("mysqldump");
        commande.add("-h");
        commande.add(hote);
        commande.add("-u");
        commande.add(datasourceUsername);
        commande.add("--single-transaction");
        commande.add("--routines");
        commande.add(nomBase);

        try {
            ProcessBuilder pb = new ProcessBuilder(commande);
            pb.redirectOutput(cheminSortie.toFile());
            if (datasourcePassword != null && !datasourcePassword.isBlank()) {
                pb.environment().put("MYSQL_PWD", datasourcePassword);
            }

            Process process = pb.start();

            String erreurs;
            try (InputStream errStream = process.getErrorStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(errStream))) {
                erreurs = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }

            boolean termineATemps = process.waitFor(5, TimeUnit.MINUTES);
            if (!termineATemps) {
                process.destroyForcibly();
                log.error("mysqldump n'a pas terminé dans le délai imparti (5 minutes) — processus interrompu");
                return false;
            }

            int codeRetour = process.exitValue();
            if (codeRetour != 0) {
                log.error("mysqldump a retourné le code d'erreur {} (base={}, hôte={}) : {}",
                        codeRetour, nomBase, hote, erreurs);
                return false;
            }
            if (!erreurs.isBlank()) {
                log.warn("mysqldump a produit des messages sur stderr (code retour 0, non bloquant) : {}", erreurs);
            }
            return true;
        } catch (IOException e) {
            // Cas attendu sur le jar Windows local (application-boutique-local.properties) :
            // mysqldump n'est probablement pas dans le PATH -> échoue ici silencieusement,
            // juste loggué en erreur. Non bloquant pour le reste de l'application.
            log.error("Impossible de lancer mysqldump (probablement absent du PATH système) : {}", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Sauvegarde interrompue pendant l'exécution de mysqldump : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Compresse le fichier .sql en .gz. Si la compression échoue pour une raison
     * quelconque, on garde le .sql brut (non bloquant, simple warning en log).
     */
    private Path compresser(Path cheminSql) {
        Path cheminGz = Paths.get(cheminSql.toString() + ".gz");
        try (InputStream in = Files.newInputStream(cheminSql);
             GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(cheminGz))) {
            in.transferTo(out);
        } catch (IOException e) {
            log.warn("Échec de la compression gzip du dump SQL, conservation du fichier .sql brut : {}",
                    e.getMessage());
            supprimerSiPresent(cheminGz);
            return cheminSql;
        }

        try {
            Files.delete(cheminSql);
        } catch (IOException e) {
            log.warn("Compression réussie mais impossible de supprimer le .sql temporaire {} : {}",
                    cheminSql, e.getMessage());
        }
        return cheminGz;
    }

    /**
     * Ne garde que les N derniers fichiers de sauvegarde (N = backup.retention-jours),
     * triés par date de dernière modification. Les plus anciens sont supprimés.
     */
    private void appliquerRotation(Path dossier) {
        try {
            List<Path> fichiers;
            try (Stream<Path> flux = Files.list(dossier)) {
                fichiers = flux
                        .filter(Files::isRegularFile)
                        .filter(p -> NOM_FICHIER_PATTERN.matcher(p.getFileName().toString()).matches())
                        .sorted(Comparator.comparingLong(this::dateModificationMillis).reversed())
                        .collect(Collectors.toList());
            }

            if (fichiers.size() <= retentionFichiers) {
                return;
            }

            List<Path> aSupprimer = fichiers.subList(retentionFichiers, fichiers.size());
            for (Path fichier : aSupprimer) {
                try {
                    Files.delete(fichier);
                    log.info("Rotation des sauvegardes : suppression de l'ancien fichier {}", fichier.getFileName());
                } catch (IOException e) {
                    log.warn("Rotation des sauvegardes : impossible de supprimer {} : {}",
                            fichier.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Échec de la rotation des sauvegardes dans {} : {}", dossier, e.getMessage());
        }
    }

    private long dateModificationMillis(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private void supprimerSiPresent(Path chemin) {
        try {
            Files.deleteIfExists(chemin);
        } catch (IOException ignored) {
            // best-effort de nettoyage, non bloquant
        }
    }

    private BackupInfoDto versBackupInfoDto(Path p) {
        try {
            long taille = Files.size(p);
            LocalDateTime date = LocalDateTime.ofInstant(Files.getLastModifiedTime(p).toInstant(), FUSEAU);
            return new BackupInfoDto(p.getFileName().toString(), date, taille);
        } catch (IOException e) {
            log.warn("Impossible de lire les métadonnées du fichier de sauvegarde {} : {}", p, e.getMessage());
            return new BackupInfoDto(p.getFileName().toString(), null, 0L);
        }
    }

    /**
     * Extrait le nom de la base depuis spring.datasource.url
     * (ex: jdbc:mysql://localhost:3306/alimentation?useSSL=false... -> "alimentation").
     */
    private String extraireNomBase(String url) {
        int dernierSlash = url.lastIndexOf('/');
        String reste = url.substring(dernierSlash + 1);
        int indexParametres = reste.indexOf('?');
        return indexParametres >= 0 ? reste.substring(0, indexParametres) : reste;
    }

    /**
     * Extrait l'hôte depuis spring.datasource.url
     * (ex: jdbc:mysql://localhost:3306/alimentation... -> "localhost").
     */
    private String extraireHote(String url) {
        int indexProtocole = url.indexOf("://");
        String sansProtocole = indexProtocole >= 0 ? url.substring(indexProtocole + 3) : url;
        int indexSlash = sansProtocole.indexOf('/');
        String hotePort = indexSlash >= 0 ? sansProtocole.substring(0, indexSlash) : sansProtocole;
        int indexDeuxPoints = hotePort.indexOf(':');
        return indexDeuxPoints >= 0 ? hotePort.substring(0, indexDeuxPoints) : hotePort;
    }
}
