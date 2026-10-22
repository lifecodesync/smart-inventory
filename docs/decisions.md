# Decisions (what we chose, the alternatives, why)

| # | Decision | Alternatives | Why this one |
|---|---|---|---|
| 1 | **Java 21 + Spring Boot 3.5** | Node/Express, Python/Django | Java is my practical + internship language; Spring Boot gives security, DB, validation, health checks ready-made |
| 2 | **Server-rendered Thymeleaf** | React SPA + REST API, static site | One language, fewer moving parts, enough for an internal tool |
| 3 | **PostgreSQL** | MongoDB (NoSQL), MySQL, Excel | Data is relational (products ↔ movements); needs transactions, row locks, CHECK constraints; free managed Postgres on Render |
| 4 | **Ledger table `stock_movement`** + stock column | Only a stock column | Every change is traceable (audit), and the forecast needs sales history |
| 5 | **Flyway migrations** | `ddl-auto=update` | Repeatable, versioned, reviewable schema; Hibernate only validates |
| 6 | **Anti-oversell: atomic `UPDATE … WHERE stock >= qty`** + `CHECK (stock >= 0)` | `synchronized` (fails with 2+ app instances), `SELECT … FOR UPDATE` (works, 2 queries), `@Version` optimistic lock (needs retry logic) | One statement, the DB does the locking, works across many app instances, no retries; CHECK is a safety net |
| 7 | **BCrypt passwords, form login, sessions, CSRF on** | Plain text (never), SHA-256 (too fast to brute force), JWT (unneeded for server pages) | Industry standard for server-rendered apps |
| 8 | **Roles via URL rules in one SecurityConfig** | Annotations per method | All access rules visible in one place; easy to defend in viva |
| 9 | **Forecast: moving average vs exponential smoothing, choose lower MAE** | ARIMA, Prophet, ML | Explainable by hand, testable, enough data (56 days); MAE is in units, easy to explain |
| 10 | **Render (PaaS) + Render Postgres** | AWS EC2 + RDS (IaaS + DBaaS), Railway, Fly.io, Azure App Service | Free without a credit card, deploys from Dockerfile + GitHub, HTTPS + health checks + logs built in, Singapore region near India |
| 11 | **Docker multi-stage build** | Buildpacks, plain jar upload | Same image everywhere ("works on my laptop" = works in cloud); small runtime image; non-root user |
| 12 | **Secrets in environment variables** | In application.yml / Git | Anyone with the repo would get the passwords; env vars are set per environment |
| 13 | **GitHub Actions CI with a Postgres service** | Manual testing | Every push proves tests pass on a clean machine |
| 14 | **Times stored in UTC, days computed in Asia/Kolkata** | Server local time | Cloud servers run in UTC; "today" must mean today in India |
