#!/usr/bin/env python3
"""Generate CodePulse-Full-Showcase.postman_collection.json."""

from __future__ import annotations

import json
from pathlib import Path

OUT = Path(__file__).with_name("CodePulse-Full-Showcase.postman_collection.json")

SAVE_TOKEN = """
pm.test('Status 200', () => pm.response.to.have.status(200));
const j = pm.response.json();
pm.expect(j.accessToken).to.be.a('string');
pm.environment.set('token', j.accessToken);
""".strip()

SAVE_TOKEN_APP_ADMIN = SAVE_TOKEN + "\npm.environment.set('token_app_admin', j.accessToken);"
SAVE_TOKEN_CHALLENGE = SAVE_TOKEN + "\npm.environment.set('token_challenge_admin', j.accessToken);"
SAVE_TOKEN_MANAGER = SAVE_TOKEN + "\npm.environment.set('token_manager', j.accessToken);"
SAVE_TOKEN_USER = SAVE_TOKEN + "\npm.environment.set('token_user', j.accessToken);"

INGEST_BODY = """[
  {
    "user": {
      "id": {{external_user_id}},
      "nom": "Dupont",
      "prenom": "Alice",
      "userName": "alice.dupont.postman",
      "email": "{{candidate_email}}",
      "status": true
    },
    "test": {
      "id": {{external_test_id}},
      "titre": "Two Sum Postman",
      "description": "Find two numbers that add up to target.",
      "tag": "arrays",
      "duree": 45,
      "codeUrl": "https://example.com/two-sum",
      "parameter": false
    }
  }
]"""

INGEST_TESTS = """
pm.test('Ingest succeeded', () => {
  pm.expect(pm.response.code).to.be.oneOf([200, 207]);
  const j = pm.response.json();
  pm.expect(j.succeeded).to.be.at.least(1);
  const item = j.items[0];
  pm.environment.set('user_id', String(item.userId));
  pm.environment.set('challenge_id', String(item.challengeId));
  pm.environment.set('external_user_id', String(item.userExternalId));
  pm.environment.set('external_test_id', String(item.testExternalId));
  pm.environment.set('candidate_email', item.userEmail);
});
""".strip()

NOTIF_SEND_TESTS = """
pm.test('Notification response', () => pm.response.to.have.status(200));
const j = pm.response.json();
pm.expect(j.urlAction, 'urlAction missing — copy token manually from DB/email').to.be.a('string');
if (j.notification && j.notification.id) {
  pm.environment.set('notification_id', String(j.notification.id));
}
const url = j.urlAction || '';
const m = url.match(/[?&]token=([^&]+)/);
pm.expect(m, 'No token= in urlAction: ' + url).to.be.ok;
pm.environment.set('setup_token', decodeURIComponent(m[1]));
console.log('setup_token saved:', m[1]);
const m2 = url.match(/[?&]challengeId=(\\d+)/);
if (m2) pm.environment.set('challenge_id', m2[1]);
""".strip()

SETUP_INFO_PREREQUEST = """
const t = pm.environment.get('setup_token');
if (!t || t.includes('{') || t.length < 30) {
  throw new Error(
    'setup_token is empty or not resolved. Run POST /notifications first and check the Tests tab.'
  );
}
""".strip()

SETUP_INFO_TESTS = """
pm.test('Setup info OK', () => pm.response.to.have.status(200));
const j = pm.response.json();
pm.expect(j.email).to.eql(pm.environment.get('candidate_email'));
""".strip()

COMPLETE_ACCOUNT_TESTS = """
pm.test('Account completed', () => pm.response.to.have.status(200));
const j = pm.response.json();
pm.environment.set('token_user', j.accessToken);
console.log('token_user saved — use for feedback; re-login admin before search steps');
""".strip()

FEEDBACK_FORM_PREREQUEST = """
const tok = pm.environment.get('token_user');
if (!tok) throw new Error('token_user missing — run step 6 complete-account first');
pm.environment.set('token', tok);
""".strip()

