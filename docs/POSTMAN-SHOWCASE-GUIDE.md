# CodePulse — Guide Postman complet (démo manager / soutenance)

Ce guide accompagne la **collection importable** et l’**environnement Postman** du projet.  
Objectif : montrer **toutes** les fonctionnalités importantes — surtout **ingest → notification → activation → feedback → relance** — devant un manager, avec une base **propre** et des **candidats créés par Postman** (pas des users préexistants).

---

## Fichiers à importer dans Postman

| Fichier | Rôle |
|---------|------|
| `docs/postman/CodePulse-Local.postman_environment.json` | Variables (`spring`, `search`, tokens, ids…) |
| `docs/postman/CodePulse-Full-Showcase.postman_collection.json` | ~80 requêtes organisées en dossiers |

**Import :** Postman → **Import** → glisser les 2 fichiers → sélectionner l’environnement **CodePulse-Local** en haut à droite.

Référence détaillée (matrice rôles, cas anormaux) : `docs/POSTMAN-FULL-APP-TEST-PLAN.md`.

---

## 1. Préparer une base « fraîche » (`ddl-auto=create`)

Tu veux une démo où **Alice Dupont** et les autres candidats **n’existent pas** avant l’ingest Postman. Voici la procédure.

### 1.1 PostgreSQL

```bash
# Une seule fois si la DB n’existe pas
sudo -u postgres psql -f scripts/create-db.sql
```

### 1.2 Extension pgvector (obligatoire pour la recherche)

Après chaque **recréation** du schéma, réactiver pgvector en superuser :

```bash
sudo -u postgres psql -d codepulse -f codepulse-search/scripts/enable_pgvector.sql
```

### 1.3 Backend — mode create

Dans `backend/src/main/resources/application.properties` (fichier local, gitignored) :

```properties
codepulse.mode=standalone
spring.profiles.active=${codepulse.mode}

spring.jpa.hibernate.ddl-auto=create
# Remettre update après la démo si tu veux conserver les données

# Pour tester les relances vite en démo Postman :
codepulse.notification.relance.delay=2m
codepulse.notification.relance.check-interval=1m
codepulse.notification.relance.initial-delay=30s
codepulse.notification.relance.max=2
```

**Effet de `create` :** au démarrage Spring, **toutes les tables sont recréées vides**, puis :

1. **RoleAccountsSeeder** recrée les **4 comptes staff** (admin, challenge admin, manager, demo.user).
2. **DemoDataSeeder** (profil `standalone`) recrée ~48 challenges, ~28 candidats, feedbacks, etc.

Les candidats **Postman** (ex. `alice.dupont.postman@example.com`) restent **absents** tant que tu n’as pas lancé l’ingest — c’est ce que tu montreras au manager.

### 1.4 Search service

```bash
cd codepulse-search
./run.sh   # ou run.bat sur Windows
```

Vérifier : `GET http://localhost:8090/health/ready` → `database: true`, `pgvector: true`.

Tables search : créées au premier démarrage / via `python scripts/init_db.py` si besoin.

### 1.5 Démarrer Spring + (optionnel) front

```bash
# Backend (JDK 21)
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--codepulse.mode=standalone

# Frontend (Node 20+)
cd frontend && npm start
```

Kafka local `:9092` recommandé en mode standalone (script `scripts/kafka-start.sh`).

---

## 2. Variables Postman — incrémenter à chaque démo

Avant **chaque** présentation avec `ddl-auto=create`, mets à jour l’environnement :

| Variable | Exemple run 1 | Run 2 |
|----------|---------------|-------|
| `external_user_id` | `990001` | `990002` |
| `external_test_id` | `880001` | `880003` |
| `external_test_id_2` | `880002` | `880004` |
| `candidate_email` | `alice.dupont.postman@example.com` | `bob.martin.postman@example.com` |

Sinon l’ingest retombe en cas **C3 BOTH_EXIST** (idempotent) au lieu de **C1 BOTH_NEW**.

---

## 3. Comptes fixes (toujours recréés au boot)

| Rôle | Email | Mot de passe | Variable token |
|------|-------|--------------|----------------|
| APP ADMIN | `admin@codepulse.local` | `Admin1234!` | `token_app_admin` |
| CHALLENGE ADMIN | `challenge.admin@codepulse.local` | `Challenge1234!` | `token_challenge_admin` |
| MANAGER RH | `manager.rh@codepulse.local` | `Manager1234!` | `token_manager` |
| USER démo | `demo.user@codepulse.local` | `Demo1234!` | `token_user` |

Les **candidats métier** viennent uniquement de **`POST /coding-challenges/ingest-batch`**.

---

## 4. Script de démo manager (15–20 min) — dossier `99 — E2E Showcase`

Exécuter dans l’ordre (Collection Runner sur ce dossier, ou manuellement).

### Acte 1 — Socle (2 min)

1. **00 — Setup & Health** : Search ready + Spring joignable.
2. **01 — Login APP ADMIN** : token sauvegardé automatiquement.

