package com.ges.boutique.client;

import com.ges.boutique.exception.RessourceIntrouvableException;
import com.ges.boutique.journalaudit.JournalAuditService;
import com.ges.boutique.journalaudit.TypeActionAudit;
import com.ges.boutique.utilisateur.Utilisateur;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final JournalAuditService journalAuditService;

    @Override
    @Transactional
    @CacheEvict(value = "clients", allEntries = true)
    public Client creerClient(Client client) {
        preparerEtValiderClient(client);
        if (clientRepository.findByNumeroTelephone(client.getNumeroTelephone()).isPresent()) {
            throw new IllegalArgumentException("Un client avec ce numéro de téléphone existe déjà");
        }
        return clientRepository.save(client);
    }

    @Override
    @Transactional
    @CacheEvict(value = "clients", allEntries = true)
    public Client modifierClient(Long id, Client client) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Client non trouvé avec l'ID: " + id));

        preparerEtValiderClient(client);

        Optional<Client> clientWithSamePhone = clientRepository.findByNumeroTelephone(client.getNumeroTelephone());
        if (clientWithSamePhone.isPresent() && !clientWithSamePhone.get().getId().equals(id)) {
            throw new IllegalArgumentException("Un client avec ce numéro de téléphone existe déjà");
        }

        existingClient.setNom(client.getNom());
        existingClient.setPrenom(client.getPrenom());
        existingClient.setNumeroTelephone(client.getNumeroTelephone());
        existingClient.setAdresse(client.getAdresse());
        existingClient.setEmail(client.getEmail());
        existingClient.setPartenaire(client.isPartenaire());

        return clientRepository.save(existingClient);
    }

    @Override
    @Transactional
    @CacheEvict(value = "clients", allEntries = true)
    public void supprimerClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Client non trouvé avec l'ID: " + id));
        if (clientRepository.countVentesActivesByClientId(id) > 0) {
            throw new IllegalStateException("Impossible de supprimer un client associé à des ventes");
        }
        clientRepository.deleteById(id);

        Utilisateur auteur = getUtilisateurCourantAudit();
        journalAuditService.enregistrer(
                auteur != null ? auteur.getId() : null,
                auteur != null ? auteur.getNomComplet() : null,
                TypeActionAudit.SUPPRESSION_CLIENT,
                "Client #" + client.getId() + " (" + client.getNom() + " " + client.getPrenom() + ") supprimé");
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
    public Optional<Client> trouverParId(Long id) {
        return clientRepository.findById(id);
    }

    @Override
    public Optional<Client> trouverParNumeroTelephone(String numeroTelephone) {
        if (numeroTelephone == null || numeroTelephone.trim().isEmpty()) {
            return Optional.empty();
        }
        return clientRepository.findByNumeroTelephone(numeroTelephone.trim());
    }

    @Override
    @Cacheable("clients")
    public List<Client> trouverTous() {
        return clientRepository.findAll();
    }

    @Override
    public List<Client> rechercherParNomOuPrenom(String nom, String prenom) {
        String searchNom = nom != null ? nom : "";
        String searchPrenom = prenom != null ? prenom : "";
        return clientRepository.findByNomContainingOrPrenomContaining(searchNom, searchPrenom);
    }

    @Override
    public List<Client> trouverTousTriesParDateCreation() {
        return clientRepository.findAllOrderByDateCreationDesc();
    }

    @Override
    public List<Object[]> trouverTopClientsParMontant() {
        return clientRepository.findTopClientsByMontant();
    }

    @Override
    public List<Client> getPartenaires() {
        return clientRepository.findByPartenaireTrue();
    }

    private void preparerEtValiderClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Le client est requis");
        }
        if (client.getNom() == null || client.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du client est requis");
        }
        if (client.getNumeroTelephone() == null || client.getNumeroTelephone().trim().isEmpty()) {
            throw new IllegalArgumentException("Le numéro de téléphone du client est requis");
        }

        client.setNom(client.getNom().trim());
        client.setPrenom(client.getPrenom() != null ? client.getPrenom().trim() : "");
        client.setNumeroTelephone(client.getNumeroTelephone().trim());
    }
}