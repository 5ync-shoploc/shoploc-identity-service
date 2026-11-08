# ShopLoc – Service d'identité

`shoploc-identity-service` est le microservice d'identité de la plateforme ShopLoc.
Il gère les comptes des utilisateurs (clients, commerçants, collectivités) en s'appuyant
sur **Keycloak**, qui stocke les comptes, hache les mots de passe et délivre les tokens JWT.

> La validation et l'activation des comptes commerçants relèvent du
> `shoploc-governance-service`.

---

## Sommaire

1. [Architecture](#architecture)
2. [Prérequis](#prérequis)
3. [Lancer Keycloak en local](#lancer-keycloak-en-local)
4. [Lancer le service](#lancer-le-service)
5. [API](#api)
6. [Tests](#tests)
7. [Configuration](#configuration)
8. [Dépannage](#dépannage)

---

## Architecture

```text
   Frontend React ──► API Gateway (:8080) ──► Identity Service (:8083) ──► Keycloak (:8180)
                      /api/identity/**        Spring Boot / Java 21        realm « shoploc »
```

- Le service **ne stocke aucun mot de passe** : il appelle l'API d'administration de
  Keycloak avec son propre compte technique (client `shoploc-identity`, flux
  *client credentials*).
- Keycloak est la **source de vérité** des profils (nom, prénom, e-mail, téléphone, rôle).
- Les rôles ShopLoc sont des *realm roles* Keycloak : `CLIENT`, `COMMERCANT`, `COLLECTIVITE`.

---

## Prérequis

| Outil | Version | Vérification |
|---|---|---|
| Java (JDK) | **21** | `java -version` |
| Docker + Docker Compose | récent | `docker compose version` |
| Maven | inutile, le wrapper `./mvnw` est fourni | |

> Si plusieurs JDK sont installés (ex. 21 et 25), forcez Java 21 :
> `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`

### Accès au POM parent (GitHub Packages)

Le projet hérite de `com.shoploc:shoploc-parent`, publié sur GitHub Packages.
Deux possibilités :

**Option A – installer le parent localement (le plus simple)**

```bash
git clone --branch v1.0.2 git@github.com:5ync-shoploc/shoploc-parent.git /tmp/shoploc-parent
./mvnw -f /tmp/shoploc-parent/pom.xml install
```

**Option B – configurer `~/.m2/settings.xml`** avec un token GitHub ayant le droit
`read:packages` (serveur `github`, dépôt
`https://maven.pkg.github.com/5ync-shoploc/shoploc-parent`), comme le fait la CI.

---

## Lancer Keycloak en local

Keycloak est défini dans [`docker-compose.yml`](docker-compose.yml) et le realm ShopLoc
dans [`keycloak/realm-shoploc.json`](keycloak/realm-shoploc.json). Le realm est importé
automatiquement au démarrage.

### 1. Créer le fichier `.env` (une seule fois)

```bash
cp .env.example .env
```

Le fichier `.env` contient le mot de passe admin et le secret du client. Il est ignoré
par Git : **ne jamais le commiter**.

### 2. Démarrer Keycloak

```bash
docker compose up -d
```

Suivre le démarrage (≈ 30 s) jusqu'à voir `Realm 'shoploc' imported` puis
`Keycloak ... started` (Ctrl+C pour quitter les logs, Keycloak continue de tourner) :

```bash
docker logs -f shoploc-keycloak
```

### 3. Vérifier dans la console d'administration

- URL : <http://localhost:8180/admin> — identifiants : ceux du `.env` (`admin` / `admin` par défaut)
- Choisir le realm **shoploc** en haut à gauche, puis vérifier :
  - **Realm roles** : `CLIENT`, `COMMERCANT`, `COLLECTIVITE`
  - **Clients** : `shoploc-identity` (utilisé par ce service) et `shoploc-frontend` (React)
  - **Realm settings → User profile** : attribut `phone`

### 4. Arrêter Keycloak

```bash
docker compose down
```

> ⚠️ En mode développement, Keycloak repart de zéro à chaque `down` / `up` : le realm
> est réimporté et **les utilisateurs de test sont perdus**. C'est voulu, pour que toute
> l'équipe ait le même environnement.

### Contenu du realm `shoploc`

| Élément | Rôle |
|---|---|
| Realm roles `CLIENT`, `COMMERCANT`, `COLLECTIVITE` | Rôles métier attribués à l'inscription |
| Client `shoploc-identity` (confidentiel) | Compte technique du service : création d'utilisateurs, attribution des rôles (`manage-users`, `view-users`, `query-users`, `view-realm`) et connexion par e-mail / mot de passe |
| Client `shoploc-frontend` (public, PKCE) | Connexion depuis l'application React (`http://localhost:5173`) |
| Attribut utilisateur `phone` | Téléphone modifiable depuis le profil |
| E-mail unique, connexion par e-mail | `duplicateEmailsAllowed: false`, `loginWithEmailAllowed: true` |
| E-mail = nom d'utilisateur | `registrationEmailAsUsername: true` : le nom d'utilisateur suit l'e-mail quand il est modifié |

Pour modifier le realm : faire la modification dans la console, puis la reporter dans
`keycloak/realm-shoploc.json` et commiter ce fichier, sinon elle sera perdue au prochain
redémarrage.

---

## Lancer le service

Keycloak doit être démarré. Le Config Server (`:8888`) est **optionnel** en local.

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # si besoin
./mvnw spring-boot:run
```

Le service écoute sur <http://localhost:8083>. Vérification :

```bash
curl http://localhost:8083/actuator/health
```

---

## API

Préfixe commun : `/api/identity` (route de l'API Gateway).

### `POST /api/identity/auth/register` — S'inscrire (US 1.1, SCRUM-34)

Accès public. Crée le compte dans Keycloak et lui attribue son rôle.

```bash
curl -i -X POST http://localhost:8083/api/identity/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
        "lastName": "Dupont",
        "firstName": "Julie",
        "email": "julie@mail.fr",
        "password": "motdepasse",
        "role": "CLIENT"
      }'
```

| Champ | Règle |
|---|---|
| `lastName`, `firstName` | obligatoires, 100 caractères max |
| `email` | obligatoire, format e-mail, **unique** (mis en minuscules) |
| `password` | obligatoire, 8 à 128 caractères, haché et stocké par Keycloak |
| `role` | `CLIENT`, `COMMERCANT` ou `COLLECTIVITE` |

| Réponse | Cas |
|---|---|
| `201 Created` | compte créé — renvoie `id`, `lastName`, `firstName`, `email`, `role` (jamais le mot de passe) |
| `400 Bad Request` | champ manquant ou invalide, rôle inconnu — le détail est dans `errors` |
| `409 Conflict` | un compte existe déjà avec cet e-mail |

Si l'attribution du rôle échoue, le compte est supprimé : on ne garde jamais un compte
sans rôle.

### `POST /api/identity/auth/login` — Se connecter (US 1.2, SCRUM-35)

Accès public. Le service transmet l'e-mail et le mot de passe à Keycloak (OpenID Connect,
endpoint `/token`) et renvoie le token JWT.

```bash
curl -i -X POST http://localhost:8083/api/identity/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email": "julie@mail.fr", "password": "motdepasse"}'
```

| Réponse | Cas |
|---|---|
| `200 OK` | `accessToken`, `refreshToken`, `tokenType` (`Bearer`), `expiresIn` (secondes) |
| `400 Bad Request` | e-mail ou mot de passe manquant |
| `401 Unauthorized` | identifiants invalides (même message que l'e-mail existe ou non) |

Contenu utile de l'`accessToken` (à décoder sur [jwt.io](https://jwt.io)) :

| Claim | Signification |
|---|---|
| `sub` | identifiant de l'utilisateur (le même que l'`id` renvoyé à l'inscription) |
| `realm_access.roles` | contient le rôle ShopLoc : `CLIENT`, `COMMERCANT` ou `COLLECTIVITE` |
| `email`, `given_name`, `family_name` | informations du profil |

Les autres services vérifient ce token et utilisent `sub` et le rôle pour les contrôles
d'accès.

### `GET /api/identity/users/me` — Consulter mon profil (US 1.3, SCRUM-36)

Token obligatoire. Il n'y a pas d'identifiant dans l'URL : le service utilise le `sub`
du token, donc chacun ne peut lire **que son propre profil**.

```bash
TOKEN=...   # accessToken renvoyé par /auth/login
curl -i http://localhost:8083/api/identity/users/me -H "Authorization: Bearer $TOKEN"
```

Réponse `200 OK` : `id`, `lastName`, `firstName`, `email`, `phone`, `role`.

### `PUT /api/identity/users/me` — Modifier mon profil (US 1.3, SCRUM-36)

```bash
curl -i -X PUT http://localhost:8083/api/identity/users/me \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"lastName": "Dupont", "firstName": "Julie", "email": "julie@mail.fr", "phone": "06 12 34 56 78"}'
```

| Champ | Règle |
|---|---|
| `lastName`, `firstName`, `email` | obligatoires (mêmes règles qu'à l'inscription) |
| `phone` | facultatif, chiffres, espaces, `.`, `-` et `+` (6 à 20 caractères) ; vide = supprimé |

| Réponse | Cas |
|---|---|
| `200 OK` | profil mis à jour (renvoie le nouveau profil) |
| `400 Bad Request` | champ manquant ou invalide |
| `401 Unauthorized` | pas de token, token expiré ou invalide |
| `409 Conflict` | le nouvel e-mail appartient déjà à un autre compte |

Après un changement d'e-mail, l'utilisateur se connecte avec le **nouvel** e-mail
(le realm utilise l'e-mail comme nom d'utilisateur). Le token déjà délivré reste valide
jusqu'à son expiration (15 min) mais contient encore l'ancien e-mail.

---

## Tests

```bash
./mvnw test
```

| Classe | Ce qui est testé |
|---|---|
| `RegistrationServiceTest` | création + rôle, e-mail déjà utilisé, annulation si le rôle échoue (Mockito) |
| `AuthControllerTest` | inscription (201 / 400 / 409) et connexion (200 / 400 / 401), accès sans token (MockMvc) |
| `KeycloakAdminClientTest` | appels à l'API d'administration de Keycloak (MockRestServiceServer) |
| `KeycloakTokenClientTest` | demande de token à Keycloak, refus des identifiants (MockRestServiceServer) |
| `ProfileControllerTest` | profil : 401 sans token, 200 avec un JWT simulé, 400, 409 |
| `ProfileServiceTest` | lecture / modification du profil, normalisation de l'e-mail et du téléphone |
| `ShoplocIdentityServiceApplicationTests` | démarrage du contexte Spring |

Les tests n'ont pas besoin de Keycloak (il est simulé).

---

## Configuration

Variables d'environnement (valeurs par défaut adaptées au poste de développement) :

| Variable | Défaut | Description |
|---|---|---|
| `KEYCLOAK_URL` | `http://localhost:8180` | URL de Keycloak |
| `KEYCLOAK_REALM` | `shoploc` | Realm ShopLoc |
| `KEYCLOAK_CLIENT_ID` | `shoploc-identity` | Client technique du service |
| `SHOPLOC_IDENTITY_CLIENT_SECRET` | `change-me` | Secret du client (doit être identique dans le `.env` de Keycloak) |
| `KEYCLOAK_ISSUER_URL` | `http://localhost:8180/realms/shoploc` | Émetteur attendu dans les JWT (claim `iss`) : l'URL de Keycloak **vue par le frontend** |

> En Docker / Kubernetes, `KEYCLOAK_URL` (adresse interne, ex. `http://keycloak:8180`) et
> `KEYCLOAK_ISSUER_URL` (adresse publique) peuvent être différentes : c'est pour cela
> qu'elles sont séparées.

En dehors du poste de développement, le secret est fourni par les GitHub Secrets /
Secrets Kubernetes, jamais écrit dans le code.

---

## Dépannage

| Problème | Solution |
|---|---|
| `Non-resolvable parent POM ... shoploc-parent` | Installer le POM parent (voir [Prérequis](#accès-au-pom-parent-github-packages)) |
| `release version 21 not supported` | Maven utilise un autre JDK : `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` |
| `port is already allocated` sur 8180 | Un autre Keycloak tourne : `docker ps`, puis `docker stop <nom>` |
| Inscription en erreur 500, logs `401` vers Keycloak | Le secret du `.env` et `SHOPLOC_IDENTITY_CLIENT_SECRET` ne correspondent pas |
| Modification du realm perdue | La reporter dans `keycloak/realm-shoploc.json` (voir plus haut) |
| `401` sur `/users/me` avec un token pourtant récent | Le `iss` du token ne correspond pas à `KEYCLOAK_ISSUER_URL` (ex. token obtenu via `127.0.0.1` au lieu de `localhost`) |
