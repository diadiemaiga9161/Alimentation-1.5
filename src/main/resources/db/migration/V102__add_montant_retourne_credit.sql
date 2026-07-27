-- Migration additive : suivi du montant retourné sur une vente à crédit
-- (réduit le solde restant à payer sans jamais toucher la caisse, puisque
-- l'argent des articles retournés n'a jamais été encaissé pour une vente
-- à crédit non réglée). Hibernate (ddl-auto=update) ajoute déjà cette
-- colonne automatiquement ; ce script est fourni pour les environnements
-- qui n'utilisent pas ddl-auto=update.

ALTER TABLE ventes ADD COLUMN IF NOT EXISTS montant_retourne DOUBLE NOT NULL DEFAULT 0;
