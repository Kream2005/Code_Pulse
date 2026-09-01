# Prompt pour Claude — Rédaction du mémoire CodePulse (LaTeX IID)

Tu rédiges le **contenu** du mémoire de fin d’études (Diplôme d’Ingénieur d’État, filière **Informatique et Ingénierie des Données**, ENSA) dans le fichier unique :

`Code_Pulse/rapport/main.tex`

Le template LaTeX (`iid.cls`) et la structure des chapitres sont **déjà en place**. Remplace uniquement les blocs marqués :

```latex
% <<< CLAUDE: ... >>>
```

Ne réinvente pas la mise en page. Ne crée pas de fichiers `.tex` supplémentaires.  
Les figures pointent déjà vers `images/*.png` : assume que les PNG seront fournis (PlantUML déjà prêts dans `rapport/plantuml/`).

---

## Style d’écriture (obligatoire)

1. **Français** pour tout le corps (sauf Abstract EN + résumé AR court).
2. **Humain** : phrases naturelles d’étudiant ingénieur en stage, pas de ton marketing, pas de « révolutionnaire / innovant / cutting-edge » à chaque paragraphe.
3. **Seulement le nécessaire** : pas de remplissage, pas de lorem, pas de digressions théoriques hors sujet (pas de cours Kafka de 10 pages). Une notion = 1 définition courte + pourquoi on l’a choisie ici.
4. **Factuel** : base-toi **uniquement** sur le brief projet ci-dessous. N’invente pas de features, chiffres KPI réels, noms de managers, ou résultats d’évaluation non fournis. Si une info personnelle manque (nom, encadrants, dates), laisse un `\textit{[À compléter]}`.
5. Longueur indicative raisonnable pour un PFE : introduction ~2–3 p., chaque chapitre ~10–20 p. équivalent texte (sans gonfler), conclusion ~1–2 p.
6. Cite la biblio avec `\cite{...}` quand tu mentionnes une techno de façon formelle (`springboot`, `fastapi`, `pgvector`, `rrf2009`, `minilm2020`, `kafka`, `ollama`, `react`).
7. Utilise le glossaire quand c’est naturel : `\gls{jwt}`, `\gls{rag}`, etc.

---

## Identité du document (placeholders à respecter)

- Titre déjà proposé dans `main.tex` (tu peux l’affiner légèrement s’il est trop long).
- Étudiant / encadrants / jury / date : `[À compléter]` sauf si l’utilisateur te les donne.
- Contexte stage : plateforme **CodePulse**, usage type entreprise (ex. Capgemini / cabinet de services numériques) pour collecter les feedbacks **après** des coding challenges. Le challenge lui-même se passe **hors** CodePulse.

---

## Brief projet — de 0 à 100 (source de vérité)

### 1. Problème métier

Après un coding challenge (plateforme externe), l’organisation a besoin de :

- notifier le candidat pour qu’il donne son avis ;
- activer un compte si le candidat n’existe pas encore ;
- collecter un feedback structuré (note, commentaire, questions) ;
- relancer si pas de réponse ;
- analyser les retours (RH / managers) ;
- plus récemment : **chercher sémantiquement** dans challenges / feedbacks / questions / fiches entreprise, poser des **KPI en langage naturel**, et un **assistant** avec citations.

Sans CodePulse : mails ad hoc, pas de rôles, pas d’audit, pas de relance contrôlée, pas d’analytics unifiés.

### 2. Solution CodePulse (vue d’ensemble)

Application full-stack :

| Couche | Techno | Port |
|--------|--------|------|
| Frontend | React 19, Vite, Tailwind, i18n FR/EN, thème clair/sombre | 4200 |
| Backend métier | Spring Boot (Java 21), JPA, Security OAuth2 Resource Server JWT RS256 | 8080 |
| Messaging | Apache Kafka (topic `coding-challenges` + DLT) | 9092 |
| E-mail | GreenMail embarqué (standalone `:1025` + `/dev/mailbox`) **ou** SMTP réel (ex. Gmail) | — |
| Recherche / KPI / RAG | FastAPI `codepulse-search`, MiniLM, pgvector, Ollama optionnel | 8090 |
| BDD | **Un seul** PostgreSQL (+ extension **pgvector**) | 5432 |
| Publisher démo | Python `challenge-publisher` (HTTP et/ou Kafka) | 9999 |

**Modes** : `codepulse.mode=standalone` (laptop démo, Kafka binaire, GreenMail) ou `full`. Flags : `kafka.enabled`, `notification.enabled`, `external-api.enabled`.

**Pas d’inscription libre** : invite-only (ingest / admin). Soft-delete partout où pertinent (feedbacks conservés).

### 3. Quatre rôles

