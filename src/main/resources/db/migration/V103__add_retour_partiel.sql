-- Migration additive : distinction retour total / retour partiel sur une vente.
-- Hibernate (ddl-auto=update) ajoute déjà cette colonne automatiquement ; ce
-- script est fourni pour les environnements qui n'utilisent pas ddl-auto=update.

ALTER TABLE ventes ADD COLUMN IF NOT EXISTS retour_partiel BOOLEAN NOT NULL DEFAULT FALSE;
