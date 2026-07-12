package com.ges.boutique.depot;

import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepotClientServiceImpl implements DepotClientService {

    private final DepotClientRepository repository;

    @Override
    @Transactional
    public DepotClientDto creer(DepotClientRequest request) {
        if (request.getNom() == null || request.getNom().isBlank())
            throw new IllegalArgumentException("Le nom est obligatoire");
        if (request.getNumero() == null || request.getNumero().isBlank())
            throw new IllegalArgumentException("Le numéro de téléphone est obligatoire");

        repository.findByNumero(request.getNumero().trim()).ifPresent(c -> {
            String nom = (c.getPrenom() != null ? c.getPrenom() + " " : "") + c.getNom();
            throw new IllegalStateException("Un client avec ce numéro existe déjà : " + nom + " (" + c.getNumero() + ")");
        });

        DepotClient client = new DepotClient();
        client.setNom(request.getNom().trim());
        client.setPrenom(request.getPrenom() != null ? request.getPrenom().trim() : null);
        client.setNumero(request.getNumero().trim());
        client.setAdresse(request.getAdresse());
        client.setObservation(request.getObservation());
        return DepotClientDto.fromEntity(repository.save(client));
    }

    @Override
    @Transactional
    public DepotClientDto modifier(Long id, DepotClientRequest request) {
        DepotClient client = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Client introuvable"));
        if (request.getNom() != null && !request.getNom().isBlank())
            client.setNom(request.getNom().trim());
        if (request.getPrenom() != null)
            client.setPrenom(request.getPrenom().trim());
        if (request.getNumero() != null && !request.getNumero().isBlank())
            client.setNumero(request.getNumero().trim());
        if (request.getAdresse() != null)
            client.setAdresse(request.getAdresse());
        if (request.getObservation() != null)
            client.setObservation(request.getObservation());
        return DepotClientDto.fromEntity(repository.save(client));
    }

    @Override
    public List<DepotClientDto> getTous() {
        return repository.findAllByOrderByNomAsc().stream()
                .map(DepotClientDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<DepotClientDto> rechercher(String query) {
        return repository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrNumeroContaining(
                query, query, query).stream()
                .map(DepotClientDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public DepotClientDto getById(Long id) {
        return DepotClientDto.fromEntity(repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Client introuvable")));
    }

    @Override
    @Transactional
    public void supprimer(Long id) {
        if (!repository.existsById(id))
            throw new RessourceIntrouvableException("Client introuvable");
        repository.deleteById(id);
    }
}