| Rôle | Compte démo | Capacités principales |
|------|-------------|------------------------|
| `USER` | `demo.user@codepulse.local` / `Demo1234!` | Inbox, feedback, mes feedbacks, profil, forgot password |
| `ADMIN_CODING_CHALLENGE` | `challenge.admin@...` / `Challenge1234!` | Sync/ingest/archive challenges, notifications, lire feedbacks |
| `MANAGER_RH` | `manager.rh@...` / `Manager1234!` | Feedbacks, analytics, **pas** users/questions/logs/sync |
| `ADMIN_CODEPULSE` | `admin@...` / `Admin1234!` | Tout admin + users staff, questions, demandes reset, logs, analytics |

Compte incomplet démo : `pending.setup@codepulse.demo` (pour setup / relance).

### 4. Flux métier centraux

#### 4.1 Ingest challenge → notification (4 cas à expliquer)

Événement `CodingChallengeEvent { user, test }`.

Voies : Kafka consumer **ou** `POST /coding-challenges/ingest-batch` **ou** sync externe `POST /coding-challenges/synchroniser` (puis Kafka).

Pipeline : upsert user + challenge → `notifyChallengeCompletion` → e-mail HTML (bouton CTA FR) si `notification.enabled`.

**Quatre cas objets :**

1. **Nouvel utilisateur + nouveau challenge** → user souvent `compteComplet=false`, setupToken, mail lien `/complete-account?token=&challengeId=`
2. **Utilisateur existant + nouveau challenge** → nouvelle notif pour ce couple
3. **Même user + même challenge** → **idempotent** (pas de doublon notif)
4. **User déjà complet + nouveau challenge** → mail lien `/feedback/form?challengeId=`

Validation : user/test requis, ids > 0, titre et email non vides.

#### 4.2 Complete-account

`GET /auth/setup-info?token=` → `POST /auth/complete-account` → JWT.  
Erreurs : jeton invalide / expiré / compte déjà complet / username pris.

#### 4.3 Feedback

`GET /feedbacks/form` puis `POST /feedbacks/submit` (rôle USER).  
Types questions : `NOTE`, `TEXTE`, `CHOIX` (≥2 options).  
Si `SOUMIS` : notification liée → `LUE` ; **plus de relance**.

#### 4.4 Relance

Scheduler si `relance.enabled` ; critères : délai depuis envoi/dernière relance, statut EN_ATTENTE/ENVOYEE/ECHEC, `nombreRelances < max`, **pas** de feedback SOUMIS.  
Mail « Rappel #N » + **nouveau** lien. Démo manuelle : `GET /dev/relance/run` (standalone).

#### 4.5 Reset MDP

Forgot → demande `EN_ATTENTE` → admin send-link **ou** temporary-password **ou** reject → reset-password.

#### 4.6 Analytics

Dashboards distincts par rôle (`/analytics/dashboard/...`) : notes, participation, tags, top/bottom challenges, export CSV, etc.

#### 4.7 Logs d’intégration

Types : AUTH, ENVOI_NOTIFICATION, RELANCE, FEEDBACK, SYNC_CHALLENGE, CONFIG, etc.

### 5. E-mails

- HTML FR soigné (bouton CTA compatible Gmail/Outlook).
- `notification.to` si renseigné → **tous** les mails vers cette adresse (attention démo).
- Standalone : GreenMail + page `/dev/mailbox`.
- Relances throttlables (ex. delay 48h prod / 2m démo).

### 6. Frontend (pages clés)

- Candidat : login, complete-account, inbox, feedback/form, my-feedback, profile, forgot-password.
- Admin : dashboard, challenges, notifications, feedbacks, analytics, users, password-requests, questions, logs.
- **Recherche intelligente** : `/admin/smart-search` (onglets Recherche / KPI / Assistant / Connaissance).
- Vite proxy : Spring `:8080`, search `:8090` (`/search`, `/kpi`, `/assistant`, `/knowledge`, `/ingestion`, `/health`).

### 7. codepulse-search (détail technique important)

Service Python séparé, **même Postgres**, **même JWT RS256** (`public.key` Spring).

**Capabilities :**

1. **Recherche hybride** : embedding MiniLM (`all-MiniLM-L6-v2`, 384 dim) + FTS/keyword → fusion **RRF** → un résultat par source.
2. **KPI conversationnels** : routeur par mots-clés (FR/EN) vers outils SQL (`get_average_score`, `get_participation_rate`, `count_challenges`, …). **Jamais** de chiffre inventé ; si pas d’outil → `tool: null`.
3. **Assistant RAG** : retrieve → Ollama (`llama3.2:1b`) pour prose si joignable ; sinon passages + citations. Chitchat (`hello`, etc.) → réponse guide **sans** dump d’index. Snippets nettoyés (pas Keywords/headers bruts).
4. **Knowledge** : fiches entreprise (`knowledge_document`, catégorie `company`) ; import `.md`/`.txt` ; UI Connaissance.
5. **Continuous learning** : sync incrémental par hash (`search_index_state`), learner périodique (~120 s), `POST /ingestion/sync` `{full:false|true}`.

**Scopes search par rôle :**

