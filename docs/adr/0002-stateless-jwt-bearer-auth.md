# ADR 0002: Stateless JWT (Bearer header) authentication

**Status:** accepted

## Context
The React frontend and the Spring Boot API will be deployed on different free
hosts, so they live on different origins. The project must cost $0 to run, and
reviewers should be able to try it without creating an account.

## Decision
- **Spring Security as an OAuth2 resource server** validating HS256-signed JWTs
  (the Nimbus library bundled with Spring, no extra third-party JWT library).
- Clients send `Authorization: Bearer <token>`. Tokens last 24 hours, carry the
  user id as `sub`, and the server holds no session state.
- **Passwords are hashed with BCrypt.** Login failures return the same message
  for "unknown email" and "wrong password", and an unknown email still performs
  a hash comparison so response times do not reveal which emails exist.
- **Demo login:** `POST /api/v1/auth/demo` signs in as one shared account
  (`demo@parkhfx.example`) that has a random, unknown password. It can be turned
  off with `DEMO_ENABLED=false`.
- The signing secret comes from the `JWT_SECRET` environment variable. The app
  refuses to start if it is shorter than 32 bytes.

## Alternatives considered
- **httpOnly cookie:** safer against token theft through XSS, but with the
  frontend and API on different free hosts it needs cross-site cookie settings
  and CSRF protection. Worth revisiting if both are served from one domain.
- **Auth0 / Keycloak / Supabase Auth:** more to learn and host, and less to
  show in a Java portfolio project.

## Consequences
- CSRF protection is disabled because no cookie carries the credential.
- A stolen token works until it expires. Refresh tokens, rate limiting and
  token revocation are deliberate non-goals for this portfolio project.
