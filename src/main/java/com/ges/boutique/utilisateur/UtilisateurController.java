package com.ges.boutique.utilisateur;

import com.ges.boutique.email.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final EmailService emailService;

    @Value("${spring.application.name:MG Boutique}")
    private String boutiqueName;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir tous les utilisateurs")
    public ResponseEntity<List<Utilisateur>> obtenirTousLesUtilisateurs() {
        return ResponseEntity.ok(utilisateurService.obtenirTousLesUtilisateurs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir un utilisateur par ID")
    public ResponseEntity<Utilisateur> obtenirUtilisateurParId(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.obtenirUtilisateurParId(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir l'utilisateur connecté")
    public ResponseEntity<UtilisateurDto> obtenirUtilisateurConnecte(Principal principal) {
        String username = principal.getName();
        Utilisateur utilisateur = utilisateurService.obtenirUtilisateurParUsername(username);

        UtilisateurDto dto = new UtilisateurDto();
        dto.setId(utilisateur.getId());
        dto.setUsername(utilisateur.getUsername());
        dto.setNomComplet(utilisateur.getNomComplet());
        dto.setEmail(utilisateur.getEmail());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setRole(utilisateur.getRole());
        dto.setActif(utilisateur.isActif());
        dto.setPhoto(utilisateur.getPhoto());

        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un nouvel utilisateur")
    public ResponseEntity<Utilisateur> creerUtilisateur(@RequestBody Utilisateur utilisateur) {
        Utilisateur cree = utilisateurService.creerUtilisateur(utilisateur);
        if (cree.getEmail() != null && !cree.getEmail().isBlank()) {
            emailService.envoyerBienvenueVendeur(cree.getEmail(), cree.getNomComplet(), cree.getUsername(), boutiqueName);
        }
        return ResponseEntity.ok(cree);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un utilisateur")
    public ResponseEntity<Utilisateur> modifierUtilisateur(
            @PathVariable Long id,
            @RequestBody Utilisateur utilisateurDetails) {
        return ResponseEntity.ok(utilisateurService.modifierUtilisateur(id, utilisateurDetails));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<Void> supprimerUtilisateur(@PathVariable Long id) {
        utilisateurService.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/photo")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Mettre à jour la photo de profil")
    public ResponseEntity<Map<String, Object>> mettreAJourPhoto(
            @RequestBody Map<String, String> body,
            Principal principal) {
        Utilisateur utilisateur = utilisateurService.obtenirUtilisateurParUsername(principal.getName());
        Utilisateur updated = utilisateurService.mettreAJourPhoto(utilisateur.getId(), body.get("photo"));
        return ResponseEntity.ok(Map.of("success", true, "photo", updated.getPhoto() != null ? updated.getPhoto() : ""));
    }
}