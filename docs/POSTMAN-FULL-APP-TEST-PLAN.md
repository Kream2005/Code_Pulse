# CodePulse — Plan de tests Postman (application complète)

Document prêt à copier / envoyer par e-mail.  
Objectif : construire une **collection Postman** qui couvre **toutes** les fonctionnalités de CodePulse (Spring Boot + Recherche intelligente), en **cas normaux** et **cas anormaux**, pour les **4 rôles**.

---

## 0. Prérequis avant de tester

Services démarrés :

| Service | URL | Comment démarrer |
|---------|-----|------------------|
| Spring Boot | `http://localhost:8080` | `mvnw spring-boot:run` (profil `standalone`) |
| Recherche intelligente | `http://localhost:8090` | `codepulse-search` → `run.bat` / `run.sh` |
| Frontend (optionnel pour Postman) | `http://localhost:4200` | `npm run dev` |
| Ollama (optionnel) | `http://127.0.0.1:11434` | pour réponses Assistant rédigées |
| PostgreSQL + pgvector | `localhost:5432` | obligatoire |

Vérifications rapides (sans auth) :

1. `GET http://localhost:8080` — app up (ou login UI)
2. `GET http://localhost:8090/health`
3. `GET http://localhost:8090/health/ready` → `database`, `pgvector`, `jwt_public_key` = true

---

## 1. Environnement Postman

Créer un **Environment** `CodePulse-Local` :

| Variable | Valeur initiale | Notes |
|----------|-----------------|-------|
| `spring` | `http://localhost:8080` | API métier |
| `search` | `http://localhost:8090` | API recherche / KPI / assistant |
| `token` | *(vide)* | rempli après login |
| `token_user` | | JWT candidat |
| `token_challenge_admin` | | JWT ADMIN_CODING_CHALLENGE |
| `token_manager` | | JWT MANAGER_RH |
| `token_app_admin` | | JWT ADMIN_CODEPULSE |
| `user_id` | | id utilisateur (ex. demo.user) |
| `challenge_id` | | id challenge |
| `notification_id` | | id notification |
| `feedback_id` | | id feedback |
| `question_id` | | id question formulaire |
| `demande_id` | | id demande reset MDP |
| `setup_token` | | jeton activation compte |
| `reset_token` | | jeton reset MDP |
| `knowledge_doc_id` | | id document connaissance |
| `external_user_id` | `990001` | id externe unique pour ingest |
| `external_test_id` | `880001` | id test externe unique |

**Header par défaut** sur les dossiers protégés :

```http
Authorization: Bearer {{token}}
Content-Type: application/json
```

---

## 2. Comptes de démo (4 rôles)

| # | Rôle | Email | Mot de passe | Variable token |
|---|------|-------|--------------|----------------|
| 1 | `USER` (candidat) | `demo.user@codepulse.local` | `Demo1234!` | `token_user` |
| 2 | `ADMIN_CODING_CHALLENGE` | `challenge.admin@codepulse.local` | `Challenge1234!` | `token_challenge_admin` |
| 3 | `MANAGER_RH` | `manager.rh@codepulse.local` | `Manager1234!` | `token_manager` |
| 4 | `ADMIN_CODEPULSE` | `admin@codepulse.local` | `Admin1234!` | `token_app_admin` |

Compte incomplet (activation / relance) en standalone :

- Email : `pending.setup@codepulse.demo` (pas de mot de passe tant que le compte n’est pas complété)

---

## 3. Organisation recommandée de la collection

```
CodePulse Full Tests
├── 00 — Health & Auth
├── 01 — Role matrix (403)
├── 02 — Users (ADMIN_CODEPULSE)
├── 03 — Coding challenges (ingest / sync / CRUD)
├── 04 — Notifications (send / list / statut / relance)
├── 05 — Account setup (complete-account)
├── 06 — Feedbacks (form / submit / validation)
├── 07 — Questions formulaire
├── 08 — Password reset (forgot → admin → reset)
├── 09 — Analytics dashboards
├── 10 — Integration logs
├── 11 — Smart search (8090)
├── 12 — Knowledge + ingestion
└── 99 — End-to-end scenarios (4 personas)
```

Pour chaque requête : ajouter des **Tests** Postman (status code + champs JSON).

Exemple de script Login (Tests) :

```javascript
pm.test("Login 200", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.accessToken).to.be.a("string");
pm.environment.set("token", json.accessToken);
pm.environment.set("token_app_admin", json.accessToken); // adapter selon le dossier
```

---

## 4. Matrice d’accès par rôle (à tester systématiquement)

Légende : ✅ autorisé · ❌ 403 · ⚪ non pertinent / pas d’API

