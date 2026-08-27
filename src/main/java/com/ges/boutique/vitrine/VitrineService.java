package com.ges.boutique.vitrine;

import java.util.List;

public interface VitrineService {

    List<VitrineProduitDto> obtenirProduitsVitrine();

    VitrineInfoDto obtenirInfosVitrine();
}
