# ParkHFX

A peer-to-peer parking marketplace for Halifax, built as a portfolio project.
Hosts list spare parking spots, drivers search a map and book by the hour.
It runs in demo mode only: no real payments, and all data is sample data.

## Stack
Java 25 + Spring Boot, PostgreSQL + PostGIS (Flyway migrations),
React + TypeScript, OpenStreetMap + Leaflet, Docker, GitHub Actions,
Testcontainers.

## What exists so far (slices 1-3)
- Database schema with PostGIS geo columns and an **exclusion constraint that
  makes double-booking impossible** (see `docs/adr/0001-...`).
- `BookingService`: 10-minute holds, confirm, cancel, expiry of abandoned holds.
- Tests on a real PostGIS database, including a **20-thread concurrency test**
  that proves exactly one booking wins.
- **Auth:** register, login, one-click demo login, `/me`. Spring Security with
  stateless JWT bearer tokens and BCrypt (see `docs/adr/0002-...`).
- **Listings:** public map-bounds and radius search, listing detail, photos,
  weekly availability windows, and 70 seeded Halifax-area listings.
- CI that builds and runs the tests on every push.
- React/TypeScript frontend shell with Halifax listing cards and
  OpenStreetMap/Leaflet integration. The map uses public tiles with required
  OpenStreetMap attribution and needs no API key.

## API so far
| Method | Path | Auth | What it does |
|---|---|---|---|
| POST | `/api/v1/auth/register` | none | Create an account, returns `{token, user}` |
| POST | `/api/v1/auth/login` | none | Sign in, returns `{token, user}` |
| POST | `/api/v1/auth/demo` | none | One-click sign in as the shared demo user |
| GET | `/api/v1/me` | Bearer token | The signed-in user |
| GET | `/api/v1/listings` | none | Search listings by map bounds or radius |
| GET | `/api/v1/listings/{id}` | none | Listing detail, photos, host and availability |
| GET | `/actuator/health` | none | Health check |

Errors are JSON in the standard "problem details" shape
(for example `409` when a spot is already booked).

## Configuration (environment variables)
| Variable | Default | Notes |
|---|---|---|
| `JWT_SECRET` | dev-only value | **Set this in any deployed environment.** At least 32 characters. |
| `DEMO_ENABLED` | `true` | Turns the demo login on or off. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated frontend origins. |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | match `docker-compose.yml` | Database connection. |

## Run it ($0 tools)
You need: JDK 25 (Temurin is free), Maven (or IntelliJ's bundled one), and
Docker (Docker Desktop is free for personal use).

```bash
docker compose up -d          # start PostgreSQL + PostGIS
cd backend
mvn verify                    # run the tests (Docker must be running)
mvn spring-boot:run           # start the API
curl localhost:8080/actuator/health

# try the auth API
curl -s -X POST localhost:8080/api/v1/auth/demo
curl -s -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"a long password","displayName":"You"}'
# then: curl -s localhost:8080/api/v1/me -H "Authorization: Bearer <token>"
```

## Roadmap
- [x] 1. Schema + double-booking guard + tests + CI
- [x] 2. Auth (Spring Security + JWT) and demo login
- [x] 3. Listings API + map search + Halifax seed data
- [ ] 4. React frontend: search map, listing page
- [ ] 5. Pricing + booking flow + simulated payments (Stripe test mode later)
- [ ] 6. Host dashboard, reviews, analytics views
- [ ] 7. Deploy, README polish, demo video
