package com.ges.boutique.securite;

import com.ges.boutique.utilisateur.AuthRequest;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "API d'authentification")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UtilisateurService utilisateurService;

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Récupérer l'utilisateur complet
        Utilisateur utilisateur = utilisateurService.obtenirUtilisateurParUsername(userDetails.getUsername());

        // Utiliser la nouvelle méthode avec l'ID dans le token
        String token = jwtUtil.generateTokenWithId(
                userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority(),
                utilisateur.getId()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", userDetails.getUsername());
        response.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        response.put("nomComplet", utilisateur.getNomComplet());
        response.put("email", utilisateur.getEmail());
        response.put("telephone", utilisateur.getTelephone());
        response.put("id", utilisateur.getId()); // ID dans la réponse JSON

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir les informations de l'utilisateur connecté")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Principal principal) {
        String username = principal.getName();
        Utilisateur utilisateur = utilisateurService.obtenirUtilisateurParUsername(username);

        Map<String, Object> response = new HashMap<>();
        response.put("id", utilisateur.getId());
        response.put("username", utilisateur.getUsername());
        response.put("nomComplet", utilisateur.getNomComplet());
        response.put("email", utilisateur.getEmail());
        response.put("telephone", utilisateur.getTelephone());
        response.put("role", utilisateur.getRole().name());
        response.put("actif", utilisateur.isActif());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/profil")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Obtenir le profil de l'utilisateur connecté")
    public ResponseEntity<Utilisateur> obtenirMonProfil(Principal principal) {
        String username = principal.getName();
        Utilisateur utilisateur = utilisateurService.obtenirUtilisateurParUsername(username);
        return ResponseEntity.ok(utilisateur);
    }

    @PutMapping("/profil")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Modifier le profil de l'utilisateur connecté")
    public ResponseEntity<Utilisateur> modifierMonProfil(
            Principal principal,
            @RequestBody Utilisateur utilisateurDetails) {
        String username = principal.getName();
        Utilisateur utilisateur = utilisateurService.obtenirUtilisateurParUsername(username);

        Utilisateur updated = utilisateurService.modifierUtilisateur(utilisateur.getId(), utilisateurDetails);
        return ResponseEntity.ok(updated);
    }
}