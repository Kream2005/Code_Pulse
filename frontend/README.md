# CodePulse frontend

React 19 + Vite 8 + Tailwind 4 UI, proxied to the Spring Boot API.

Requires **Node.js 25+**.

```bash
npm install
npm start   # http://localhost:4200 → proxies API to :8080
```

## Roles (UI)

| Role | Sees |
|------|------|
| USER | Inbox (+ KPIs), give feedback, my feedbacks, profile |
| ADMIN_CODING_CHALLENGE | Admin dashboard KPIs, challenges, notifications, feedbacks |
| MANAGER_RH | Admin dashboard KPIs, feedbacks, analytics |
| ADMIN_CODEPULSE | Full admin (users, questions, logs, resets, analytics, …) |

See root [`README.md`](../README.md) and [`docs/APPLICATION-FUNCTIONALITIES.md`](../docs/APPLICATION-FUNCTIONALITIES.md).
