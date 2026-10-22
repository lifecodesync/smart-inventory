# Glossary (rewrite each in your own words)

| Term | Plain English | Canteen example |
|---|---|---|
| Client | The program that asks (the browser) | Customer placing an order |
| Server | The program that answers (our Spring Boot app) | Kitchen |
| HTTP | The fixed format client and server use to talk: request → response | The order slip format |
| Request / Response | One question / one answer. Request has a method (GET = read, POST = change) and a URL | "1 samosa" / the samosa or "sold out" |
| Status code | 3-digit result: 200 OK, 302 redirect, 400 bad input, 403 forbidden, 404 not found, 409 conflict, 500 server bug | "Here you go" / "Not allowed in kitchen" / "No such dish" / "Sold out" |
| DNS | Turns a name (google.com) into an IP address | Phone contacts: name → number |
| Database | Organised storage that many users can safely read and write at once, with rules | The canteen register that never forgets |
| Table / row / column | Table = one kind of thing; row = one item; column = one property | Product list; one line = Samosa; the "price" column |
| Primary key | A column that uniquely identifies a row (id) | Token number |
| Foreign key | A column pointing to another table's primary key | Sale slip says "product #3" |
| CRUD | Create, Read, Update, Delete | Add dish, view menu, change price, remove dish |
| Validation | Checking input before using it; never trust the user | Refuse an order for "-2 samosas" |
| Transaction | A group of DB changes that all happen or none happen | A bill: stock down AND sale written, or neither |
| Race condition | Two actions at the same time interfere and give a wrong result | Two staff both sell the last samosa |
| Row lock | DB makes the second writer wait until the first finishes | One person at the register at a time |
| CHECK constraint | A rule inside the database itself, e.g. stock ≥ 0 | Register page that physically can't show negative |
| Index | A lookup structure that finds rows without scanning everything | Book index |
| JOIN | Combine rows from two tables by a matching column | Match sale slips with the menu to get names |
| Spring Boot | Java framework that gives us a web server, DB access and security pre-wired | A ready-made kitchen with appliances installed |
| Dependency injection | Spring creates objects and hands them to the classes that need them | Manager assigns a cook to each counter |
| Maven | Build tool: downloads libraries (pom.xml), compiles, tests, packages a .jar | Supplier list + recipe card |
| JPA / Hibernate | Maps Java classes to tables | Translator between Java and SQL |
| Flyway | Runs versioned SQL scripts (V1, V2…) once, in order | Dated renovation plans for the kitchen |
| Thymeleaf | HTML templates the server fills with data | A printed menu with blanks filled daily |
| Hashing | One-way scrambling; can't be reversed | Fingerprint of a password |
| BCrypt | Slow, salted password hashing, designed for passwords | A fingerprint that takes deliberate effort to make |
| Session / cookie | Server remembers you're logged in; browser keeps a cookie ID | Wristband after paying entry |
| Role | A named set of permissions (MANAGER, STAFF) | Staff can bill; only manager can order stock |
| CSRF | Attack where another site submits a form as you; blocked with a secret token in our forms | A stranger slipping a fake order slip |
| Docker image / container | Image = packaged app + runtime (recipe); container = running copy (the dish) | Recipe vs plate |
| Cloud | Renting someone else's computers over the internet, paying for what you use | Hiring a caterer instead of building a kitchen |
| IaaS / PaaS / SaaS | Rent machines / rent a platform that runs your app / rent finished software | Empty kitchen / kitchen + staff / ordering food |
| Environment variable | A setting given to the app from outside the code (e.g. DB password) | Today's password told verbally, not written on the menu |
| Health check | URL the cloud pings to know the app is alive (`/actuator/health`) | Manager asking "kitchen OK?" |
| Load balancer | Spreads requests across several app copies; also terminates HTTPS | Queue manager sending customers to free counters |
| CI | Automatic build + tests on every push (GitHub Actions) | Food taster before serving |
| Moving average | Forecast = average of the last N days | "We sold ~60/day this week" |
| Exponential smoothing | Weighted average where recent days count more (α) | Trusting yesterday more than last Monday |
| MAE | Mean Absolute Error: average size of forecast mistakes, in units | "We're usually off by 6 samosas" |
| Safety stock | Extra buffer for bad-luck days | Keeping 10 extra samosas for exam week |