**Ce que tu dis :** « CodePulse reçoit des événements de fin de challenge depuis la plateforme externe ; on simule ça en Postman. »

### Acte 2 — Ingest & 4 cas métier (5 min)

3. **C1 — BOTH_NEW** : nouvel email + nouveau test externe.

Réponse attendue (extrait) :

```json
{
  "succeeded": 1,
  "entityCaseCounts": { "BOTH_NEW": 1 },
  "items": [{
    "entityCase": "BOTH_NEW",
    "userEmail": "alice.dupont.postman@example.com",
    "notificationCreated": true,
    "userId": 123,
    "challengeId": 456
  }]
}
```

Les scripts Postman enregistrent `user_id`, `challenge_id`, `candidate_email`.

4. Montrer **C2** (même user, nouveau test), **C3** (ré-ingest → `BOTH_EXIST`, pas de doublon notif), **C4** (`demo.user` + nouveau challenge → lien feedback direct).

**Ce que tu dis :** « Quatre cas : tout nouveau, user connu, idempotence, compte déjà activé. »

### Acte 3 — Notifications (cœur de la démo) (5 min)

5. **POST /notifications** avec `user_id` + `challenge_id`  
   → réponse avec `urlAction`, `livraisonEmail`, `notification.id`.

Champs importants :

| Champ | Signification |
|-------|----------------|
| `dejaExistante` | `true` si paire user/challenge déjà notifiée |
| `livraisonEmail` | `ENVOYE` / `ECHEC` / `DESACTIVE` / `NON_APPLICABLE` |
| `urlAction` | Lien UI (`/complete-account?token=…` ou feedback) |
| `notification.statut` | `EN_ATTENTE`, `ENVOYEE`, `LUE`, `ECHEC` |

6. **Lire l’e-mail :**
   - **GreenMail (standalone)** : `GET /dev/mailbox` ou `/dev/mailbox/json` — le script extrait `setup_token`.
   - **Gmail (ton PC Windows)** : ouvrir la boîte `codepulse.notification.to` ; copier le token du lien dans `setup_token`.

7. **GET notifications** (inbox user, liste admin, count par statut).

8. **Relance :**
   - Attendre `relance.delay` (ex. 2 min en config démo), **ou**
   - `GET /dev/relance/run` (sans auth, profil standalone).
   - Vérifier `GET /integration-logs/.../type=RELANCE` et nouveau mail « Rappel #1 ».

**Ce que tu dis :** « Tant qu’aucun feedback SOUMIS, le système relance ; après soumission, relance = 0. »

### Acte 4 — Activation compte + feedback (4 min)

9. `GET /auth/setup-info?token=…`
10. `POST /auth/complete-account` → JWT candidat
11. `GET /feedbacks/form?challengeId=…` → récupère `question_id`
12. `POST /feedbacks/submit` (statut `SOUMIS`)
13. `GET /dev/relance/run` → **`sent: 0`**

**Ce que tu dis :** « Le candidat active son compte, remplit le formulaire RH, la notification passe LUE, les relances s’arrêtent. »

### Acte 5 — Gouvernance & IA (4 min)

14. Login **MANAGER** → dashboards analytics + export CSV.
15. Login **APP ADMIN** → logs intégration (`ENVOI_NOTIFICATION`, `FEEDBACK`, `RELANCE`).
16. **Search `:8090`** : KPI « taux de participation », Assistant Capgemini (Ollama optionnel).

---

## 5. Dossiers de la collection (vue d’ensemble)

| Dossier | Contenu |
|---------|---------|
| `00 — Setup & Health` | Ping Spring + Search |
| `01 — Auth` | 4 logins + `/api/me` + 401 |
| `02 — Ingest` | C1–C4 + validations + list challenges |
| `03 — Notifications` | envoi, lecture, statut, mailbox, relance, logs |
| `04 — Complete account` | setup-info → complete-account → login |
| `05 — Feedback` | form → submit → relance bloquée |
| `06 — Password reset` | forgot → demandes → send-link → reset |
| `07 — Users & Questions` | staff CRUD + refus rôle USER |
| `08 — Analytics & Logs` | 4 dashboards + export + logs |
| `09 — Smart Search` | search, KPI, assistant, knowledge, sync |
| `10 — Role matrix` | 403 smoke tests |
| `99 — E2E Showcase` | enchaînement manager |

---

## 6. Notifications — requêtes essentielles (copier-coller)

### Envoi manuel

```http
POST {{spring}}/notifications
Authorization: Bearer {{token_app_admin}}
Content-Type: application/json

{
  "utilisateurId": {{user_id}},
  "codingChallengeId": {{challenge_id}}
}
```

### Inbox candidat

```http
GET {{spring}}/notifications/get-notifications-by-utilisateur-pages/page?utilisateurId={{user_id}}&page=1&size=10
Authorization: Bearer {{token_user}}
```

### Marquer comme lue

