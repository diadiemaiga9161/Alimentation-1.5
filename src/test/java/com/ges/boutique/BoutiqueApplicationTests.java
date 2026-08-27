package com.ges.boutique;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Profil "local" : la configuration par defaut (application.properties)
// exige maintenant des variables d'environnement pour les secrets
// (DB_PASSWORD, JWT_SECRET, ...) et pointe vers la base de prod
// "alimentation". Le profil local utilise la base MySQL locale
// "alimentation_local" et des secrets de dev fixes, sans rien exiger
// en variable d'environnement. Necessite un MySQL local actif
// (voir application-local.properties).
@SpringBootTest
@ActiveProfiles("local")
class BoutiqueApplicationTests {

	@Test
	void contextLoads() {
	}

}
