package com.ges.boutique.rapport;

import java.time.LocalDate;
import java.util.Map;

public interface RapportService {

    Map<String, Object> genererRapportJournalier(LocalDate date);
    Map<String, Object> genererRapportHebdomadaire();
    Map<String, Object> genererRapportMensuel();
    Map<String, Object> genererStatistiquesGenerales();
}