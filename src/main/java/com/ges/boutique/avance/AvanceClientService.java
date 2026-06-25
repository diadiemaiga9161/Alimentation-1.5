package com.ges.boutique.avance;

import java.util.List;

public interface AvanceClientService {
    AvanceClient enregistrerAvance(AvanceClientRequest request);
    Double getSoldeDisponible(String clientNom, String clientTelephone);
    List<AvanceClient> getHistoriqueParClient(String clientNom, String clientTelephone);
    List<AvanceClient> getToutesLesAvances();
    void utiliserAvance(String clientNom, Double montantAUtiliser);
    void remettreAvance(String clientNom, Double montantARestituer);
}
