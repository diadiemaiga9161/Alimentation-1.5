package com.ges.boutique.depot;

import java.util.List;

public interface DepotClientService {
    DepotClientDto creer(DepotClientRequest request);
    DepotClientDto modifier(Long id, DepotClientRequest request);
    List<DepotClientDto> getTous();
    List<DepotClientDto> rechercher(String query);
    DepotClientDto getById(Long id);
    void supprimer(Long id);
}
