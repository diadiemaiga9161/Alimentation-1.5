-- Migration additive : index de performance
-- Aucun DROP, aucune modification de données existantes.
-- Utilise IF NOT EXISTS pour être idempotent sur toutes les instances.

-- Table : ventes
CREATE INDEX IF NOT EXISTS idx_vente_date     ON ventes(date_vente);
CREATE INDEX IF NOT EXISTS idx_vente_statut   ON ventes(annulee, est_credit, credit_regle);
CREATE INDEX IF NOT EXISTS idx_vente_vendeur  ON ventes(vendeur_id);
CREATE INDEX IF NOT EXISTS idx_vente_client   ON ventes(client_id);

-- Table : lignes_vente
CREATE INDEX IF NOT EXISTS idx_ligne_vente_produit ON lignes_vente(produit_id);
CREATE INDEX IF NOT EXISTS idx_ligne_vente_vente   ON lignes_vente(vente_id);

-- Table : produits
CREATE INDEX IF NOT EXISTS idx_produit_quantite    ON produits(quantite);
CREATE INDEX IF NOT EXISTS idx_produit_categorie   ON produits(categorie_id);
CREATE INDEX IF NOT EXISTS idx_produit_fournisseur ON produits(fournisseur_id);

-- Table : mouvements_stock
CREATE INDEX IF NOT EXISTS idx_mouvement_stock_date    ON mouvements_stock(date_mouvement);
CREATE INDEX IF NOT EXISTS idx_mouvement_stock_produit ON mouvements_stock(produit_id);
CREATE INDEX IF NOT EXISTS idx_mouvement_stock_type    ON mouvements_stock(type_mouvement);