- ADMIN_CODEPULSE : CHALLENGE, FEEDBACK, QUESTION, DOCUMENT  
- ADMIN_CODING_CHALLENGE : CHALLENGE, QUESTION, DOCUMENT (pas FEEDBACK)  
- MANAGER_RH : FEEDBACK, QUESTION, DOCUMENT (pas CHALLENGE)  
- USER : **pas** d’accès API search  

Table `search_chunk` **owned** par le service search.

**Contraintes démo laptop Windows** : open-source only, pas Docker obligatoire, ML deps séparées (`requirements-ml.txt`), modèle MiniLM parfois fourni hors-ligne (~80 Mo zip).

### 8. Sécurité

- JWT RS256, claims `sub` (email), `roles`, `uid`, `iss=codepulse-dev`.
- `@PreAuthorize` par endpoint.
- Soft-delete ; ownership sur notifs/feedbacks USER.

### 9. Tests

Plan Postman complet : `docs/POSTMAN-FULL-APP-TEST-PLAN.md`  
Mentionne la stratégie (4 rôles, cas normaux/anormaux, E2E ingest→mail→setup→feedback→relance→search) **sans recopier** tout le plan.

### 10. Difficultés réelles rencontrées (à citer si tu écris le chap. réalisation)

- Extension **pgvector** absente sur Windows → installer binaires / compiler, puis `CREATE EXTENSION vector`.
- Téléchargement MiniLM bloqué → transfert zip local + `EMBEDDING_MODEL_NAME=./models/...`.
- Front **502** sur `/search` → service `run.bat` (8090) non démarré (≠ Ollama).
- Relances « invisibles » → délai 24h ; pour démo `delay=2m` ou `/dev/relance/run`.
- Gmail : app password + port 587 parfois filtré réseau entreprise.
- `@` dans password Postgres → URL-encode `%40` dans `.env` search.

### 11. Ce qui n’est PAS dans le projet

- Pas de plateforme d’exécution du coding challenge (seulement l’événement de fin).
- Pas d’API LLM cloud payante par défaut.
- Pas d’obligation Docker pour la démo standalone.

---

## Structure à remplir dans `main.tex` (rappel)

1. Dédicace, remerciements  
2. Résumé FR + mots-clés  
3. Abstract EN + keywords  
4. Résumé AR (court ; ou `[À compléter]` si tu ne maîtrises pas l’arabe — **ne fabrique pas** un mauvais arabe)  
5. Introduction générale  
6. **Chapitre 1** — Contexte, problématique, objectifs, acteurs/CU, EF/ENF, méthodo  
7. **Chapitre 2** — Architecture, choix techno, classes, séquences (ingest, feedback, relance, RAG), sécurité, modes  
8. **Chapitre 3** — Réalisation modules, UI (référencer captures), search, mails, tests, difficultés, perspectives  
9. Conclusion générale  
10. Bibliographie déjà branchée (`biblio.bib`)

Chaque chapitre a déjà Introduction / Conclusion section*.

---

## Figures — mapping PlantUML → PNG

Tu n’exportes pas les images. Indique juste dans le texte qu’elles sont générées depuis :

| Fichier PlantUML | Image attendue dans `images/` |
|------------------|-------------------------------|
| `plantuml/07_cas_utilisation.puml` | `cas_utilisation.png` |
| `plantuml/01_architecture_globale.puml` | `architecture_globale.png` |
| `plantuml/02_diagramme_classes.puml` | `diagramme_classes.png` |
| `plantuml/03_sequence_ingest_notification.puml` | `sequence_ingest_notif.png` |
| `plantuml/04_sequence_feedback.puml` | `sequence_feedback.png` |
| `plantuml/05_sequence_relance.puml` | `sequence_relance.png` |
| `plantuml/06_sequence_rag.puml` | `sequence_rag.png` |

Captures UI (à fournir par l’étudiant) : `screenshot_login.png`, `screenshot_inbox.png`, `screenshot_feedback_form.png`, `screenshot_smart_search.png`, `screenshot_analytics.png`, `screenshot_mailbox.png`.

Si une image manque encore, garde le `\includegraphics{...}` (ne commente pas toute la figure).

---

## Format de ta réponse

1. Produis le **contenu LaTeX prêt à coller** section par section (ou un `main.tex` complet mis à jour).  
2. N’inclus pas d’explications méta longues après coup — le livrable **est** le LaTeX.  
3. Signale en fin de message une courte liste `[À compléter]` (noms, dates, résumé arabe, captures).

---

## Checklist qualité avant de rendre

- [ ] Tous les `% <<< CLAUDE` remplacés (sauf si info personnelle manquante)  
- [ ] Aucune feature inventée  
- [ ] Les 4 cas d’ingest sont expliqués  
- [ ] Relance, soft-delete, JWT, scopes search par rôle mentionnés  
- [ ] Search / KPI / RAG / knowledge / continuous learning présents mais proportionnés  
- [ ] Ton humain, français correct, pas de blabla  
- [ ] `\cite` utilisés à bon escient  
- [ ] Figures référencées avec `\ref{fig:...}`

---

**Commence maintenant par remplir `main.tex` en respectant strictement ce brief.**
