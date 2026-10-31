---
name: rfr-test-data
description: Rules for creating test data in rfr-e2e. Use when adding or changing @User preconditions, UserFixture decorators, repositories in data/db, SQL files or any code that creates users, profiles and relations for a test.
---

# Create test data

Test data is created by the test itself, owned by the fixture tree, and removed with it.

## Rules

1. **Unique data.** Every username and password comes from `RandomDataUtils` per test. No literals, no shared constants, no data reused between tests or runs, so tests stay safe in parallel.
2. **The owner is the decorator tree.** `UserRequest.fixture()` builds it, the Store holds its root, `UserExtension.afterEach` closes it. Each decorator owns what it created. Test code, statics and `ThreadLocal` own nothing.
3. **A new precondition is a new decorator.** Add a `UserWith...Fixture` composed in `UserRequest.fixture()` and an `@User` attribute. No flag in an existing fixture, no branch in `UserExtension`. Validate the attribute (for example a negative count) in `UserRequest` before anything is created.
4. **SQL only in `data/db`.** Each statement is a named file in `src/test/resources/sql/{api,auth}`, called through `ApiUserRepository` or `AuthUserRepository`. Use `JdbcClient` with named parameters. Check the row count of every insert with `Jdbc.requireOneRow`. No SQL in `jupiter`, `ui` or `test`.
5. **The API profile is created explicitly.** `rfr-api` creates profiles lazily on the first login, and a test must not depend on that. The fixture calls `ApiUserRepository.createProfile` right after the auth account, and `close()` deletes both by username.
6. **Delete exactly what was created.** By username, partners first, then the origin. No `TRUNCATE`, no broad `DELETE`.

## Answer format

- For each new piece of data: who creates it, who removes it, where its SQL lives.
- Name the rule for each violation, with file and line.
