package com.ges.boutique.notification;

import com.ges.boutique.config.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersistanceService {

    private final NotificationRepository notificationRepository;
    private final NotificationService wsNotificationService;

    @Transactional
    public Notification creer(String type, String titre, String message, String lien) {
        Notification n = Notification.creer(type, titre, message, lien);
        Notification saved = notificationRepository.save(n);

        Map<String, Object> data = new HashMap<>();
        data.put("id", saved.getId());
        data.put("type", saved.getType());
        data.put("titre", saved.getTitre());
        data.put("message", saved.getMessage());
        data.put("lien", saved.getLien());
        data.put("dateCreation", saved.getDateCreation().toString());
        data.put("countNonLues", notificationRepository.countNonLues());
        wsNotificationService.notifierNouvelleNotification(data);

        return saved;
    }

    @Transactional
    public void stockFaible(Long produitId, String produitNom, int quantite, int seuil) {
        boolean dejaNotifie = notificationRepository.existsByTypeAndReferenceIdAndReferenceTypeAndLuFalse(
                "STOCK_FAIBLE", produitId, "PRODUIT");
        if (dejaNotifie) return;

        String msg = produitNom + " — stock : " + quantite + " unité(s) (seuil : " + seuil + ")";
        Notification n = Notification.creer("STOCK_FAIBLE", "Stock faible", msg, "/pages/produit");
        n.setReferenceId(produitId);
        n.setReferenceType("PRODUIT");
        Notification saved = notificationRepository.save(n);

        Map<String, Object> data = new HashMap<>();
        data.put("id", saved.getId());
        data.put("type", saved.getType());
        data.put("titre", saved.getTitre());
        data.put("message", saved.getMessage());
        data.put("lien", saved.getLien());
        data.put("dateCreation", saved.getDateCreation().toString());
        data.put("countNonLues", notificationRepository.countNonLues());
        wsNotificationService.notifierNouvelleNotification(data);
    }

    @Transactional
    public void ruptureStock(Long produitId, String produitNom) {
        boolean dejaNotifie = notificationRepository.existsByTypeAndReferenceIdAndReferenceTypeAndLuFalse(
                "RUPTURE_STOCK", produitId, "PRODUIT");
        if (dejaNotifie) return;

        Notification n = Notification.creer("RUPTURE_STOCK", "Rupture de stock !",
                produitNom + " est en rupture de stock.", "/pages/produit");
        n.setReferenceId(produitId);
        n.setReferenceType("PRODUIT");
        Notification saved = notificationRepository.save(n);

        Map<String, Object> data = new HashMap<>();
        data.put("id", saved.getId());
        data.put("type", saved.getType());
        data.put("titre", saved.getTitre());
        data.put("message", saved.getMessage());
        data.put("lien", saved.getLien());
        data.put("dateCreation", saved.getDateCreation().toString());
        data.put("countNonLues", notificationRepository.countNonLues());
        wsNotificationService.notifierNouvelleNotification(data);
    }

    public List<Notification> getNonLues() {
        return notificationRepository.findByLuFalseOrderByDateCreationDesc();
    }

    public List<Notification> getTout() {
        return notificationRepository.findAllByOrderByDateCreationDesc();
    }

    public long countNonLues() {
        return notificationRepository.countNonLues();
    }

    @Transactional
    public Notification marquerLue(Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification introuvable: " + id));
        n.setLu(true);
        Notification saved = notificationRepository.save(n);

        Map<String, Object> data = new HashMap<>();
        data.put("countNonLues", notificationRepository.countNonLues());
        wsNotificationService.notifierNouvelleNotification(data);

        return saved;
    }

    @Transactional
    public void marquerToutesLues() {
        List<Notification> nonLues = notificationRepository.findByLuFalseOrderByDateCreationDesc();
        nonLues.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(nonLues);

        Map<String, Object> data = new HashMap<>();
        data.put("countNonLues", 0L);
        wsNotificationService.notifierNouvelleNotification(data);
    }
}