USE_ADMIN_TOKEN = """
const tok = pm.environment.get('token_app_admin');
if (!tok) throw new Error('token_app_admin missing — run step 1 Login APP ADMIN first');
pm.environment.set('token', tok);
""".strip()

MAILBOX_TOKEN_TESTS = """
pm.test('Mailbox JSON', () => pm.response.to.have.status(200));
const mails = pm.response.json();
pm.expect(mails).to.be.an('array');
if (mails.length) {
  const body = mails[0].body || '';
  const m = body.match(/token=([a-f0-9-]{36})/i);
  if (m) pm.environment.set('setup_token', m[1]);
  const r = body.match(/reset[^"]*token=([a-f0-9-]{36})/i);
  if (r) pm.environment.set('reset_token', r[1]);
}
""".strip()

FEEDBACK_FORM_TESTS = """
pm.test('Feedback form', () => pm.response.to.have.status(200));
const j = pm.response.json();
if (j.questions && j.questions.length) {
  const q = j.questions.find(x => x.obligatoire) || j.questions[0];
  pm.environment.set('question_id', String(q.id));
}
""".strip()

SUBMIT_FEEDBACK_PREREQUEST = """
const qid = pm.environment.get('question_id');
if (!qid || qid.includes('{')) {
  throw new Error('question_id is empty. Run GET /feedbacks/form first (with token_user Bearer).');
}
const tok = pm.environment.get('token_user');
if (tok) pm.environment.set('token', tok);
""".strip()

SUBMIT_FEEDBACK_BODY = """{
  "codingChallengeId": {{challenge_id}},
  "noteGlobale": 4.5,
  "commentaire": "Challenge clair — test Postman.",
  "statut": "SOUMIS",
  "reponses": [
    { "questionId": {{question_id}}, "valeur": "4" }
  ]
}"""


def req(name: str, method: str, url: str, body: str | None = None, tests: str | None = None, auth: bool = True, prerequest: str | None = None):
    item: dict = {
        "name": name,
        "request": {
            "method": method,
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "url": url,
        },
    }
    if auth:
        item["request"]["auth"] = {
            "type": "bearer",
            "bearer": [{"key": "token", "value": "{{token}}", "type": "string"}],
        }
    if body is not None:
        item["request"]["body"] = {"mode": "raw", "raw": body}
    events = []
    if prerequest:
        events.append({"listen": "prerequest", "script": {"exec": prerequest.split("\n"), "type": "text/javascript"}})
    if tests:
        events.append({"listen": "test", "script": {"exec": tests.split("\n"), "type": "text/javascript"}})
    if events:
        item["event"] = events
    return item


def folder(name: str, items: list, description: str = ""):
    f = {"name": name, "item": items}
    if description:
        f["description"] = description
    return f


