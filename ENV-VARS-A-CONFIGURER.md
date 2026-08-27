# Variables d'environnement à configurer sur le VPS avant de déployer

Ce changement sort les secrets (mots de passe, clés) du code source en clair.
**Ne déploie pas ce changement sur le VPS avant d'avoir configuré ces
variables sur chacun des 5 services systemd**, sinon les 5 boutiques ne
démarreront plus (`PlaceholderResolutionException` au démarrage — Spring
Boot refuse de démarrer si une variable `${VAR}` sans valeur par défaut
n'est pas fournie).

En local (profil `local`), rien à faire : des valeurs de dev sont déjà
fixées dans `application-local.properties` / `config/application-local.properties`.

## Ce qui a changé

| Ancien (en clair dans le fichier) | Nouveau | Variable d'environnement |
|---|---|---|
| `spring.datasource.password=Boutique@2026!` | `${DB_PASSWORD}` | `DB_PASSWORD` |
| `spring.datasource.username=boutique_user` | `${DB_USERNAME:boutique_user}` | `DB_USERNAME` (optionnel, garde `boutique_user` par défaut) |
| `jwt.secret=MaigaBoutiqueNSecret...` (différent par boutique) | `${JWT_SECRET}` | `JWT_SECRET` |
| `spring.mail.password=Diadie@2027` | `${SMTP_PASSWORD}` | `SMTP_PASSWORD` |
| `transfert.service.key=GesLafia@InterServiceSecret2026!` | `${INTER_BOUTIQUE_SECRET}` | `INTER_BOUTIQUE_SECRET` |

⚠️ **Important pour `JWT_SECRET`** : utilise bien la **même valeur qu'avant**
pour chaque boutique (listée ci-dessous), pas une nouvelle valeur aléatoire.
Si le secret change, tous les tokens déjà distribués aux utilisateurs/apps
mobiles connectés deviennent invalides instantanément → déconnexion forcée
de tout le monde au redémarrage du service.

## Valeurs actuelles à reporter (identiques à celles qui étaient dans le code)

Ces valeurs sont EXACTEMENT celles qui étaient déjà dans les fichiers
`.properties` avant ce changement — les reporter en variables d'environnement
ne change donc RIEN au comportement de l'application, ça retire juste le
mot de passe/la clé du fichier source versionné dans git.

```
DB_USERNAME=boutique_user
DB_PASSWORD=Boutique@2026!
SMTP_PASSWORD=Diadie@2027
INTER_BOUTIQUE_SECRET=GesLafia@InterServiceSecret2026!

# JWT_SECRET est DIFFÉRENT pour chaque boutique/service :
# boutique  (8080) : JWT_SECRET=MaigaBoutique1SecretJWTProduction2026ChangeMoiAvantMiseEnLigne
# boutique2 (8082) : JWT_SECRET=MaigaBoutique2SecretJWTProduction2026ChangeMoiAvantMiseEnLigne
# boutique3 (8083) : JWT_SECRET=MaigaBoutique3SecretJWTProduction2026ChangeMoiAvantMiseEnLigne
# boutique4 (8084) : JWT_SECRET=MaigaBoutique4SecretJWTProduction2026ChangeMoiAvantMiseEnLigne
# boutique5 (8085) : JWT_SECRET=MaigaBoutique5SecretJWTProduction2026ChangeMoiAvantMiseEnLigne
```

## Comment les configurer sur le VPS (systemd)

Les 5 services s'appellent `boutique`, `boutique2`, `boutique3`, `boutique4`,
`boutique5` (vus dans `deploy-all.bat`). Pour chacun, deux options :

**Option A — fichier d'environnement séparé (recommandé, plus propre)**

Sur le VPS, créer un fichier par boutique, par ex. `/opt/boutique/boutique1.env` :
```
DB_USERNAME=boutique_user
DB_PASSWORD=Boutique@2026!
JWT_SECRET=MaigaBoutique1SecretJWTProduction2026ChangeMoiAvantMiseEnLigne
SMTP_PASSWORD=Diadie@2027
INTER_BOUTIQUE_SECRET=GesLafia@InterServiceSecret2026!
```
Puis dans l'unité systemd correspondante (`/etc/systemd/system/boutique.service`),
ajouter sous `[Service]` :
```
EnvironmentFile=/opt/boutique/boutique1.env
```
Répéter pour `boutique2.env` … `boutique5.env` (en changeant `JWT_SECRET`
selon le tableau ci-dessus) et les unités `boutique2.service` … `boutique5.service`.

**Option B — directement dans l'unité systemd**
```
[Service]
Environment=DB_USERNAME=boutique_user
Environment=DB_PASSWORD=Boutique@2026!
Environment=JWT_SECRET=MaigaBoutique1SecretJWTProduction2026ChangeMoiAvantMiseEnLigne
Environment=SMTP_PASSWORD=Diadie@2027
Environment=INTER_BOUTIQUE_SECRET=GesLafia@InterServiceSecret2026!
```

Après modification des unités :
```
systemctl daemon-reload
systemctl restart boutique boutique2 boutique3 boutique4 boutique5
```

## Étape suivante recommandée (pas faite ici)

Une fois ce déploiement validé et stable, envisager de **changer les
valeurs elles-mêmes** (mot de passe DB, clé inter-boutique, mots de passe
SMTP) puisqu'elles ont circulé en clair dans le repo git jusqu'ici — un
simple retrait du code ne les rend pas secrètes rétroactivement pour qui
aurait déjà accès à l'historique git. Cette rotation n'a pas été faite
maintenant pour ne rien casser sans coordination avec toi sur le timing.
