# Deploy to Render (free, no credit card)

**Why Render:** it's a PaaS. We give it a Dockerfile and it runs the container, provides HTTPS, health checks, logs, auto-deploy from GitHub and a managed PostgreSQL database. On IaaS (e.g. AWS EC2) we would have to set up the server, Java, the firewall, TLS certificates and database backups ourselves.

## 1. Put the code on GitHub
```powershell
cd D:\projects\smart-inventory
git init -b main
git add .
git commit -m "Smart Inventory: first working version"
```
On github.com, create a **new empty repository** named `smart-inventory` (no README). Then:
```powershell
git remote add origin https://github.com/lifecodesync/smart-inventory.git
git push -u origin main
```
Open the repo's **Actions** tab. The CI run should go green. Red means a test failed: open it and read the log.

## 2. Create everything on Render with the Blueprint
1. Sign up at render.com with **GitHub** (no card needed).
2. **New + → Blueprint**, pick the `smart-inventory` repo. Render reads `render.yaml`.
3. It asks for `MANAGER_PASSWORD` and `STAFF_PASSWORD`. Type strong passwords. These are secrets, so they never go in Git.
4. Click **Apply**. Render creates:
   - `inventory-db`: managed PostgreSQL (free, 1 GB, **expires after 30 days**)
   - `smart-inventory`: web service built from the `Dockerfile`
5. The first build takes about 5–8 minutes (it downloads Maven libraries). Watch **Logs**. Success looks like `Started InventoryApplication` and `Demo data created`.

## 3. Check it works
- Open `https://smart-inventory-xxxx.onrender.com/actuator/health`. It should show `{"status":"UP"}`.
- Log in as manager, record a sale, and check that the dashboard updates.
- Log in as staff and try `/dashboard`. You should get a 403 page.

## Common cloud problems
| Symptom | Likely cause | Fix |
|---|---|---|
| Build fails at `mvn package` | compile error | read the build log; run `mvn package` locally |
| `Connection refused` to DB | DB env vars missing | check Environment tab: DB_HOST etc. come from `inventory-db` |
| Health check failing / "No open ports" | app not listening on `$PORT` | `server.port: ${PORT:8080}` in application.yml |
| Out of memory, restarts | free tier = 512 MB | `-XX:MaxRAMPercentage=75` is set in Dockerfile |
| First page takes ~1 min | free service sleeps after 15 min idle | normal on free tier; open it before your viva |