collection = {
    "info": {
        "name": "CodePulse Full Showcase",
        "description": (
            "Collection complète pour démo manager / soutenance. "
            "Importer avec CodePulse-Local.postman_environment.json. "
            "Voir docs/POSTMAN-SHOWCASE-GUIDE.md pour l'ordre d'exécution et le mode DB create."
        ),
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "item": [
        folder(
            "00 — Setup & Health",
            [
                req("Spring — reachable (GET /dev/relance/run)", "GET", "{{spring}}/dev/relance/run",
                    tests="pm.test('Spring reachable', () => pm.response.to.have.status(200));",
                    auth=False),
                req("Search — health", "GET", "{{search}}/health", tests="pm.test('Search up', () => pm.response.to.have.status(200));", auth=False),
                req("Search — ready (DB + Ollama)", "GET", "{{search}}/health/ready",
                    tests="pm.test('Search ready', () => { pm.response.to.have.status(200); pm.expect(pm.response.json().database).to.eql(true); });",
                    auth=False),
            ],
            "Vérifier que Spring (:8080) et Search (:8090) tournent avant la démo.",
        ),
        folder(
            "01 — Auth (4 rôles)",
            [
                req("Login APP ADMIN", "POST", "{{spring}}/auth/login",
                    '{"email":"{{email_app_admin}}","password":"{{password_app_admin}}"}', SAVE_TOKEN_APP_ADMIN, auth=False),
                req("Login CHALLENGE ADMIN", "POST", "{{spring}}/auth/login",
                    '{"email":"{{email_challenge_admin}}","password":"{{password_challenge_admin}}"}', SAVE_TOKEN_CHALLENGE, auth=False),
                req("Login MANAGER RH", "POST", "{{spring}}/auth/login",
                    '{"email":"{{email_manager}}","password":"{{password_manager}}"}', SAVE_TOKEN_MANAGER, auth=False),
                req("Login USER (demo.user)", "POST", "{{spring}}/auth/login",
                    '{"email":"{{email_demo_user}}","password":"{{password_demo_user}}"}', SAVE_TOKEN_USER, auth=False),
                req("GET /api/me (token courant)", "GET", "{{spring}}/api/me",
                    tests="pm.test('Profile OK', () => pm.response.to.have.status(200));"),
                req("GET /api/profile", "GET", "{{spring}}/api/profile"),
                req("Login — mauvais mot de passe (401, test volontaire)", "POST", "{{spring}}/auth/login",
                    '{"email":"{{email_app_admin}}","password":"IntentionallyWrongPassword!"}',
                    "pm.test('401', () => pm.response.to.have.status(401));", auth=False),
            ],
        ),
        folder(
            "02 — Ingest challenges (4 cas métier)",
            [
                req("C1 — BOTH_NEW (nouvel user + nouveau challenge)", "POST", "{{spring}}/coding-challenges/ingest-batch",
                    INGEST_BODY, INGEST_TESTS),
                req("C2 — USER_EXISTS + nouveau test", "POST", "{{spring}}/coding-challenges/ingest-batch",
                    """[
  {
    "user": {
      "id": {{external_user_id}},
      "nom": "Dupont",
      "prenom": "Alice",
      "userName": "alice.dupont.postman",
      "email": "{{candidate_email}}",
      "status": true
    },
    "test": {
      "id": {{external_test_id_2}},
      "titre": "Valid Parentheses Postman",
      "description": "Check balanced brackets.",
      "tag": "stacks",
      "duree": 30,
      "codeUrl": "https://example.com/parentheses",
      "parameter": false
    }
  }
]""", INGEST_TESTS),
                req("C3 — BOTH_EXIST (idempotent, même event C1)", "POST", "{{spring}}/coding-challenges/ingest-batch",
                    INGEST_BODY, """
pm.test('Idempotent ingest', () => pm.response.to.have.status(200));
const item = pm.response.json().items[0];
pm.expect(item.entityCase).to.eql('BOTH_EXIST');
pm.expect(item.notificationAlreadyExisted).to.eql(true);
"""),
                req("C4 — demo.user (compte complet) + nouveau challenge", "POST", "{{spring}}/coding-challenges/ingest-batch",
                    """[
  {
    "user": {
      "id": 90002,
      "nom": "User",
      "prenom": "Demo",
      "userName": "demo.user",
      "email": "demo.user@codepulse.local",
      "status": true
    },
    "test": {
      "id": 880099,
      "titre": "Merge Intervals Demo",
      "description": "Merge overlapping intervals for demo user.",
      "tag": "intervals",
      "duree": 40,
      "codeUrl": "https://example.com/merge",
      "parameter": false
    }
  }
]""", """
pm.test('C4 ingest', () => pm.response.to.have.status(200));
const item = pm.response.json().items[0];
pm.expect(item.entityCase).to.eql('USER_EXISTS_CHALLENGE_NEW');
pm.environment.set('challenge_id', String(item.challengeId));
"""),
                req("Ingest — sans token (403)", "POST", "{{spring}}/coding-challenges/ingest-batch", "[]",
                    "pm.test('403', () => pm.response.to.have.status(403));", auth=False),
                req("Ingest — event invalide (test manquant)", "POST", "{{spring}}/coding-challenges/ingest-batch",
                    '[{"user":{"id":1,"email":"x@y.com","nom":"X","prenom":"Y","userName":"x","status":true}}]',
                    "pm.test('Validation fail', () => pm.expect(pm.response.json().failed).to.be.at.least(1));"),
                req("GET challenge by id", "GET", "{{spring}}/coding-challenges/get-coding-challenge/{{challenge_id}}"),
                req("GET challenges page (search Postman)", "GET",
                    "{{spring}}/coding-challenges/get-coding-challenges-pages/page?page=0&size=10&q=Postman"),
                req("GET tags", "GET", "{{spring}}/coding-challenges/tags"),
                req("GET count challenges", "GET", "{{spring}}/coding-challenges/count-coding-challenges"),
            ],
            "Utiliser des external_user_id / external_test_id uniques à chaque démo fraîche (voir guide).",
        ),
        folder(
            "03 — Notifications (cœur démo)",
            [
                req("POST envoyer notification (récupère urlAction + setup_token)", "POST", "{{spring}}/notifications",
                    '{"utilisateurId": {{user_id}}, "codingChallengeId": {{challenge_id}}}', NOTIF_SEND_TESTS),
                req("POST notification — idempotent (déjà existante, refresh urlAction)", "POST", "{{spring}}/notifications",
                    '{"utilisateurId": {{user_id}}, "codingChallengeId": {{challenge_id}}}', NOTIF_SEND_TESTS + "\npm.expect(pm.response.json().dejaExistante).to.eql(true);"),
                req("GET notification by id", "GET", "{{spring}}/notifications/get-notification/{{notification_id}}"),
                req("GET notifications by utilisateur", "GET",
                    "{{spring}}/notifications/get-notification-by-utilisateur?utilisateurId={{user_id}}"),
                req("GET notifications page (user inbox)", "GET",
                    "{{spring}}/notifications/get-notifications-by-utilisateur-pages/page?utilisateurId={{user_id}}&page=0&size=10"),
                req("GET all notifications (admin)", "GET", "{{spring}}/notifications/get-all-notifications"),
                req("GET notifications page + filtre ENVOYEE", "GET",
                    "{{spring}}/notifications/get-notifications-pages/page?page=0&size=10&statut=ENVOYEE"),
                req("GET count notifications", "GET", "{{spring}}/notifications/count"),
                req("GET count by statut ENVOYEE", "GET", "{{spring}}/notifications/count/statut?statut=ENVOYEE"),
                req("PATCH statut → LUE", "PATCH",
                    "{{spring}}/notifications/update-statut/{{notification_id}}/statut?statut=LUE",
                    tests="pm.test('Statut LUE', () => pm.response.to.have.status(200));"),
                req("Dev mailbox JSON (standalone GreenMail)", "GET", "{{spring}}/dev/mailbox/json",
                    MAILBOX_TOKEN_TESTS, auth=False),
                req("Dev relance — run now (standalone)", "GET", "{{spring}}/dev/relance/run",
                    tests="""
pm.test('Relance endpoint', () => pm.response.to.have.status(200));
const j = pm.response.json();
console.log('Relances sent:', j.sent, j.message);
""", auth=False),
                req("Logs — type RELANCE", "GET",
                    "{{spring}}/integration-logs/get-integration-logs-by-type?type=RELANCE"),
                req("Logs — type ENVOI_NOTIFICATION", "GET",
                    "{{spring}}/integration-logs/get-integration-logs-by-type?type=ENVOI_NOTIFICATION"),
            ],
            "Pour Gmail : lire l'e-mail dans votre boîte au lieu de /dev/mailbox. urlAction aussi dans POST /notifications.",
        ),
        folder(
            "04 — Complete account (nouveau candidat C1)",
            [
                req("GET setup-info (needs setup_token from POST /notifications)", "GET", "{{spring}}/auth/setup-info?token={{setup_token}}",
                    tests=SETUP_INFO_TESTS, auth=False, prerequest=SETUP_INFO_PREREQUEST),
                req("POST complete-account", "POST", "{{spring}}/auth/complete-account",
                    """{
  "token": "{{setup_token}}",
  "password": "{{candidate_password}}",
  "nom": "{{candidate_nom}}",
  "prenom": "{{candidate_prenom}}",
  "userName": "{{candidate_username}}"
}""",
                    COMPLETE_ACCOUNT_TESTS, auth=False),
            ],
        ),
        folder(
            "05 — Feedback (USER — token must be candidat, not admin)",
            [
                req("GET feedback form (Bearer = token_user after complete-account)", "GET", "{{spring}}/feedbacks/form?challengeId={{challenge_id}}",
                    tests=FEEDBACK_FORM_TESTS + "\npm.environment.set('token', pm.environment.get('token_user'));"),
                req("POST submit feedback SOUMIS", "POST", "{{spring}}/feedbacks/submit", SUBMIT_FEEDBACK_BODY, """
pm.test('Feedback submitted', () => pm.response.to.have.status(200));
pm.environment.set('feedback_id', String(pm.response.json().id));
""", prerequest=SUBMIT_FEEDBACK_PREREQUEST),
                req("GET feedback details", "GET", "{{spring}}/feedbacks/details/{{feedback_id}}"),
                req("GET feedback pages (admin)", "GET",
                    "{{spring}}/feedbacks/get-feedback-pages/page?page=0&size=10&tag=arrays"),
                req("GET average note (admin)", "GET", "{{spring}}/feedbacks/get-average-note"),
                req("GET count feedbacks", "GET", "{{spring}}/feedbacks/count-all-feedbacks"),
                req("Relance après SOUMIS — doit être 0", "GET", "{{spring}}/dev/relance/run",
                    "pm.test('No relance', () => pm.expect(pm.response.json().sent).to.eql(0));", auth=False),
            ],
        ),
        folder(
            "06 — Password reset",
            [
                req("POST forgot-password (demo.user)", "POST", "{{spring}}/auth/forgot-password",
                    '{"email":"demo.user@codepulse.local"}',
                    "pm.test('Forgot OK', () => pm.response.to.have.status(200));", auth=False),
                req("GET demandes EN_ATTENTE (app admin)", "GET",
                    "{{spring}}/demandes-reinit/page?page=0&size=10&statut=EN_ATTENTE", """
pm.test('Demandes', () => pm.response.to.have.status(200));
const page = pm.response.json();
if (page.content && page.content.length) {
  pm.environment.set('demande_id', String(page.content[0].id));
}
"""),
                req("POST send-link", "POST", "{{spring}}/demandes-reinit/{{demande_id}}/send-link"),
                req("Dev mailbox — extract reset_token", "GET", "{{spring}}/dev/mailbox/json", MAILBOX_TOKEN_TESTS, auth=False),
                req("GET reset-info", "GET", "{{spring}}/auth/reset-info?token={{reset_token}}", auth=False),
                req("POST reset-password", "POST", "{{spring}}/auth/reset-password",
                    '{"token":"{{reset_token}}","password":"ResetPass1234!"}', auth=False),
            ],
        ),
        folder(
            "07 — Users & Questions (APP ADMIN)",
            [
                req("POST add staff user (MANAGER_RH)", "POST", "{{spring}}/utilisateurs/add-user",
                    """{
  "nom": "Test",
  "prenom": "Staff",
  "email": "staff.test@codepulse.local",
  "rawPassword": "Staff1234!",
  "role": "MANAGER_RH"
}""", """
pm.test('Staff created', () => pm.response.to.have.status(200));
pm.environment.set('staff_user_id', String(pm.response.json().id));
"""),
                req("POST add-user role USER — must 400", "POST", "{{spring}}/utilisateurs/add-user",
                    '{"nom":"X","prenom":"Y","email":"bad@x.com","rawPassword":"Bad1234!","role":"USER"}',
                    "pm.test('400 ROLE_NON_AUTORISE', () => pm.response.to.have.status(400));"),
                req("GET all users", "GET", "{{spring}}/utilisateurs/get-all-users"),
                req("GET user by email", "GET",
                    "{{spring}}/utilisateurs/get-user-by-email?email={{candidate_email}}"),
                req("POST add question NOTE", "POST", "{{spring}}/questions-feedback/add-question",
                    '{"libelle":"Note globale ressentie","type":"NOTE","obligatoire":true}', """
pm.test('Question created', () => pm.response.to.have.status(200));
pm.environment.set('question_id', String(pm.response.json().id));
"""),
                req("GET all questions", "GET", "{{spring}}/questions-feedback/get-all-questions"),
                req("GET questions obligatoires", "GET",
                    "{{spring}}/questions-feedback/get-questions-by-obligatoire?obligatoire=true"),
            ],
        ),
        folder(
            "08 — Analytics & Logs",
            [
                req("Dashboard USER", "GET", "{{spring}}/analytics/dashboard/user"),
                req("Dashboard challenge admin", "GET", "{{spring}}/analytics/dashboard/challenge-admin"),
                req("Dashboard manager", "GET", "{{spring}}/analytics/dashboard/manager"),
                req("Dashboard app admin", "GET", "{{spring}}/analytics/dashboard/app-admin"),
                req("Average score by tag arrays", "GET", "{{spring}}/analytics/average-score-by-tag?tag=arrays"),
                req("Feedback participation", "GET", "{{spring}}/analytics/feedback-participation"),
                req("Top challenges", "GET", "{{spring}}/analytics/top-challenges?limit=5"),
                req("Export CSV", "GET",
                    "{{spring}}/analytics/export?startDate=2024-01-01T00:00:00Z&endDate=2030-01-01T00:00:00Z&format=csv"),
                req("Integration logs — all", "GET", "{{spring}}/integration-logs/get-all-integration-logs"),
                req("Integration logs — failed count", "GET", "{{spring}}/integration-logs/count/failed"),
            ],
        ),
        folder(
            "09 — Smart Search (8090)",
            [
                req("POST search libre", "POST", "{{search}}/search",
                    '{"query":"feedback stacks","top_k":10,"filters":{}}', """
pm.test('Search OK', () => pm.response.to.have.status(200));
pm.expect(pm.response.json().results).to.be.an('array');
"""),
                req("POST search CHALLENGE + tag arrays", "POST", "{{search}}/search",
                    '{"query":"two sum","top_k":8,"filters":{"source_type":"CHALLENGE","tag":"arrays"}}'),
                req("POST KPI — moyenne notes", "POST", "{{search}}/kpi",
                    '{"question":"Quelle est la moyenne des notes ?"}', """
pm.test('KPI', () => pm.response.to.have.status(200));
pm.expect(pm.response.json()).to.have.property('tool');
"""),
                req("POST KPI — participation", "POST", "{{search}}/kpi",
                    '{"question":"Quel est le taux de participation ?"}'),
                req("POST KPI — count challenges", "POST", "{{search}}/kpi",
                    '{"question":"Combien de challenges ?"}'),
                req("POST Assistant — Capgemini", "POST", "{{search}}/assistant",
                    '{"question":"Quels sont les principaux services de Capgemini ?"}', """
pm.test('Assistant', () => pm.response.to.have.status(200));
const j = pm.response.json();
pm.expect(j.answer).to.be.a('string');
console.log('Answer length:', j.answer.length, 'Citations:', (j.citations||[]).length);
"""),
                req("POST knowledge document", "POST", "{{search}}/knowledge/documents",
                    """{
  "title": "Capgemini — note Postman",
  "body": "Capgemini est un groupe de conseil et services numériques.",
  "category": "company",
  "tags": "capgemini,company,postman"
}""", """
pm.test('Knowledge created', () => pm.response.to.have.status(201));
pm.environment.set('knowledge_doc_id', String(pm.response.json().id));
"""),
                req("POST ingestion sync incremental", "POST", "{{search}}/ingestion/sync", '{"full": false}'),
                req("GET ingestion status", "GET", "{{search}}/ingestion/status"),
            ],
        ),
        folder(
            "10 — Role matrix (403 smoke)",
            [
                req("USER → ingest (403)", "POST", "{{spring}}/coding-challenges/ingest-batch", "[]",
                    "pm.test('403', () => pm.response.to.have.status(403));"),
                req("MANAGER → add-user (403)", "POST", "{{spring}}/utilisateurs/add-user",
                    '{"nom":"X","prenom":"Y","email":"x@y.com","rawPassword":"X12345678!","role":"MANAGER_RH"}',
                    "pm.test('403', () => pm.response.to.have.status(403));"),
                req("CHALLENGE ADMIN → search FEEDBACK filter (403 on search svc)", "POST", "{{search}}/search",
                    '{"query":"commentaires","top_k":5,"filters":{"source_type":"FEEDBACK"}}',
                    "pm.test('403 scope', () => pm.response.to.have.status(403));"),
            ],
            "Changer token via Login du bon rôle avant chaque requête, ou dupliquer avec auth fixe.",
        ),
        folder(
            "99 — E2E Showcase (ordre manager)",
            [
                req("1 — Login APP ADMIN", "POST", "{{spring}}/auth/login",
                    '{"email":"{{email_app_admin}}","password":"{{password_app_admin}}"}', SAVE_TOKEN_APP_ADMIN, auth=False),
                req("2 — Ingest C1 BOTH_NEW", "POST", "{{spring}}/coding-challenges/ingest-batch",
                    INGEST_BODY, INGEST_TESTS),
                req("3 — Send notification + urlAction", "POST", "{{spring}}/notifications",
                    '{"utilisateurId": {{user_id}}, "codingChallengeId": {{challenge_id}}}', NOTIF_SEND_TESTS),
                req("4 — Mailbox ou Gmail (voir guide)", "GET", "{{spring}}/dev/mailbox/json", MAILBOX_TOKEN_TESTS, auth=False),
                req("5 — setup-info", "GET", "{{spring}}/auth/setup-info?token={{setup_token}}",
                    tests=SETUP_INFO_TESTS, auth=False, prerequest=SETUP_INFO_PREREQUEST),
                req("6 — complete-account", "POST", "{{spring}}/auth/complete-account",
                    """{"token":"{{setup_token}}","password":"{{candidate_password}}","nom":"{{candidate_nom}}","prenom":"{{candidate_prenom}}","userName":"{{candidate_username}}"}""",
                    COMPLETE_ACCOUNT_TESTS, auth=False),
                req("7 — feedback form (Bearer token_user)", "GET", "{{spring}}/feedbacks/form?challengeId={{challenge_id}}",
                    FEEDBACK_FORM_TESTS, prerequest=FEEDBACK_FORM_PREREQUEST),
                req("8 — submit feedback (Bearer token_user)", "POST", "{{spring}}/feedbacks/submit", SUBMIT_FEEDBACK_BODY,
                    prerequest=SUBMIT_FEEDBACK_PREREQUEST),
                req("9 — relance blocked", "GET", "{{spring}}/dev/relance/run",
                    "pm.expect(pm.response.json().sent).to.eql(0);", auth=False),
                req("10 — Login APP ADMIN again (search needs admin, not USER)", "POST", "{{spring}}/auth/login",
                    '{"email":"{{email_app_admin}}","password":"{{password_app_admin}}"}', SAVE_TOKEN_APP_ADMIN, auth=False),
                req("11 — KPI participation", "POST", "{{search}}/kpi",
                    '{"question":"Quel est le taux de participation ?"}', prerequest=USE_ADMIN_TOKEN),
                req("12 — Assistant Capgemini", "POST", "{{search}}/assistant",
                    '{"question":"Quels sont les principaux services de Capgemini ?"}', prerequest=USE_ADMIN_TOKEN),
            ],
            "Exécuter ce dossier en Collection Runner pour la démo live devant le manager.",
        ),
    ],
}

OUT.write_text(json.dumps(collection, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print(f"Wrote {OUT}")
