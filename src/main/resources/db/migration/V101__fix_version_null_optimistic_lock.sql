-- Migration additive : correctif verrou optimiste (colonne version)
-- BUG CRITIQUE : produits.version (et produit_niveaux.version) sont NULL sur les lignes
-- existantes car la colonne @Version a été ajoutée sans backfill. Hibernate plante avec
-- une NullPointerException ("current" is null) dès qu'il doit incrémenter la version d'une
-- ligne existante (ex: décrémenter le stock lors d'une vente) -> vente impossible (HTTP 500).
-- Aucun DROP, aucune perte de données : simple backfill à 0 + valeur par défaut.

UPDATE produits SET version = 0 WHERE version IS NULL;
ALTER TABLE produits MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE produit_niveaux SET version = 0 WHERE version IS NULL;
ALTER TABLE produit_niveaux MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
