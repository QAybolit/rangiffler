# CLAUDE.md (rfr-gql-client)

Guidance for working in this module. The root `CLAUDE.md` applies as well (its Java-specific rules concern the Java modules only); details are in `README.md` next to this file.

## Purpose

React single-page frontend of Rangiffler: travel map, photo feed, people and friendship, profile. It talks to `rfr-api` over GraphQL and signs in at `rfr-auth` with OAuth 2.1 Authorization Code + PKCE. It is an npm project and is not part of the Gradle build. Port 3001. Stack: React 19, TypeScript 6, Vite 8 (Node.js `^20.19.0` or `>=22.12.0`), Apollo Client 4, MUI 9, React Router 8.

## Commands

`rfr-auth` (9001) and `rfr-api` (8081) must be running.
```bash
cd rfr-gql-client
npm ci               # or npm install
npm run dev          # http://localhost:3001
npm run build        # tsc + vite build into dist/
npm run lint         # eslint, --max-warnings 0
npm run preview      # serve dist/
```

## Configuration

`.env` holds `VITE_AUTH_URL=http://localhost:9001`, `VITE_API_URL=http://localhost:8081`, `VITE_FRONT_HOST=localhost`, `VITE_FRONT_URL=http://localhost:3001`, `VITE_CLIENT_ID=client`. The dev server port 3001 is fixed in `vite.config.ts` and must match the redirect URI of the OAuth client in `rfr-auth`.

## Routes

`/` landing (Login, Register), `/redirect` (builds PKCE and goes to `rfr-auth`), `/authorized` (OAuth callback), `/logout` (clears localStorage), and the protected `/my-travels`, `/people`, `/profile`. Protected routes are wrapped by `PrivateRoute`, which runs `GetUser` (this also creates the API profile on the first sign-in); `UNAUTHORIZED` or `FORBIDDEN` sends the user to `/`, other errors show `QueryErrorAlert` with Retry.

## Authentication

- Login builds `code_verifier` / `code_challenge` (S256), stores them in `localStorage` and opens `/oauth2/authorize` with `scope=openid`; `/authorized` exchanges the `code` for tokens.
- Apollo sends **only the access token** as `Authorization: Bearer ...`. The `id_token` is kept only for `id_token_hint` on logout.
- HTTP 401 from `rfr-api` makes `ErrorLink` clear the session and go to `/`. There is no refresh token, so after the token expires the user signs in again (without a password while the `rfr-auth` session lives).
- Logout: `POST /oauth2/revoke`, clear the Apollo store, then `/connect/logout?id_token_hint=...`, then `/logout` clears `localStorage`.
- `localStorage` keys: `access_token`, `id_token`, `codeVerifier`, `codeChallenge`, `mui-mode` (also sent to `rfr-auth` as `theme`), `rangiffler-map-collapsed`.

## GraphQL operations

Each operation lives in its own hook in `src/hooks`, and the operation names are stable, so tests can match requests by `operationName`: `GetUser`, `GetCountries`, `GetFeed`, `GetPeople`, `GetFriends`, `GetInvitations`, `GetOutcomeInvitations`, `UpdateUser`, `FriendshipAction`, `CreatePhoto`, `UpdatePhoto`, `LikePhoto`, `DeletePhoto`. Errors with `extensions.classification` `BAD_REQUEST` or `NOT_FOUND` show the server text to the user; other errors show a generic message.

## Rules

- **Keep the mocks.** The photo, like, feed and statistics mocks (served by `rfr-api`: `FeedMockQueryController`, `PhotoMockMutationController`, `resources/mock/*.json`) stay in the project until I explicitly ask to replace them with real implementations. Do not replace, remove or work around them on your own, and do not change the frontend to bypass them.

## Things that are easy to get wrong

- **A success message does not prove persistence.** Apollo normalizes entities by `id` and the mutation response updates the card immediately, and the photo, like and feed backend is a mock. To check that something was saved, read it back independently (for example reload the page).
- The feed, photos and likes return JSON content after a re-read because `rfr-api` mocks them.

## Structure

`src/api/` (Apollo client, auth client, PKCE and token helpers, error text), `src/components/` (UI parts such as `ProfileForm`, `PeopleTable`, `PhotoCard`, `WorldMap`), `src/context/` (session, countries, dialog, snackbar), `src/hooks/` (one hook per GraphQL operation), `src/pages/`, `src/types/`, `src/theme.tsx` (MUI light and dark themes).
