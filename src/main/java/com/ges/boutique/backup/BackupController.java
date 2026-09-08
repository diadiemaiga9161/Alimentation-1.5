package com.ges.boutique.backup;

import com.ges.boutique.exception.RessourceIntrouvableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sauvegarde technique complète de la base MySQL (dump + compression + rotation) —
 * réservé aux admins. Double protection : voir aussi SecurityConfig, règle sur
 * /api/backup/**, en plus des @PreAuthorize ci-dessous.
 *
 * Contrat de réponse (utilisé tel quel par les apps front) :
 * - GET  /api/backup/liste                    -> { success, sauvegardes: BackupInfoDto[], nombre }
 * - POST /api/backup/declencher                -> succès 200 { success:true, message, nomFichier, tailleOctets, dateCreation }
 *                                                  échec  500 { success:false, message }
 * - GET  /api/backup/telecharger/{nomFichier}  -> binaire (Content-Disposition: attachment) ou 400/404
 * - POST /api/backup/restaurer/{nomFichier}    -> réservé au super admin, voir BackupService.restaurer
 *                                                  succès 200 { success:true, message } | échec 400 { success:false, message }
 */
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@Tag(name = "Sauvegarde", description = "Sauvegarde technique complète de la base MySQL (admin uniquement)")
public class BackupController {

    private static final String DOSSIER_BACKUPS = "backups";

    private final BackupService backupService;

    @GetMapping("/liste")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les fichiers de sauvegarde disponibles, du plus récent au plus ancien")
    public ResponseEntity<Map<String, Object>> lister() {
        List<BackupInfoDto> sauvegardes = backupService.listerSauvegardes();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sauvegardes", sauvegardes);
        response.put("nombre", sauvegardes.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/declencher")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Déclencher une sauvegarde immédiate (synchrone)")
    public ResponseEntity<Map<String, Object>> declencher() {
        ResultatSauvegardeDto resultat = backupService.effectuerSauvegarde();

        Map<String, Object> response = new HashMap<>();
        response.put("success", resultat.isSuccess());
        response.put("message", resultat.getMessage());

        if (resultat.isSuccess()) {
            response.put("nomFichier", resultat.getNomFichier());
            response.put("tailleOctets", resultat.getTailleOctets());
            response.put("dateCreation", resultat.getDateCreation());
            return ResponseEntity.ok(response);
        }

        // Échec technique géré (ex: mysqldump introuvable) : on distingue clairement ce cas
        // d'un succès via le code HTTP 500, en plus du champ success=false dans le corps.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @GetMapping("/telecharger/{nomFichier}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Télécharger un fichier de sauvegarde existant")
    public ResponseEntity<Resource> telecharger(@PathVariable String nomFichier) {
        if (nomFichier == null
                || nomFichier.contains("..")
                || nomFichier.contains("/")
                || nomFichier.contains("\\")
                || !BackupService.NOM_FICHIER_PATTERN.matcher(nomFichier).matches()) {
            throw new IllegalArgumentException("Nom de fichier de sauvegarde invalide : " + nomFichier);
        }

        Path dossierBackups = Paths.get(DOSSIER_BACKUPS).toAbsolutePath().normalize();
        Path cheminDemande = dossierBackups.resolve(nomFichier).normalize();

        // Défense en profondeur : même si le nom a déjà été validé par regex ci-dessus, on
        // vérifie que le chemin résolu reste bien A L'INTÉRIEUR du dossier backups/ avant de
        // servir quoi que ce soit (comparaison de chemins normalisés, pas de concaténation brute).
        if (!cheminDemande.startsWith(dossierBackups)) {
            throw new IllegalArgumentException("Nom de fichier de sauvegarde invalide : " + nomFichier);
        }

        File fichier = cheminDemande.toFile();
        if (!fichier.exists() || !fichier.isFile()) {
            throw new RessourceIntrouvableException("Fichier de sauvegarde introuvable : " + nomFichier);
        }

        Resource resource = new FileSystemResource(fichier);
        MediaType type = nomFichier.endsWith(".gz")
                ? MediaType.parseMediaType("application/gzip")
                : MediaType.parseMediaType("application/octet-stream");

        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                .contentLength(fichier.length())
                .body(resource);
    }

    // Réservé au super admin (pas un rôle séparé — flag superAdmin sur le compte, voir
    // Utilisateur.java) : un admin classique de boutique ne doit jamais pouvoir écraser
    // la base actuelle avec une ancienne sauvegarde.
    @PostMapping("/restaurer/{nomFichier}")
    @PreAuthorize("hasRole('ADMIN') and authentication.principal.superAdmin")
    @Operation(summary = "Restaurer la base depuis une sauvegarde existante — réservé au super admin")
    public ResponseEntity<Map<String, Object>> restaurer(@PathVariable String nomFichier) {
        ResultatRestaurationDto resultat = backupService.restaurer(nomFichier);

        Map<String, Object> response = new HashMap<>();
        response.put("success", resultat.isSuccess());
        response.put("message", resultat.getMessage());

        return resultat.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
