package com.ges.boutique.vente;

import com.ges.boutique.utilisateur.Utilisateur;
import com.ges.boutique.utilisateur.UtilisateurMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VenteMapper {

    private final UtilisateurMapper utilisateurMapper;

    public LigneVenteDto toLigneVenteDto(LigneVente ligneVente) {
        LigneVenteDto dto = new LigneVenteDto();
        dto.setProduitId(ligneVente.getProduitId());
        dto.setProduitNom(ligneVente.getProduitNom());
        dto.setQuantite(ligneVente.getQuantite());
        dto.setPrixUnitaire(ligneVente.getPrixUnitaire());
        dto.setRemisePourcentage(ligneVente.getRemisePourcentage());
        dto.setRemiseMontant(ligneVente.getRemiseMontant());
        dto.setPrixApresRemise(ligneVente.getPrixApresRemise());
        dto.setSousTotal(ligneVente.getSousTotal());
        dto.setMontantRemise(ligneVente.getMontantRemise());
        return dto;
    }

    public VenteDto toVenteDto(Vente vente) {
        VenteDto dto = new VenteDto();
        dto.setId(vente.getId());
        dto.setNumeroVente(vente.getNumeroVente());
        dto.setVendeurId(vente.getVendeurId());
        dto.setVendeurNom(getVendeurNom(vente));

        // Ajouter le DTO vendeur sécurisé
        if (vente.getVendeur() != null) {
            dto.setVendeur(utilisateurMapper.toVendeurDto(vente.getVendeur()));
        }

        // Convertir les lignes de vente en DTO
        if (vente.getLignes() != null) {
            List<LigneVenteDto> ligneDtos = vente.getLignes().stream()
                    .map(this::toLigneVenteDto)
                    .collect(Collectors.toList());
            dto.setLignes(ligneDtos);
        }

        dto.setMontantTotal(vente.getMontantTotal());
        dto.setMontantRemiseTotal(vente.getMontantRemiseTotal());
        dto.setMontantApresRemise(vente.getMontantApresRemise());
        dto.setRemiseGlobale(vente.getRemiseGlobale());
        dto.setTypeRemiseGlobale(vente.getTypeRemiseGlobale());
        dto.setModePaiement(vente.getModePaiement());
        dto.setReferencePaiement(vente.getReferencePaiement());
        dto.setDateVente(vente.getDateVente());

        return dto;
    }

    // Méthode pour obtenir le nom du vendeur
    private String getVendeurNom(Vente vente) {
        if (vente.getVendeur() == null) {
            return "Inconnu";
        }

        try {
            return vente.getVendeur().getNomComplet();
        } catch (Exception e) {
            return "Vendeur #" + vente.getVendeurId();
        }
    }

    // Méthode pour créer une Map facile à utiliser dans le frontend
    public Map<String, Object> toVenteMap(Vente vente) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", vente.getId());
        map.put("numeroVente", vente.getNumeroVente());
        map.put("vendeurId", vente.getVendeurId());
        map.put("vendeurNom", getVendeurNom(vente));

        // Ajouter le DTO vendeur sécurisé
        if (vente.getVendeur() != null) {
            map.put("vendeur", utilisateurMapper.toVendeurDto(vente.getVendeur()));
        }

        map.put("montantTotal", vente.getMontantTotal());
        map.put("montantRemiseTotal", vente.getMontantRemiseTotal());
        map.put("montantApresRemise", vente.getMontantApresRemise());
        map.put("remiseGlobale", vente.getRemiseGlobale());
        map.put("typeRemiseGlobale", vente.getTypeRemiseGlobale() != null ?
                vente.getTypeRemiseGlobale().toString() : null);
        map.put("modePaiement", vente.getModePaiement().toString());
        map.put("referencePaiement", vente.getReferencePaiement());
        map.put("dateVente", vente.getDateVente());
        map.put("nombreProduits", vente.getLignes() != null ? vente.getLignes().size() : 0);
        map.put("produits", vente.getLignes() != null ?
                vente.getLignes().stream().map(this::toLigneMap).collect(Collectors.toList()) :
                new ArrayList<>());
        return map;
    }

    // Méthode publique pour mapper une ligne de vente
    public Map<String, Object> toLigneMap(LigneVente ligne) {
        Map<String, Object> map = new HashMap<>();
        map.put("produitId", ligne.getProduitId());
        map.put("produitNom", ligne.getProduitNom());
        map.put("quantite", ligne.getQuantite());
        map.put("prixUnitaire", ligne.getPrixUnitaire());
        map.put("prixApresRemise", ligne.getPrixApresRemise());
        map.put("sousTotal", ligne.getSousTotal());
        map.put("remisePourcentage", ligne.getRemisePourcentage());
        map.put("remiseMontant", ligne.getRemiseMontant());
        map.put("montantRemise", ligne.getMontantRemise());
        return map;
    }

    // Méthode pour créer une liste de Map pour toutes les ventes
    public List<Map<String, Object>> toVenteMapList(List<Vente> ventes) {
        return ventes.stream()
                .map(this::toVenteMap)
                .collect(Collectors.toList());
    }

    // Méthode utilitaire supplémentaire
    public Map<String, Object> toSimpleVenteMap(Vente vente) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", vente.getId());
        map.put("numeroVente", vente.getNumeroVente());
        map.put("dateVente", vente.getDateVente());
        map.put("vendeurId", vente.getVendeurId());

        // Ajouter les infos basiques du vendeur
        if (vente.getVendeur() != null) {
            Map<String, Object> vendeurSimple = new HashMap<>();
            vendeurSimple.put("id", vente.getVendeur().getId());
            vendeurSimple.put("username", vente.getVendeur().getUsername());
            vendeurSimple.put("nomComplet", vente.getVendeur().getNomComplet());
            map.put("vendeur", vendeurSimple);
        }

        map.put("montantTotal", vente.getMontantTotal());
        map.put("modePaiement", vente.getModePaiement());
        map.put("nombreProduits", vente.getLignes() != null ? vente.getLignes().size() : 0);
        return map;
    }
}