# CodePulse — fonctionnalités & rôles

Plateforme de collecte et d’analyse des feedbacks après coding challenges.

## Modes d’exécution

| Mode | Propriété | Prérequis | Ingestion challenges |
|------|-----------|-----------|----------------------|
| Standalone | `codepulse.mode=standalone` | PostgreSQL, JDK, Node, binaire Kafka local | Kafka (+ publisher HTTP) |
| Full | `codepulse.mode=full` | PostgreSQL, Kafka (+ Mailpit optionnel) | Kafka (et/ou sync HTTP → Kafka) |

PostgreSQL est **toujours** utilisé. Kafka et les e-mails restent commutables via `codepulse.kafka.enabled` / `codepulse.notification.enabled`.

Les notifications non lues sont **relancées automatiquement** (délai 24h, max 3, nouveau lien compte ou feedback).

---

## Comptes de démo (seed)

| Rôle | Email | Mot de passe |
|------|-------|--------------|
| Utilisateur (candidat) | `demo.user@codepulse.local` | `Demo1234!` |
| Admin Coding Challenge | `challenge.admin@codepulse.local` | `Challenge1234!` |
| Manager / RH | `manager.rh@codepulse.local` | `Manager1234!` |
| Admin application | `admin@codepulse.local` | `Admin1234!` |

---

## Rôle : Utilisateur (candidat / collaborateur)

**Objectif :** passer un coding challenge (source externe), recevoir une notification, fournir un feedback.

| Fonctionnalité | Page / flux |
|----------------|-------------|
| Activation de compte (invite via challenge) | `/complete-account` |
| Connexion | `/login` |
| Inbox des notifications de challenges | `/inbox` |
| Remplir le formulaire de feedback | `/feedback/form` |
| Historique de ses feedbacks | `/my-feedback` |
| Profil (email, nom, prénom, username) | `/profile` |
| Demande de réinitialisation MDP | `/forgot-password` → traitement admin |

Le coding challenge lui-même est passé hors CodePulse ; CodePulse reçoit l’événement (Kafka ou HTTP) et crée la notification.

---

## Rôle : Administrateur Coding Challenge

**Objectif :** paramétrer les tests existants, consulter les feedbacks associés.

| Fonctionnalité | Page |
|----------------|------|
| Tableau de bord admin | `/admin` |
| Liste / sync / archivage des challenges | `/admin/challenges` |
| Toutes les notifications | `/admin/notifications` |
| Consultation des feedbacks | `/admin/feedbacks` |
| Profil | `/profile` |

API : sync `POST /coding-challenges/synchroniser`, soft-delete challenges, lecture feedbacks.

---

## Rôle : Manager / RH

**Objectif :** analyser les retours, consulter tableaux de bord et indicateurs.

| Fonctionnalité | Page |
|----------------|------|
| Tableau de bord (ex. taux de participation) | `/admin` |
| Consultation des feedbacks | `/admin/feedbacks` |
| Analytics (scores par tag, top challenges, stats) | `/admin/analytics` |
| Profil | `/profile` |

Pas d’accès à la gestion des questions, utilisateurs, logs d’intégration ni sync challenges.

---

## Rôle : Administrateur application feedback

**Objectif :** gérer les questions, configurer / superviser l’intégration et l’application.

| Fonctionnalité | Page |
|----------------|------|
| Tableau de bord | `/admin` |
| Gestion des comptes staff | `/admin/users` |
| Demandes de réinit. mot de passe | `/admin/password-requests` |
| Challenges (sync / archive) | `/admin/challenges` |
| Notifications globales | `/admin/notifications` |
| Feedbacks | `/admin/feedbacks` |
| Questions du formulaire | `/admin/questions` |
| Logs d’intégration / audit | `/admin/logs` |
| Analytics | `/admin/analytics` |
| Profil | `/profile` |

Supervision : flags démarrage (`codepulse.mode`, kafka, notification, external-api) tracés en logs `CONFIG`.

---

## KPIs tableaux de bord (par rôle)

| Rôle | Endpoint | Indicateurs |
|------|----------|-------------|
| USER | `GET /analytics/dashboard/user` | notifications, en attente, feedbacks soumis |
| Admin Coding Challenge | `GET /analytics/dashboard/challenge-admin` | challenges actifs/archivés, notifications, feedbacks, note moyenne |
| Manager RH | `GET /analytics/dashboard/manager` | participation, challenges, feedbacks, note, pending, tags |
| Admin app | `GET /analytics/dashboard/app-admin` | staff, candidats, questions, réinit., erreurs, kafka/mail, challenges, feedbacks |

Service : `AnalyticsService` / `AnalyticsServiceImp`.

## Fonctionnalités transverses

- Authentification JWT, invite-only (pas d’inscription libre)
- Soft-delete (challenges, users, questions, notifications, feedbacks) avec conservation des feedbacks archivés
- Journal d’intégration (sync, auth, feedback, config, etc.)
- i18n FR / EN, thème clair / sombre
- Pagination sur les listes principales
- Publisher de challenges : HTTP et/ou Kafka (`challenge-publisher/`)

---

## Matrice pages × rôles

| Page | USER | ADMIN_CODING_CHALLENGE | MANAGER_RH | ADMIN_CODEPULSE |
|------|:----:|:----------------------:|:----------:|:---------------:|
| Inbox / feedback / mes feedbacks | ✓ | | | |
| Profil | ✓ | ✓ | ✓ | ✓ |
| Dashboard admin | | ✓ | ✓ | ✓ |
| Challenges + notifications admin | | ✓ | | ✓ |
| Feedbacks admin | | ✓ | ✓ | ✓ |
| Analytics | | | ✓ | ✓ |
| Users + password requests | | | | ✓ |
| Questions | | | | ✓ |
| Logs | | | | ✓ |
