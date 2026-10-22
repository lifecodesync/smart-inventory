# Glossary (rewrite each in your own words)

| Term | Plain English | Mobile-shop example |
|---|---|---|
| Client | The program that asks (the browser) | Customer asking at the counter |
| Server | The program that answers (our Spring Boot app) | Stock room behind the counter |
| HTTP | The fixed format client and server use to talk: request → response | The bill format every counter uses |
| Request / Response | One question / one answer. Request has a method (GET = read, POST = change) and a URL | "1 charger please" / the charger or "out of stock" |
| Status code | 3-digit result: 200 OK, 302 redirect, 400 bad input, 403 forbidden, 404 not found, 409 conflict, 500 server bug | "Here you go" / "Staff only area" / "No such model" / "Sold out" |
| DNS | Turns a name (google.com) into an IP address | Phone contacts: name → number |
| Database | Organised storage that many users can safely read and write at once, with rules | The shop register that never forgets |
| Table / row / column | Table = one kind of thing; row = one item; column = one property | Product list; one line = Fast Charger 20W; the "price" column |
| Primary key | A column that uniquely identifies a row (id) | Barcode / SKU number |
| Foreign key | A column pointing to another table's primary key | Bill line says "product #3" |
| CRUD | Create, Read, Update, Delete | Add a model, view catalogue, change price, discontinue a model |
| Validation | Checking input before using it; never trust the user | Refuse a bill for "-2 power banks" |
| Transaction | A group of DB changes that all happen or none happen | A bill: stock down AND sale written, or neither |
| Race condition | Two actions at the same time interfere and give a wrong result | Two staff both sell the last pair of earbuds |
| Row lock | DB makes the second writer wait until the first finishes | One person at the register at a time |
| CHECK constraint | A rule inside the database itself, e.g. stock ≥ 0 | A stock register that physically can't show negative |
| Index | A lookup structure that finds rows without scanning everything | Book index |
| JOIN | Combine rows from two tables by a matching column | Match bills with the catalogue to get product names |
| Spring Boot | Java framework that gives us a web server, DB access and security pre-wired | A ready-made shop fit-out: shelves, counter and till already installed |
| Dependency injection | Spring creates objects and hands them to the classes that need them | Manager assigns a salesperson to each counter |
| Maven | Build tool: downloads libraries (pom.xml), compiles, tests, packages a .jar | Supplier list + assembly instructions |
| JPA / Hibernate | Maps Java classes to tables | Translator between Java and SQL |
| Flyway | Runs versioned SQL scripts (V1, V2…) once, in order | Dated renovation plans for the shop |
| Thymeleaf | HTML templates the server fills with data | A price-list template with today's prices filled in |
| Hashing | One-way scrambling; can't be reversed | Fingerprint of a password |
| BCrypt | Slow, salted password hashing, designed for passwords | A fingerprint that takes deliberate effort to make |
| Session / cookie | Server remembers you're logged in; browser keeps a cookie ID | Staff ID badge after signing in |
| Role | A named set of permissions (MANAGER, STAFF) | Staff can bill; only the manager can order stock |
| CSRF | Attack where another site submits a form as you; blocked with a secret token in our forms | A stranger slipping a fake bill into the till |
| Docker image / container | Image = packaged app + runtime (recipe); container = running copy (the dish) | Sealed product box vs the device switched on |
| Cloud | Renting someone else's computers over the internet, paying for what you use | Renting a ready shop in a mall instead of building one |
| IaaS / PaaS / SaaS | Rent machines / rent a platform that runs your app / rent finished software | Empty shell / furnished shop with security & electricity / a franchise you just use |
| Environment variable | A setting given to the app from outside the code (e.g. DB password) | The safe code told privately, not written on the price board |
| Health check | URL the cloud pings to know the app is alive (`/actuator/health`) | Manager asking "counter OK?" every few minutes |
| Load balancer | Spreads requests across several app copies; also terminates HTTPS | Floor manager sending customers to free counters |
| CI | Automatic build + tests on every push (GitHub Actions) | Quality check before a device goes on the shelf |
| Moving average | Forecast = average of the last N days | "We sold ~9 earphones/day this week" |
| Exponential smoothing | Weighted average where recent days count more (α) | Trusting yesterday more than last month |
| MAE | Mean Absolute Error: average size of forecast mistakes, in units | "We're usually off by 2 chargers a day" |
| Safety stock | Extra buffer for bad-luck days | Keeping extra chargers for the festival rush |