| Fonctionnalité | USER | CHALLENGE ADMIN | MANAGER RH | APP ADMIN |
|----------------|------|-----------------|------------|-----------|
| Login / profile `/api/me` | ✅ | ✅ | ✅ | ✅ |
| Inbox notifications (soi) | ✅ | ✅* | ✅* | ✅* |
| Soumettre feedback | ✅ | ❌ | ❌ | ❌ |
| Sync / ingest challenges | ❌ | ✅ | ❌ | ✅ |
| Soft-delete challenge | ❌ | ✅ | ❌ | ✅ |
| Envoyer notification | ❌ | ✅ | ❌ | ✅ |
| Lire tous feedbacks | ❌ | ✅ | ✅ | ✅ |
| Analytics manager | ❌ | ❌ | ✅ | ✅ |
| Gérer utilisateurs | ❌ | ❌ | ❌ | ✅ |
| Gérer questions | ❌ | ❌ | ❌ | ✅ |
| Demandes reset MDP | ❌ | ❌ | ❌ | ✅ |
| Logs intégration | ❌ | ❌ | ❌ | ✅ |
| `POST /search` | ❌ | ✅ | ✅ | ✅ |
| Search filter `FEEDBACK` | ❌ | ❌ (403) | ✅ | ✅ |
| Knowledge CRUD | ❌ | ❌ (lecture OK via search) | ✅ | ✅ |

\* Les admins peuvent aussi lire les notifications via APIs admin.

**Cas anormaux communs à tous les endpoints protégés :**

| Cas | Attendu |
|-----|---------|
| Pas de header `Authorization` | **401** |
| Token expiré / altéré | **401** |
| Bon token, mauvais rôle | **403** |
| Body JSON invalide | **400** |
| ID inexistant | **404** (quand applicable) |

---

# PARTIE A — API Spring (`{{spring}}`)

---

## 5. Auth & profil

### 5.1 Cas normaux

| # | Requête | Body / params | Attendu |
|---|---------|---------------|---------|
| A1 | `POST {{spring}}/auth/login` | `{ "email":"admin@codepulse.local", "password":"Admin1234!" }` | **200** + `accessToken`, `tokenType=Bearer`, `expiresIn` |
| A2 | Idem pour les 3 autres comptes | emails/passwords ci-dessus | **200** + token |
| A3 | `GET {{spring}}/api/me` | Bearer token | **200** : `subject`, `roles`, `uid` |
| A4 | `GET {{spring}}/api/profile` | Bearer token | **200** : email, nom, prenom, userName |
| A5 | `GET {{spring}}/api/admin` | token challenge ou app admin | **200** `admin access granted` |

### 5.2 Cas anormaux

| # | Cas | Attendu |
|---|-----|---------|
| A10 | Login mauvais mot de passe | **401** |
| A11 | Login email inconnu | **401** |
| A12 | Login email mal formé / vide | **400** |
| A13 | Login compte incomplet (pending.setup si password null) | **401** `COMPTE_INCOMPLET` (ou équivalent) |
| A14 | `GET /api/admin` avec token USER | **403** |
| A15 | `GET /api/profile` sans token | **401** |

Répéter **A1–A5** et **A10–A15** pour chaque persona (dossier `01 — Role matrix`).

---

## 6. Utilisateurs (ADMIN_CODEPULSE uniquement)

### 6.1 Normaux

| # | Requête | Notes |
|---|---------|-------|
| U1 | `GET {{spring}}/utilisateurs/get-all-users` | Liste |
| U2 | `GET {{spring}}/utilisateurs/get-users-pages/page?page=0&size=10&q=demo` | Recherche |
| U3 | `GET {{spring}}/utilisateurs/get-user-by-email?email=demo.user@codepulse.local` | Sauver `user_id` |
| U4 | `GET {{spring}}/utilisateurs/get-user/{{user_id}}` | Détail |
| U5 | `GET {{spring}}/utilisateurs/exists?email=admin@codepulse.local` | `true` |
| U6 | `GET {{spring}}/utilisateurs/count` | nombre |
| U7 | `GET {{spring}}/utilisateurs/count/role?role=USER` | |
| U8 | `POST {{spring}}/utilisateurs/add-user` | Créer un staff (ex. `MANAGER_RH` ou `ADMIN_CODING_CHALLENGE`) — **pas** de rôle `USER` (candidats via ingest) |
| U9 | `PUT {{spring}}/utilisateurs/update-user/{{id}}` | Modifier nom/email/role |
| U10 | `PATCH {{spring}}/utilisateurs/promote-role/{{id}}` | Changer rôle |
| U11 | `DELETE {{spring}}/utilisateurs/delete-user/{{id}}` | Soft-delete |

Exemple body `add-user` :

