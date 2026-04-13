package com.ges.boutique.utilisateur;

import com.ges.boutique.vente.VendeurDto;
import org.springframework.stereotype.Component;

@Component
public class UtilisateurMapper {

    public VendeurDto toVendeurDto(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }

        VendeurDto dto = new VendeurDto();
        dto.setId(utilisateur.getId());
        dto.setUsername(utilisateur.getUsername());
        dto.setNomComplet(utilisateur.getNomComplet()); // Utilise le getter
        dto.setEmail(utilisateur.getEmail());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setRole(utilisateur.getRole() != null ? utilisateur.getRole().name() : null);
        dto.setActif(utilisateur.isActif()); // Utilise la méthode isActif()
        return dto;
    }
}