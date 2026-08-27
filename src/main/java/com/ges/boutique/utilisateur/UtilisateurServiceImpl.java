package com.ges.boutique.utilisateur;

import com.ges.boutique.employe.Employe;
import com.ges.boutique.employe.EmployeRepository;
import com.ges.boutique.employe.StatutEmploye;
import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.journalaudit.JournalAuditService;
import com.ges.boutique.journalaudit.TypeActionAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JournalAuditService journalAuditService;

    @Override
    @Transactional
    public Utilisateur creerUtilisateur(Utilisateur utilisateur) {
        if (utilisateurRepository.existsByUsername(utilisateur.getUsername())) {
            throw new RuntimeException("Le nom d'utilisateur existe déjà");
        }
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            throw new RuntimeException("L'email existe déjà");
        }

        utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
        return utilisateurRepository.save(utilisateur);
    }

    @Override
    @Transactional
    public Utilisateur modifierUtilisateur(Long id, Utilisateur utilisateurDetails) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé avec l'ID: " + id));

        if (utilisateurDetails.getNomComplet() != null) {
            utilisateur.setNomComplet(utilisateurDetails.getNomComplet());
        }
        if (utilisateurDetails.getUsername() != null && !utilisateurDetails.getUsername().equals(utilisateur.getUsername())) {
            if (utilisateurRepository.existsByUsername(utilisateurDetails.getUsername())) {
                throw new RuntimeException("Ce nom d'utilisateur existe déjà");
            }
            utilisateur.setUsername(utilisateurDetails.getUsername());
        }
        if (utilisateurDetails.getEmail() != null && !utilisateurDetails.getEmail().equals(utilisateur.getEmail())) {
            if (utilisateurRepository.existsByEmail(utilisateurDetails.getEmail())) {
                throw new RuntimeException("L'email existe déjà");
            }
            utilisateur.setEmail(utilisateurDetails.getEmail());
        }
        if (utilisateurDetails.getTelephone() != null) {
            utilisateur.setTelephone(utilisateurDetails.getTelephone());
        }
        if (utilisateurDetails.getPassword() != null) {
            utilisateur.setPassword(passwordEncoder.encode(utilisateurDetails.getPassword()));
        }
        RoleUtilisateur ancienRole = utilisateur.getRole();
        boolean roleChange = utilisateurDetails.getRole() != null && utilisateurDetails.getRole() != ancienRole;
        if (utilisateurDetails.getRole() != null) {
            utilisateur.setRole(utilisateurDetails.getRole());
        }
        // NB: "actif" n'est volontairement pas repris ici — c'est un booléen primitif
        // (jamais absent du JSON désérialisé, toujours true ou false), donc l'appliquer
        // sans condition écraserait le statut à chaque modification, y compris pour les
        // formulaires qui ne connaissent pas ce champ. Le gel/dégel passe par un endpoint
        // dédié (changerStatutUtilisateur) qui ne touche que ce champ.

        Utilisateur saved = utilisateurRepository.save(utilisateur);

        if (roleChange) {
            Utilisateur auteur = getUtilisateurCourantAudit();
            journalAuditService.enregistrer(
                    auteur != null ? auteur.getId() : null,
                    auteur != null ? auteur.getNomComplet() : null,
                    TypeActionAudit.MODIFICATION_ROLE_UTILISATEUR,
                    "Utilisateur #" + saved.getId() + " (" + saved.getUsername() + ") : rôle " + ancienRole
                            + " -> " + saved.getRole());
        }

        return saved;
    }

    /**
     * Utilisateur actuellement authentifié (contexte de sécurité Spring), pour les besoins
     * du journal d'audit — même mécanisme que celui déjà utilisé ailleurs dans le projet
     * (ex: DepenseController.getUserId(), ProduitNiveauController.getUserId()).
     */
    private Utilisateur getUtilisateurCourantAudit() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Utilisateur u) {
            return u;
        }
        return null;
    }

    @Override
    @Transactional
    public Utilisateur changerStatutUtilisateur(Long id, boolean actif) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé avec l'ID: " + id));
        utilisateur.setActif(actif);
        return utilisateurRepository.save(utilisateur);
    }

    @Override
    @Transactional
    public Utilisateur gererLienEmploye(Long id, LienEmployeRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé avec l'ID: " + id));

        if (request.isEstEmploye()) {
            Employe employe;
            if (utilisateur.getEmployeId() != null) {
                // Déjà lié une fois : on met à jour la même fiche (pas de doublon) et on la réactive.
                employe = employeRepository.findById(utilisateur.getEmployeId())
                        .orElse(null);
            } else {
                employe = null;
            }
            if (employe == null) {
                employe = new Employe();
                employe.setDateEmbauche(LocalDate.now());
            }
            employe.setNom(utilisateur.getNomComplet());
            employe.setTelephone(utilisateur.getTelephone());
            employe.setPoste(request.getPoste() != null && !request.getPoste().isBlank() ? request.getPoste() : "Vendeur");
            employe.setSalaireMensuel(request.getSalaireMensuel() != null ? request.getSalaireMensuel() : 0.0);
            employe.setStatut(StatutEmploye.ACTIF);
            employe = employeRepository.save(employe);
            utilisateur.setEmployeId(employe.getId());
        } else if (utilisateur.getEmployeId() != null) {
            // Case décochée : on ne supprime pas l'historique de paie, on désactive juste la fiche
            // (même logique que "désactiver un employé" ailleurs dans l'appli).
            employeRepository.findById(utilisateur.getEmployeId()).ifPresent(employe -> {
                employe.setStatut(StatutEmploye.INACTIF);
                employeRepository.save(employe);
            });
        }

        return utilisateurRepository.save(utilisateur);
    }

    @Override
    @Transactional
    public void supprimerUtilisateur(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé avec l'ID: " + id));
        utilisateur.setActif(false);
        utilisateurRepository.save(utilisateur);
    }

    @Override
    public Utilisateur obtenirUtilisateurParId(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé avec l'ID: " + id));
    }

    @Override
    public List<Utilisateur> obtenirTousLesUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    @Override
    public Utilisateur obtenirUtilisateurParUsername(String username) {
        return utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé avec le nom d'utilisateur: " + username));
    }

    @Override
    public long compterUtilisateurs() {
        return utilisateurRepository.count();
    }

    @Override
    @Transactional
    public Utilisateur mettreAJourPhoto(Long id, String photo) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur non trouvé avec l'ID: " + id));
        utilisateur.setPhoto(photo);
        return utilisateurRepository.save(utilisateur);
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Chercher par username, puis email, puis téléphone
        return utilisateurRepository.findByUsername(identifier)
                .or(() -> utilisateurRepository.findByEmail(identifier))
                .or(() -> utilisateurRepository.findByTelephone(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + identifier));
    }
}