```json
{
  "nom": "Test",
  "prenom": "Staff",
  "email": "staff.test@codepulse.local",
  "rawPassword": "Staff1234!",
  "role": "MANAGER_RH"
}
```

### 6.2 Anormaux

| # | Cas | Attendu |
|---|-----|---------|
| U20 | `add-user` avec `role=USER` | **400** `ROLE_NON_AUTORISE` |
| U21 | Email déjà utilisé | **400** `EMAIL_DEJA_UTILISE` |
| U22 | Mot de passe < 8 caractères | **400** |
| U23 | Appel avec token MANAGER_RH | **403** |
| U24 | `get-user/999999` | **404** |
| U25 | Soft-delete déjà archivé / id invalide | **404** |

---

## 7. Coding challenges — 4 cas d’objets (cœur métier)

Le challenge **n’est pas passé dans CodePulse**. CodePulse reçoit un **événement de completion** :

- via Kafka, ou
- via `POST /coding-challenges/ingest-batch` (idéal Postman), ou
- via sync externe `POST /coding-challenges/synchroniser`.

### 7.1 Structure d’un événement (ingest)

```json
[
  {
    "user": {
      "id": 990001,
      "nom": "Dupont",
      "prenom": "Alice",
      "userName": "alice.dupont",
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

> Utiliser des `id` externes **uniques** à chaque run (`external_user_id`, `external_test_id`) pour éviter les collisions.

### 7.2 Les 4 cas à couvrir (obligatoire)

| Cas | Description | Comment tester | Attendu |
|-----|-------------|----------------|---------|
| **C1 — Nouvel utilisateur + nouveau challenge** | Email et `test.id` jamais vus | `ingest-batch` avec ids neufs | **200** (ou **207** si mixte) ; user créé (souvent `compteComplet=false`) ; challenge créé ; **notification** créée + e-mail si activé |
| **C2 — Utilisateur existant + nouveau challenge** | Même `user.email` / external user, nouveau `test.id` | 2e ingest, même user, nouveau test | Challenge ajouté ; notification pour ce couple user/challenge |
| **C3 — Utilisateur existant + challenge déjà connu** | Même user + même `test.id` | Ré-ingest du même event | Upsert / pas de doublon destructif ; notification **idempotente** (pas de doublon) |
| **C4 — Compte déjà complet + nouveau challenge** | Ingest pour `demo.user@codepulse.local` (compte déjà finalisé) | Event avec email du demo user + nouveau test | Notification avec lien **feedback** (pas setup), login USER possible immédiatement |

Compléter C1–C4 avec :

| # | Requête | Attendu |
|---|---------|---------|
| C10 | `GET {{spring}}/coding-challenges/get-coding-challenges-pages/page?page=0&size=20&q=Postman` | Retrouve le titre |
| C11 | `GET {{spring}}/coding-challenges/get-coding-challenge/{{challenge_id}}` | Détail |
| C12 | `GET {{spring}}/coding-challenges/tags` | Contient `arrays` (ou tag utilisé) |
| C13 | `GET {{spring}}/coding-challenges/count-coding-challenges` | ≥ 1 |
| C14 | `DELETE {{spring}}/coding-challenges/delete-coding-challenge/{{challenge_id}}` | Soft-delete **200** |
| C15 | `GET` challenge soft-deleted | **404** ou absent des listes actives |
| C16 | `POST {{spring}}/coding-challenges/synchroniser` | **202** / **200** / **207** / **502** selon dispo API externe — documenter le résultat réel |

### 7.3 Cas anormaux ingest / challenges

| # | Cas | Body / action | Attendu |
|---|-----|---------------|---------|
| C20 | Token USER sur ingest | | **403** |
| C21 | Token MANAGER sur ingest | | **403** |
| C22 | Event sans `user` | `[{ "test": {...} }]` | Item en échec / validation (**207** ou erreurs item) |
| C23 | Event sans `test` | | Idem |
| C24 | `test.id` ≤ 0 ou manquant | | Échec validation |
| C25 | `user.id` ≤ 0 | | Échec validation |
| C26 | `test.titre` vide | | Échec validation |
| C27 | `user.email` vide | | Échec validation |
| C28 | JSON mal formé | `{` | **400** |
| C29 | `get-coding-challenge/999999999` | | **404** |
| C30 | Delete id inexistant | | **404** |
| C31 | Conflit d’identité (même email, autre external id conflictuel) | selon règles métier | codes conflit dans résultat item / **409** si remonté |

---

## 8. Notifications (envoi, lecture, statut, relance)

### 8.1 Envoi

| # | Requête | Body | Attendu |
|---|---------|------|---------|
| N1 | `POST {{spring}}/notifications` | `{ "utilisateurId": {{user_id}}, "codingChallengeId": {{challenge_id}} }` | **200** ; `livraisonEmail` = `ENVOYE` / `DESACTIVE` / `ECHEC` ; sauver `notification_id` + `actionUrl` si présent |
| N2 | Même couple user+challenge une 2e fois | | Idempotent : notification déjà existante, **pas de doublon** (`NON_APPLICABLE` / message “déjà existante”) |
| N3 | User ou challenge inexistant | ids inventés | **404** |
| N4 | Token USER | | **403** |

Vérifier aussi l’e-mail :

- GreenMail : `GET {{spring}}/dev/mailbox` ou `/dev/mailbox/json` (profil standalone, SMTP embarqué)
- Gmail : boîte `codepulse.notification.to` (sujet « Feedback demandé » / bouton CTA)

### 8.2 Lecture & filtres

| # | Requête | Qui |
|---|---------|-----|
| N10 | `GET {{spring}}/notifications/get-notification/{{notification_id}}` | USER propriétaire ou admin |
| N11 | `GET .../get-notification-by-utilisateur?utilisateurId={{user_id}}` | |
| N12 | `GET .../get-notifications-by-utilisateur-pages/page?utilisateurId={{user_id}}&page=0&size=10` | |
| N13 | `GET .../get-all-notifications` | challenge / app admin |
| N14 | `GET .../get-notifications-pages/page?page=0&size=10&statut=ENVOYEE` | |
| N15 | `GET .../get-notification-by-statut?statut=EN_ATTENTE` | |
| N16 | `GET .../count` et `.../count/statut?statut=ENVOYEE` | |

**Statuts :** `EN_ATTENTE` · `ENVOYEE` · `ECHEC` · `LUE`

### 8.3 Changement de statut

| # | Cas | Attendu |
|---|-----|---------|
| N20 | `PATCH .../update-statut/{{notification_id}}/statut?statut=LUE` (propriétaire) | **200** |
| N21 | USER modifie la notification d’un **autre** user | **403** |
| N22 | Statut invalide | **400** |

### 8.4 Relance automatique

Conditions métier pour qu’une relance parte :

1. `codepulse.notification.relance.enabled=true`
2. Délai écoulé depuis envoi / dernière relance (`relance.delay`, ex. `24h` — pour tests : `2m`)
3. Statut ∈ `EN_ATTENTE` / `ENVOYEE` / `ECHEC`
4. `nombreRelances < max`
5. **Aucun** feedback `SOUMIS` pour ce user+challenge
6. Notification / user / challenge non soft-deleted

| # | Requête / scénario | Attendu |
|---|--------------------|---------|
| R1 | Config delay court → attendre → scheduler | E-mail « Rappel #1 » ; `nombreRelances` incrémenté ; log type `RELANCE` |
| R2 | `GET {{spring}}/dev/relance/run` (standalone, sans auth) | `{ "sent": N, "message": "..." }` |
| R3 | Relance alors que feedback déjà `SOUMIS` | `sent=0` pour cette notif |
| R4 | Relance avant le délai | `sent=0` |
| R5 | Déjà `nombreRelances = max` | plus de relance |
| R6 | Après relance, ancien lien setup / feedback invalidé → **nouveau** lien dans le mail | vérifier `actionUrl` / inbox |

Logs : `GET {{spring}}/integration-logs/get-integration-logs-by-type?type=RELANCE` (app admin).

---

## 9. Activation de compte (`complete-account`)

Flux cible (souvent après ingest C1) :

1. Notification / e-mail contient un lien `/complete-account?token=...&challengeId=...`
2. `GET /auth/setup-info?token=...`
3. `POST /auth/complete-account`
4. Login possible + feedback

### 9.1 Normaux

| # | Requête | Attendu |
|---|---------|---------|
| S1 | Extraire `setup_token` depuis `actionUrl` / DB / mail | token non vide |
| S2 | `GET {{spring}}/auth/setup-info?token={{setup_token}}` | **200** email / nom / prenom |
| S3 | `POST {{spring}}/auth/complete-account` | **200** JWT |

Body exemple :

```json
{
  "token": "{{setup_token}}",
  "password": "NewPass1234!",
  "nom": "Dupont",
  "prenom": "Alice",
  "userName": "alice.dupont.postman"
}
```

| S4 | `POST /auth/login` avec ce nouvel email/password | **200** |
| S5 | Accès inbox / form feedback | **200** |

### 9.2 Anormaux

| # | Cas | Attendu |
|---|-----|---------|
| S10 | Token manquant / invalide | **400** `JETON_SETUP_INVALIDE` |
| S11 | Token expiré | **400** `JETON_SETUP_EXPIRE` |
| S12 | Compte déjà complété (2e appel) | **400** `COMPTE_DEJA_COMPLETE` |
| S13 | Password trop court | **400** |
| S14 | `userName` déjà pris | **400** `NOM_UTILISATEUR_DEJA_UTILISE` |
| S15 | Profil incomplet (champs requis manquants selon règles) | **400** `PROFIL_INCOMPLET` |

---

## 10. Feedbacks

### 10.1 Préparation

| # | Requête |
|---|---------|
| F0 | Login USER (`token_user`) |
| F1 | `GET {{spring}}/feedbacks/form?challengeId={{challenge_id}}` → récupérer questions obligatoires → sauver `question_id`(s) |
| F2 | `GET {{spring}}/questions-feedback/get-all-questions` (app admin) si besoin de la liste complète |

### 10.2 Soumission normale

```json
{
  "codingChallengeId": {{challenge_id}},
  "noteGlobale": 4.0,
  "commentaire": "Challenge clair, temps un peu juste.",
  "statut": "SOUMIS",
  "reponses": [
    { "questionId": {{question_id}}, "valeur": "4" }
  ]
}
```

| # | Attendu |
|---|---------|
| F10 | **200** + feedback créé ; sauver `feedback_id` |
| F11 | Notification liée passe à **`LUE`** |
| F12 | Plus de relance possible pour cette notif (voir R3) |
| F13 | `GET .../details/{{feedback_id}}` | détail + réponses |
| F14 | `GET .../get-average-note` (admin) | numérique |
| F15 | `GET .../get-feedback-pages/page?page=0&size=10&tag=arrays` | résultats |

Statuts feedback : `EN_COURS` · `NON_SOUMIS` · `SOUMIS`

### 10.3 Cas anormaux feedback

| # | Cas | Attendu |
|---|-----|---------|
| F20 | Token admin (pas USER) sur `/submit` | **403** |
| F21 | Challenge inexistant | **404** |
| F22 | Challenge archivé (soft-delete) | **400** / **410** selon endpoint |
| F23 | Double soumission même challenge (si règle anti-doublon) | **400** validation |
| F24 | Question obligatoire manquante | **400** `VALIDATION_FEEDBACK_ECHOUEE` / `REPONSE_OBLIGATOIRE_MANQUANTE` |
| F25 | NOTE hors plage / non numérique | **400** `NOTE_INVALIDE` |
| F26 | CHOIX hors options | **400** |
| F27 | `noteGlobale` < 0 ou > 5 | **400** |
| F28 | Commentaire > 5000 chars | **400** |
| F29 | USER lit le feedback d’un autre | **403** |
| F30 | `GET /feedbacks/form` sans auth | **401** |

---

## 11. Questions du formulaire (ADMIN_CODEPULSE)

### 11.1 Normaux

| # | Requête | Body / notes |
|---|---------|--------------|
| Q1 | `POST .../questions-feedback/add-question` | NOTE / TEXTE / CHOIX |
| Q2 | `PUT .../update-question/{{question_id}}` | |
| Q3 | `GET .../get-all-questions` | |
| Q4 | `GET .../get-questions-pages/page?page=0&size=10&type=NOTE` | |
| Q5 | `GET .../get-questions-by-obligatoire?obligatoire=true` | |
| Q6 | `DELETE .../delete-question/{{question_id}}` | soft-delete |

Exemple CHOIX :

```json
{
  "libelle": "Recommanderiez-vous ce challenge ?",
  "type": "CHOIX",
  "obligatoire": false,
  "choix": ["Oui", "Non", "Peut-être"]
}
```

### 11.2 Anormaux

| # | Cas | Attendu |
|---|-----|---------|
| Q10 | CHOIX avec < 2 options | **400** `CHOIX_INSUFFISANT` |
| Q11 | Token MANAGER / CHALLENGE ADMIN | **403** |
| Q12 | Update id inexistant | **404** |

---

## 12. Réinitialisation de mot de passe

### 12.1 Flux normal

| # | Étape | Requête | Attendu |
|---|-------|---------|---------|
| P1 | Candidat | `POST {{spring}}/auth/forgot-password` `{ "email":"demo.user@codepulse.local" }` | **200** `resultat=CREE` (ou `DEJA_EN_ATTENTE`) |
| P2 | App admin | `GET {{spring}}/demandes-reinit/page?page=0&size=10&statut=EN_ATTENTE` | Sauver `demande_id` |
| P3a | App admin | `POST {{spring}}/demandes-reinit/{{demande_id}}/send-link` | E-mail lien ; statut `LIEN_ENVOYE` |
| P3b | *Alternative* | `POST .../temporary-password` `{ "temporaryPassword":"TempPass123!" }` | E-mail MDP temp |
| P4 | Anonyme | `GET {{spring}}/auth/reset-info?token={{reset_token}}` | `valide=true` |
| P5 | Anonyme | `POST {{spring}}/auth/reset-password` `{ "token","password":"ResetPass123!" }` | **200** JWT |
| P6 | Login nouveau MDP | | **200** |

### 12.2 Anormaux

| # | Cas | Attendu |
|---|-----|---------|
| P10 | Forgot email inconnu | **200** `IGNORE_COMPTE_INCONNU` (pas de fuite agressive) |
| P11 | Forgot compte incomplet | **200** `IGNORE_COMPTE_INCOMPLET` |
| P12 | 2e forgot alors qu’une demande est en attente | `DEJA_EN_ATTENTE` |
| P13 | `send-link` sur demande déjà traitée | **400** `DEMANDE_DEJA_TRAITEE` |
| P14 | Reset token invalide | **401/400** |
| P15 | Reset token expiré | **400** `JETON_EXPIRE` |
| P16 | Reject demande | `POST .../{{demande_id}}/reject` → statut `REJETEE` |
| P17 | MANAGER appelle `/demandes-reinit` | **403** |

---

## 13. Analytics

Tester **chaque dashboard avec le bon rôle** + 403 avec un mauvais rôle.

| # | Requête | Rôle attendu |
|---|---------|--------------|
| Y1 | `GET {{spring}}/analytics/dashboard/user` | USER |
| Y2 | `GET {{spring}}/analytics/dashboard/challenge-admin` | CHALLENGE / APP ADMIN |
| Y3 | `GET {{spring}}/analytics/dashboard/manager` | MANAGER / APP ADMIN |
| Y4 | `GET {{spring}}/analytics/dashboard/app-admin` | APP ADMIN |
| Y5 | `GET .../average-score-by-tag?tag=arrays` | MANAGER+ |
| Y6 | `GET .../average-scores-by-tags` | |
| Y7 | `GET .../completion-rate-by-tag?tag=arrays` | |
| Y8 | `GET .../feedback-participation` | |
| Y9 | `GET .../top-challenges?limit=5` | |
| Y10 | `GET .../bottom-challenges?limit=5` | |
| Y11 | `GET .../lowest-scoring-tags?limit=5` | |
| Y12 | `GET .../mandatory-question-response-rates` | |
| Y13 | `GET .../challenge-statistics/page?page=0&size=10` | READ_FEEDBACKS |
| Y14 | `GET .../tag-statistics/page?page=0&size=10` | |
| Y15 | `GET .../export?startDate=2024-01-01T00:00:00Z&endDate=2030-01-01T00:00:00Z&format=csv` | fichier / payload |

Anormaux : USER sur `/dashboard/manager` → **403** ; params manquants → **400**.

---

## 14. Logs d’intégration (ADMIN_CODEPULSE)

| # | Requête |
|---|---------|
| L1 | `GET {{spring}}/integration-logs/get-all-integration-logs` |
| L2 | `GET .../get-integration-logs-pages/page?page=0&size=20&type=AUTH` |
| L3 | `GET .../get-integration-logs-by-type?type=ENVOI_NOTIFICATION` |
| L4 | `GET .../get-integration-logs-by-type?type=RELANCE` |
| L5 | `GET .../get-integration-logs-by-type?type=FEEDBACK` |
| L6 | `GET .../get-integration-logs-by-statut?statut=ERREUR` |
| L7 | `GET .../count/failed` |
| L8 | `GET .../get-last-integration-log` |

Types utiles : `AUTH`, `ENVOI_NOTIFICATION`, `RELANCE`, `FEEDBACK`, `SYNC_CHALLENGE`, `GESTION_UTILISATEUR`, `DEMANDE_REINIT`, `EXPORT_DONNEES`, `CONFIG`.

Anormal : token MANAGER → **403**.

---

# PARTIE B — API Recherche (`{{search}}`)

Même JWT Spring : `Authorization: Bearer {{token_app_admin}}` (ou manager / challenge admin).

---

## 15. Health

| # | Requête | Attendu |
|---|---------|---------|
| H1 | `GET {{search}}/health` | `{ "status":"ok" }` |
| H2 | `GET {{search}}/health/ready` | `database`, `pgvector`, `jwt_public_key` true ; `ollama_reachable` true si Ollama tourne |

---

## 16. Search

```json
{
  "query": "feedback stacks",
  "top_k": 10,
  "filters": { "tag": null, "source_type": null }
}
```

| # | Cas | Attendu |
|---|-----|---------|
| SR1 | Recherche libre `feedback stacks` | **200** liste `results` (type, id, title, snippet, score) |
| SR2 | `challenge arrays` + `filters.source_type=CHALLENGE` | challenges arrays |
| SR3 | `filters.tag=arrays` | résultats taggés |
| SR4 | `Qui est Capgemini ?` + `DOCUMENT` | document connaissance si indexé |
| SR5 | Token USER | **403** |
| SR6 | Token challenge admin + `source_type=FEEDBACK` | **403** (rôle ne voit pas FEEDBACK) |
| SR7 | Token manager + `source_type=CHALLENGE` | **403** |
| SR8 | Query vide | **422/400** |
| SR9 | Sans Bearer | **401** |
| SR10 | Index vide / pas de match | **200** `results: []` |

Scopes sources :

| Rôle | Sources autorisées |
|------|--------------------|
| ADMIN_CODEPULSE | CHALLENGE, FEEDBACK, QUESTION, DOCUMENT |
| ADMIN_CODING_CHALLENGE | CHALLENGE, QUESTION, DOCUMENT |
| MANAGER_RH | FEEDBACK, QUESTION, DOCUMENT |

---

## 17. KPI

```json
{ "question": "Quelle est la moyenne des notes ?" }
```

| # | Question | Attendu |
|---|----------|---------|
| K1 | moyenne des notes | `tool=get_average_score`, `value` numérique |
| K2 | taux de participation | `get_participation_rate` |
| K3 | combien de challenges | `count_challenges` |
| K4 | nombre de feedbacks | `count_feedbacks` |
| K5 | phrase sans sens | `tool=null`, pas de chiffre inventé |
| K6 | Token USER | **403** |

---

## 18. Assistant (RAG)

```json
{ "question": "Quels points reviennent dans les commentaires ?" }
```

| # | Cas | Attendu |
|---|-----|---------|
| AS1 | Question métier + Ollama ON | `answer` rédigé + `citations[]` |
| AS2 | Ollama OFF | passages / citations (pas d’erreur fatale) |
| AS3 | `hello` / chitchat | réponse généraliste, citations vides |
| AS4 | Question Capgemini (après knowledge) | citations DOCUMENT |
| AS5 | Token USER | **403** |

---

## 19. Knowledge + ingestion

| # | Requête | Body / notes | Rôles |
|---|---------|--------------|-------|
| KN1 | `GET {{search}}/knowledge/documents` | liste | admins |
| KN2 | `POST {{search}}/knowledge/documents` | create | **APP ADMIN ou MANAGER** |
| KN3 | `PUT {{search}}/knowledge/documents/{{knowledge_doc_id}}` | update | idem |
| KN4 | `DELETE .../{{knowledge_doc_id}}` | soft-delete **204** | idem |
| KN5 | Challenge admin `POST` knowledge | | **403** |
| KN6 | `POST {{search}}/ingestion/sync` `{ "full": false }` | incremental | admins |
| KN7 | `POST .../ingestion/sync` `{ "full": true }` | full reindex | |
| KN8 | `GET {{search}}/ingestion/status` | learner ON, last_result | |

Body create :

```json
{
  "title": "Capgemini — note Postman",
  "body": "Capgemini est un groupe de conseil et services numériques.",
  "category": "company",
  "tags": "capgemini,company,postman"
}
```

Après KN2 : relancer SR4 / AS4 pour vérifier l’indexation.

---

# PARTIE C — Scénarios End-to-End (à automatiser en dossiers Postman)

---

## E2E-1 — Candidat nouveau (USER incomplet → setup → feedback)

1. App admin / challenge admin : `ingest-batch` (**cas C1**)
2. Vérifier notification créée + e-mail / `actionUrl`
3. `setup-info` → `complete-account`
4. Login nouveau user
5. `GET /feedbacks/form`
6. `POST /feedbacks/submit` (`SOUMIS`)
7. Notification → `LUE`
8. `GET /dev/relance/run` → pas de relance pour cette notif
9. (Optionnel) search admin : retrouver le commentaire

## E2E-2 — Candidat déjà connu (demo.user) + nouveau challenge

1. Ingest pour `demo.user@codepulse.local` (**cas C4**)
2. Notification avec lien feedback (compte déjà complet)
3. Login `Demo1234!`
4. Inbox → form → submit
5. Analytics manager : participation / scores mis à jour

## E2E-3 — Admin challenge : sync + soft-delete + droits

1. Login challenge admin
2. `synchroniser` + `ingest-batch` OK
3. Lister challenges / notifications / feedbacks
4. Soft-delete un challenge
5. Vérifier **403** sur `/utilisateurs`, `/questions-feedback`, `/demandes-reinit`, `/integration-logs`
6. Search OK sur CHALLENGE ; **403** si filter FEEDBACK

## E2E-4 — Manager RH : analytics + search feedbacks + knowledge

1. Login manager
2. Dashboards manager + export CSV
3. Lister feedbacks ; **403** sur sync challenges / users
4. Search FEEDBACK OK ; CHALLENGE filter **403**
5. Créer document knowledge → search Capgemini
6. KPI moyenne / participation

## E2E-5 — App admin : gouvernance complète

1. CRUD users staff
2. CRUD questions
3. Traiter demande reset (send-link ou temporary-password)
4. Lire logs AUTH / ENVOI_NOTIFICATION / RELANCE / FEEDBACK
5. Dashboard app-admin
6. Search toutes sources + ingestion sync full
7. Relance manuelle `/dev/relance/run`

## E2E-6 — Relance (démo contrôlée)

1. Créer notification **sans** soumettre de feedback
2. Config `relance.delay=2m` (ou forcer `dateEnvoi` ancien en DB pour démo)
3. `GET /dev/relance/run`
4. Vérifier e-mail « Rappel #N » + `nombreRelances`
5. Vérifier nouveau lien (setup ou feedback)
6. Soumettre feedback → relances suivantes ignorées

## E2E-7 — Non-régression sécurité (smoke 403/401)

Pour **chaque** endpoint sensible listé dans ce doc :

- sans token → **401**
- mauvais rôle → **403**

Minimum : users, ingest, notifications POST, questions, demandes, logs, search, knowledge write.

---

## 20. Checklist finale (cocher avant livraison)

### Socle

- [ ] Health Spring + Search ready
- [ ] Login des 4 rôles
- [ ] Matrice 401 / 403

### Challenges (4 cas)

- [ ] C1 nouvel user + nouveau challenge
- [ ] C2 user existant + nouveau challenge
- [ ] C3 idempotence même event
- [ ] C4 user déjà complet + nouveau challenge
- [ ] Validation ingest (champs manquants / ids invalides)
- [ ] Soft-delete challenge

### Notifications & relance

- [ ] Envoi + e-mail
- [ ] Idempotence
- [ ] Lecture / filtres / patch statut
- [ ] Relance (scheduler ou `/dev/relance/run`)
- [ ] Stop relance après feedback SOUMIS

### Compte & feedback

- [ ] setup-info + complete-account
- [ ] Cas tokens invalides / expirés
- [ ] Form + submit OK
- [ ] Validations obligatoires / NOTE / CHOIX
- [ ] Notification → LUE

### Admin RH / App

- [ ] Questions CRUD + CHOIX < 2
- [ ] Users CRUD + refus rôle USER
- [ ] Forgot / send-link / temp password / reject / reset
- [ ] Analytics 4 dashboards
- [ ] Logs intégration

### Recherche intelligente

- [ ] Search multi-rôles + filtres
- [ ] KPI SQL (et tool null)
- [ ] Assistant (+/- Ollama)
- [ ] Knowledge CRUD + sync
- [ ] Scopes FEEDBACK/CHALLENGE par rôle

### E2E

- [ ] E2E-1 à E2E-7 exécutés et documentés (pass/fail)

---

## 21. Exemples de assertions Postman (réutiliser)

```javascript
// Status
pm.test("Status 200", () => pm.response.to.have.status(200));

// JWT
pm.test("Has accessToken", () => {
  pm.expect(pm.response.json().accessToken).to.be.ok;
});

// Forbidden
pm.test("Forbidden for this role", () => pm.response.to.have.status(403));

// Search results shape
pm.test("Search results array", () => {
  const j = pm.response.json();
  pm.expect(j.results).to.be.an("array");
});

// KPI never invents when unmatched
pm.test("KPI tool null or named", () => {
  const j = pm.response.json();
  pm.expect(j).to.have.property("tool");
  pm.expect(j).to.have.property("value");
});
```

---

## 22. Notes importantes pour le testeur

1. **Ne pas confondre** Spring (`:8080`) et Search (`:8090`). Un **502** sur `/search` depuis le front = search non démarré (`run.bat`).
2. Les **candidats USER** ne sont pas créés via `/utilisateurs/add-user` : ils arrivent par **ingest / Kafka**.
3. Les tags de démo sont du type `arrays`, `trees`, `stacks`, `dp` — pas « Java ».
4. Pour des relances rapides en démo : `codepulse.notification.relance.delay=2m` puis `/dev/relance/run`.
5. Même clé JWT (`public.key`) pour Spring et Search.
6. Ce document décrit le comportement du code actuel ; si un status code diffère légèrement (ex. 207 vs 200 sur batch partiel), **noter le résultat réel** dans le rapport de tests.

---

*CodePulse — plan de tests Postman application complète*  
*Couvre : auth, 4 rôles, ingest challenges (4 cas), notifications, relances, complete-account, feedbacks, questions, reset MDP, analytics, logs, recherche / KPI / assistant / knowledge.*
