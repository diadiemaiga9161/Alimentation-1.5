package com.ges.boutique.client;

import java.util.List;
import java.util.Optional;

public interface ClientService {

    Client creerClient(Client client);

    Client modifierClient(Long id, Client client);

    void supprimerClient(Long id);

    Optional<Client> trouverParId(Long id);

    Optional<Client> trouverParNumeroTelephone(String numeroTelephone);

    List<Client> trouverTous();

    List<Client> rechercherParNomOuPrenom(String nom, String prenom);

    List<Client> trouverTousTriesParDateCreation();

    List<Object[]> trouverTopClientsParMontant();
}