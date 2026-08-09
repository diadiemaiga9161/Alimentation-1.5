-- Stock du produit "principal" encore non décomposé (ex: nombre de cartons
-- fermés), utilisé comme parent implicite du premier niveau de conditionnement
-- (celui avec parent_id NULL). Sans cette colonne, la cascade automatique
-- ne pouvait pas puiser dans le stock du produit lui-même et perdait
-- silencieusement cette quantité dès la première synchronisation des niveaux.
--
-- Hibernate (ddl-auto=update) ajoute déjà cette colonne automatiquement en
-- local ; ce script est fourni pour les environnements qui n'utilisent pas
-- ddl-auto=update.
ALTER TABLE produits ADD COLUMN IF NOT EXISTS quantite_principale INT NULL;
