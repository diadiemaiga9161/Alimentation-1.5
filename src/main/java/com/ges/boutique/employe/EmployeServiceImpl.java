package com.ges.boutique.employe;

import com.ges.boutique.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeServiceImpl implements EmployeService {

    private final EmployeRepository employeRepository;

    @Override
    @Transactional
    public EmployeDto creerEmploye(EmployeRequest request) {
        if (request.getNom() == null || request.getNom().isBlank())
            throw new IllegalArgumentException("Le nom de l'employé est obligatoire");
        if (request.getPoste() == null || request.getPoste().isBlank())
            throw new IllegalArgumentException("Le poste est obligatoire");
        if (request.getSalaireMensuel() == null || request.getSalaireMensuel() <= 0)
            throw new IllegalArgumentException("Le salaire mensuel doit être supérieur à 0");

        Employe employe = new Employe();
        employe.setNom(request.getNom().trim());
        employe.setPrenom(request.getPrenom() != null ? request.getPrenom().trim() : null);
        employe.setPoste(request.getPoste().trim());
        employe.setSalaireMensuel(request.getSalaireMensuel());
        employe.setTelephone(request.getTelephone());
        employe.setObservation(request.getObservation());
        employe.setDateEmbauche(request.getDateEmbauche());
        employe.setStatut(request.getStatut() != null ? request.getStatut() : StatutEmploye.ACTIF);

        Employe saved = employeRepository.save(employe);
        log.info("Employé créé: {} {}", saved.getPrenom(), saved.getNom());
        return EmployeDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public EmployeDto modifierEmploye(Long id, EmployeRequest request) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable avec l'id: " + id));

        if (request.getNom() != null && !request.getNom().isBlank())
            employe.setNom(request.getNom().trim());
        if (request.getPrenom() != null)
            employe.setPrenom(request.getPrenom().trim());
        if (request.getPoste() != null && !request.getPoste().isBlank())
            employe.setPoste(request.getPoste().trim());
        if (request.getSalaireMensuel() != null && request.getSalaireMensuel() > 0)
            employe.setSalaireMensuel(request.getSalaireMensuel());
        if (request.getTelephone() != null)
            employe.setTelephone(request.getTelephone());
        if (request.getObservation() != null)
            employe.setObservation(request.getObservation());
        if (request.getDateEmbauche() != null)
            employe.setDateEmbauche(request.getDateEmbauche());
        if (request.getStatut() != null)
            employe.setStatut(request.getStatut());

        return EmployeDto.fromEntity(employeRepository.save(employe));
    }

    @Override
    public EmployeDto getEmployeById(Long id) {
        return EmployeDto.fromEntity(employeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable avec l'id: " + id)));
    }

    @Override
    public List<EmployeDto> getTousLesEmployes() {
        return employeRepository.findAllByOrderByNomAsc().stream()
                .map(EmployeDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmployeDto> getEmployesActifs() {
        return employeRepository.findByStatutOrderByNomAsc(StatutEmploye.ACTIF).stream()
                .map(EmployeDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void desactiverEmploye(Long id) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable avec l'id: " + id));
        employe.setStatut(StatutEmploye.INACTIF);
        employeRepository.save(employe);
        log.info("Employé {} {} désactivé", employe.getPrenom(), employe.getNom());
    }

    @Override
    @Transactional
    public void activerEmploye(Long id) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable avec l'id: " + id));
        employe.setStatut(StatutEmploye.ACTIF);
        employeRepository.save(employe);
    }

    @Override
    @Transactional
    public void supprimerEmploye(Long id) {
        if (!employeRepository.existsById(id))
            throw new RessourceIntrouvableException("Employé introuvable avec l'id: " + id);
        employeRepository.deleteById(id);
        log.info("Employé id={} supprimé", id);
    }
}
