package com.ges.boutique.produit;

import java.util.List;

public interface UniteVenteService {
    List<UniteVente> lister(Long produitId);
    UniteVente creer(Long produitId, UniteVenteRequest request);
    UniteVente modifier(Long id, UniteVenteRequest request);
    void supprimer(Long id);
}
