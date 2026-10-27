# CLAUDE.md (rfr-api)

Guidance for working in this module. The root `CLAUDE.md` applies as well; details are in `README.md` next to this file.

## Purpose

GraphQL backend of Rangiffler and an OAuth2 Resource Server. Users, countries and friendships are real (MySQL); photos, likes, the feed and statistics are a **mock** layer reading JSON files and persist nothing. Port 8081, endpoint `POST /graphql`, GraphiQL at `/graphiql`, MySQL schema `rangiffler-api` (created automatically).

## Run

MySQL (`bash localenv.sh` from the repo root) and `rfr-auth` (needed for the JWKS) must be running:
```bash
./gradlew :rfr-api:bootRun
./gradlew :rfr-api:generateJava     # regenerate the GraphQL model classes (also part of the build)
```
Check it with a token from the frontend's `localStorage` key `access_token`:
```bash
curl -X POST http://localhost:8081/graphql -H "Content-Type: application/json" -H "Authorization: Bearer <access_token>" -d '{"query":"{ user { id username location { code } } }"}'
```

## Real or mock

| Feature | Implementation |
|---|---|
| Profile (`Query.user`, `Mutation.user`) | Database; the profile is created on the first `Query.user` with country `ru` |
| Countries (`Query.countries`) | Database, 238 countries loaded by the migration |
| People search and paging (`Query.users`) | Database, sorted by username |
| Friendship (`Mutation.friendship`, friends, invitations) | Database, one row per pair |
| Feed and statistics (`Query.feed`) | Mock: `resources/mock/query_feed*.json`, the same for every user; `withFriends` selects the file |
| Create, edit, like, delete photo | Mock: returns a valid object (delete always `true`) and stores nothing |

Tables `photo`, `like`, `photo_like`, `statistic` exist in the migration but are unused; implementing them is the open work.

## Rules

- **Keep the mocks.** The photo, like, feed and statistics mocks (`FeedMockQueryController`, `PhotoMockMutationController`, `service/mock/FeedMockData`, `resources/mock/*.json`) stay in the project until I explicitly ask to replace them with real implementations. Do not replace, remove or work around them on your own.
- **The schema is the contract.** `src/main/resources/graphql/query.graphqls` was written for the finished frontend; DGS Codegen generates the Java types (`io.student.rangiffler.model.*`) into `build/generated`, so never hand-write them. `UserConnection` and `PhotoConnection` are intentionally undefined and map to `Page<...>`; the `Date` scalar maps to `LocalDate`.
- **Layering:** controller, then `service/api` interface, then `service/impl`, then repository.
- **Security:** query controllers use `@PreAuthorize("hasAuthority('read')")`, mutation controllers `hasAuthority('write')`. Only an access token is accepted (JWT checked against the JWKS of `rfr-auth`: signature, `iss`, `exp`, audience `rangiffler-api`); an `id_token` gets 401. The username of the caller is the `sub` claim.
- **Errors:** a GraphQL response is HTTP 200 even for errors, so check `errors`. `GraphQlExceptionResolver` maps `ResourceNotFoundException` to `NOT_FOUND`, `FriendshipActionException` and `IllegalArgumentException` to `BAD_REQUEST`; missing or wrong authority gives `UNAUTHORIZED` / `FORBIDDEN`; anything else is `INTERNAL_ERROR` without details. A bad or expired Bearer token is HTTP 401 from the filter, before GraphQL.
- **Profile update:** a `null` or omitted field in `UserInput` means "do not change".
- **Friendship actions:** `ADD` (any user, no existing row, creates `PENDING`), `ACCEPT` and `REJECT` (only the addressee), `DELETE` (either side, any status; removes a friend or cancels an outgoing invitation). A user cannot act on themselves.
- **Database:** `ddl-auto: none`, schema changes go only through Flyway migrations in `db/migration/rangiffler-api/`. User UUIDs in `rangiffler-api` and `rangiffler-auth` are independent; they are linked only by username.
