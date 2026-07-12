package com.ges.boutique.email;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void demanderReset(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun compte trouvé avec cet email"));

        // Supprimer les anciens tokens de cet utilisateur
        tokenRepository.supprimerTokensExpires(LocalDateTime.now());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUtilisateur(utilisateur);
        resetToken.setDateExpiration(LocalDateTime.now().plusHours(1));
        tokenRepository.save(resetToken);

        emailService.envoyerResetPassword(utilisateur.getEmail(), utilisateur.getNomComplet(), token);
        log.info("Token reset généré pour {}", email);
    }

    @Transactional
    public void reinitialiserPassword(String token, String nouveauPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Lien de réinitialisation invalide ou expiré"));

        if (resetToken.isUtilise()) {
            throw new IllegalArgumentException("Ce lien a déjà été utilisé");
        }
        if (resetToken.isExpire()) {
            throw new IllegalArgumentException("Ce lien a expiré. Veuillez faire une nouvelle demande.");
        }
        if (nouveauPassword == null || nouveauPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }

        Utilisateur utilisateur = resetToken.getUtilisateur();
        utilisateur.setPassword(passwordEncoder.encode(nouveauPassword));
        utilisateurRepository.save(utilisateur);

        resetToken.setUtilise(true);
        tokenRepository.save(resetToken);
        log.info("Mot de passe réinitialisé pour {}", utilisateur.getEmail());
    }

    public boolean validerToken(String token) {
        return tokenRepository.findByToken(token)
                .map(t -> !t.isUtilise() && !t.isExpire())
                .orElse(false);
    }
}
