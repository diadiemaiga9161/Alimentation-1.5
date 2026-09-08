-- Même bug que V106 (fonctionnalite_boutique.cle), découvert cette fois en ajoutant
-- WAVE_MONEY à ModePaiement/ModePaiementCaisse : Hibernate avait créé ventes.mode_paiement
-- et operations_caisse.mode_paiement comme des ENUM MySQL natifs figés sur les valeurs qui
-- existaient à la toute première création de ces tables. ddl-auto=update n'élargit jamais
-- un ENUM MySQL existant : toute nouvelle valeur ajoutée à l'enum Java plante en écriture
-- avec "Data truncated for column 'mode_paiement'" — confirmé en local en essayant
-- d'enregistrer une vente payée en Wave (HTTP 500).
ALTER TABLE ventes MODIFY COLUMN mode_paiement VARCHAR(20) NOT NULL;
ALTER TABLE operations_caisse MODIFY COLUMN mode_paiement VARCHAR(20) NULL;
