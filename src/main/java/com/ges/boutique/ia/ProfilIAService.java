package com.ges.boutique.ia;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilIAService {

    private final ProfilIARepository profilRepository;
    private final ParametreModeleIARepository parametreRepository;

    // ===================================================================
    // Initialisation des paramètres IA par défaut au démarrage
    // ===================================================================

    @PostConstruct
    @Transactional
    public void initialiserParametresParDefaut() {
        if (parametreRepository.count() == 0) {
            log.info("Initialisation des paramètres IA par défaut...");

            List<ParametreModeleIA> defaults = List.of(
                new ParametreModeleIA("seuil_alerte_stock",    0.30),   // 30% du stock optimal
                new ParametreModeleIA("alpha_ema",             0.30),   // facteur EMA (réactivité)
                new ParametreModeleIA("poids_recence_rfm",     0.40),   // pondération Recency RFM
                new ParametreModeleIA("poids_frequence_rfm",   0.35),   // pondération Frequency RFM
                new ParametreModeleIA("poids_monetaire_rfm",   0.25),   // pondération Monetary RFM
                new ParametreModeleIA("seuil_client_risque",  21.0),    // jours sans achat = client à risque
                new ParametreModeleIA("seuil_client_endormi", 60.0)     // jours sans achat = client endormi
            );

            parametreRepository.saveAll(defaults);
            log.info("Paramètres IA initialisés: {} entrées", defaults.size());
        }
    }

    // ===================================================================
    // CRUD Profil
    // ===================================================================

    @Transactional
    public ProfilIA sauvegarder(ProfilIA profil) {
        profil.setDateMiseAJour(LocalDateTime.now());
        if (profil.getDateCreation() == null) {
            profil.setDateCreation(LocalDateTime.now());
        }
        return profilRepository.save(profil);
    }

    @Transactional(readOnly = true)
    public Optional<ProfilIA> obtenirProfil() {
        // On utilise le premier profil trouvé (1 profil par instance boutique)
        return profilRepository.findAll()
                .stream()
                .findFirst();
    }

    @Transactional(readOnly = true)
    public boolean profilExiste() {
        return profilRepository.count() > 0;
    }
}
