package com.ges.boutique.depot;

import java.util.List;
import java.util.Map;

public interface DepotGardeService {
    DepotGardeDto creerDepot(DepotGardeRequest request);
    DepotGardeDto modifierDepot(Long id, DepotGardeRequest request);
    DepotGardeDto getById(Long id);
    List<DepotGardeDto> getTous();
    List<DepotGardeDto> getActifs();
    List<DepotGardeDto> rechercher(String query);
    DepotGardeDto effectuerRetrait(Long depotId, RetraitDepotRequest request);
    DepotGardeDto cloturerDepot(Long id);
    Map<String, Object> getStatistiques();
    List<ClientDepotGroupeDto> getDepotsGroupesParClient();
    List<DepotGardeDto> effectuerRetraitGlobal(RetraitGlobalRequest request);
}
