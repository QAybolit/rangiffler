# CLAUDE.md (rfr-auth)

Guidance for working in this module. The root `CLAUDE.md` applies as well; details are in `README.md` next to this file.

## Purpose

Registration and authentication service on Spring Authorization Server (OAuth 2.1 / OIDC): it stores accounts, serves the Thymeleaf login, register and error pages, and issues tokens to the frontend with Authorization Code + PKCE. `rfr-api` validates the access tokens against this service's JWKS. Port 9001, MySQL schema `rangiffler-auth`.

## Run

MySQL must be running (`bash localenv.sh` from the repo root), then:
```bash
./gradlew :rfr-auth:bootRun
curl http://localhost:9001/.well-known/openid-configuration    # health check
```

## Behavior to keep in mind

- **OAuth client:** one public client `client` (no secret), PKCE required, scope `openid`, redirect URI `http://localhost:3001/authorized`, post-logout redirect `http://localhost:3001/logout`. It is configured in `application.yaml` under `spring.security.oauth2.authorizationserver.client.oidc-client`.
- **Tokens:** only the access token goes to `rfr-api`; `AccessTokenCustomizer` adds `aud: ["rangiffler-api"]` and the user's `authorities` (`read`, `write`) to it. The `id_token` (`aud` = `client`) is used by the frontend only as `id_token_hint` on logout. No refresh token is issued.
- **Restart invalidates sessions:** the RSA signing key is generated on every start, and authorizations, consent and keys live in memory, so after a restart everyone must sign in again.
- **Registration** (`POST /register`, form data with the CSRF token of the same session): `username` 3-50 characters, `password` 3-12 characters, neither may contain whitespace, `passwordSubmit` must equal `password`. Success is HTTP 201 with the page "Congratulations! You've registered!"; a validation error or a taken username is HTTP 400 with the same form. The user gets the `read` and `write` authorities, the password is stored with `DelegatingPasswordEncoder` (`{bcrypt}`).
- **Registration creates only the auth account.** The profile in `rangiffler-api` is created later, on the first authorized `Query.user`.
- **Login entry point:** `GET /login` without a saved OAuth request redirects to the frontend, so a sign-in must start at `/oauth2/authorize` or at the Login button on the frontend.
- **Messages are localized** by `Accept-Language` (Spring Security texts such as "Bad credentials").
- **Theme:** `?theme=light|dark` is stored in the `rangiffler-theme` cookie by `ThemeCookieFilter`.
- **Token revocation** from the SPA works through the custom `PublicClientRevocation*` classes; `rfr-api` checks JWTs locally and does not know about revocations until `exp`.

## Structure

`config/` (security filter chains, token customizer, CORS, theme filter), `controller/` (login, register, error), `data/` (entities, repository), `service/` (registration, `UserDetailsService`, logout handler), `validation/` (`@EqualPasswords`, `@NoWhitespace`), `resources/templates/` (Thymeleaf pages), `resources/db/migration/rangiffler-auth/` (Flyway).

## Troubleshooting

- Tokens stopped working after a restart: sign in again (new signing key).
- `rfr-api` answers 401 to a token that looks valid: make sure the Bearer is the access token, not the `id_token`, and that it has not expired.
- CORS errors: the frontend must run at the configured base URI (`http://localhost:3001`); origins come from `CorsCustomizer`.