```http
PATCH {{spring}}/notifications/update-statut/{{notification_id}}/statut?statut=LUE
Authorization: Bearer {{token_user}}
```

### Relance manuelle (démo)

```http
GET {{spring}}/dev/relance/run
```

### Boîte mail locale

```http
GET {{spring}}/dev/mailbox/json
```

---

## 7. Ingest — body type C1 (nouveau candidat)

```json
[
  {
    "user": {
      "id": 990001,
      "nom": "Dupont",
      "prenom": "Alice",
      "userName": "alice.dupont.postman",
      "email": "alice.dupont.postman@example.com",
      "status": true
    },
    "test": {
      "id": 880001,
      "titre": "Two Sum Postman",
      "description": "Find two numbers that add up to target.",
      "tag": "arrays",
      "duree": 45,
      "codeUrl": "https://example.com/two-sum",
      "parameter": false
    }
  }
]
```

```http
POST {{spring}}/coding-challenges/ingest-batch
Authorization: Bearer {{token_challenge_admin}}
```

Rôles autorisés : **ADMIN_CODING_CHALLENGE** ou **ADMIN_CODEPULSE**.

---

## 8. Feedback — body type

```json
{
  "codingChallengeId": {{challenge_id}},
  "noteGlobale": 4.5,
  "commentaire": "Challenge clair — test Postman.",
  "statut": "SOUMIS",
  "reponses": [
    { "questionId": {{question_id}}, "valeur": "4" }
  ]
}
```

```http
POST {{spring}}/feedbacks/submit
Authorization: Bearer {{token_user}}
```

---

## 9. Questions qu’un manager peut poser — et quoi montrer

| Question manager | Requête / écran Postman |
|------------------|-------------------------|
| « Comment un nouveau candidat arrive ? » | Ingest C1 → `entityCase: BOTH_NEW` |
| « Pas de doublon si on renvoie le même event ? » | Ingest C3 → `BOTH_EXIST`, `notificationAlreadyExisted: true` |
| « Comment il reçoit le mail ? » | POST notifications + mailbox / Gmail |
| « Et s’il ne répond pas ? » | `/dev/relance/run` + logs RELANCE |
| « Et après le feedback ? » | Submit SOUMIS + relance `sent: 0` |
| « Qui peut envoyer une notif ? » | 403 avec token USER |
| « Où voir l’historique ? » | Integration logs ENVOI_NOTIFICATION |
| « KPI participation ? » | `POST {{search}}/kpi` ou analytics manager |
| « Recherche sur les commentaires ? » | Search avec rôle MANAGER + `source_type` |
| « Sécurité rôles ? » | Dossier `10 — Role matrix` |

---

## 10. Collection Runner — lancer toute la suite

1. Postman → Collection **CodePulse Full Showcase** → **Run**.
2. Choisir l’environnement **CodePulse-Local**.
3. Pour un smoke complet : cocher tous les dossiers sauf `99` si tu veux aller vite.
4. Pour la **démo manager** : runner **uniquement** `99 — E2E Showcase`.
5. Délai entre requêtes : **500 ms** (assistant Ollama : laisser 60–90 s sur la dernière requête).

Après un run, regarder l’onglet **Test Results** (vert/rouge).

---

## 11. Pièges fréquents

| Symptôme | Cause | Fix |
|----------|-------|-----|
| 401 partout | Token expiré (1 h) | Re-login dossier 01 |
| Ingest → C3 au lieu de C1 | Mêmes `external_*` / email | Incrémenter variables env |
| `setup_token` vide | Gmail au lieu de GreenMail | Copier token depuis l’e-mail réel |
| Search 401 | Pas de Bearer | Login admin avant dossier 09 |
| Search 502 depuis UI | Search pas démarré | `run.sh` / `run.bat` |
| Relance `sent: 0` | Délai pas écoulé ou feedback SOUMIS | `delay=2m` + attendre, ou pas de submit avant |
| pgvector false | Extension pas recréée | `enable_pgvector.sql` après `create` |
| `question_id` manquant | Pas de questions en base | DemoDataSeeder en standalone en crée 16 au boot |

---

## 12. Checklist avant la soutenance

- [ ] `ddl-auto=create` + redémarrage Spring (base fraîche)
- [ ] pgvector activé
- [ ] Search `:8090` up, `/health/ready` OK
- [ ] Variables Postman `external_user_id` / email uniques
- [ ] Relance `delay=2m` pour démo live (optionnel)
- [ ] Mailbox GreenMail **ou** Gmail configuré dans `application.properties`
- [ ] Collection + Environment importés
- [ ] Dossier **99 — E2E** exécuté une fois en répétition
- [ ] (Optionnel) Ollama + `llama3.2:1b` pour assistant rédigé

---

## 13. Regénérer la collection

Si tu modifies le générateur :

```bash
python3 docs/postman/generate_collection.py
```

---

*CodePulse — guide Postman showcase · notifications · ingest 4 cas · relances · feedback · search · analytics*
