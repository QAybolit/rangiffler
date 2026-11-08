# CLAUDE.md (rfr-e2e)

Guidance for writing and running the end-to-end tests in this module. The project-wide rules in the root `CLAUDE.md` (working agreement, indentation, Javadoc) apply here as well.

## Structure

One `src/test` source set, package `io.student.rangiffler`:

| Package | Contents |
|---|---|
| `config` | `TestConfig`, the only place that reads settings |
| `jupiter` | JUnit infrastructure: `annotation` (`@User` with `friends`, `incomeInvitations`, `outcomeInvitations`; `@StaticUser`), `extension` (`UserExtension` creates a `@User` user; `UsersQueueExtension` lends a `@StaticUser` user from the pool; `SuiteExtension` with `SuiteRun` is the base of extensions of the whole run and `MigrationCheckExtension` is one of them; `DemoExtension`, `DemoValue` and `@Demo` serve the demo tests), `probe` (`UserExtensionTest`, `UsersQueueExtensionTest` and `SuiteExtensionTest` run nested sample classes through EngineTestKit) |
| `data` | Test data and its owners: `TestUser`, `UserFixture` with `BaseUserFixture`, the decorator `UserWithRelationsFixture` with the `Relation` enum (shared partner logic in `Partners`) and `StaticUserFixture` (the pool of static users), `UserRequest`; `data/db` has `Databases`, `Jdbc`, `SchemaMigrations` and the repositories (`ApiUserRepository`, `AuthUserRepository`), `Friendship` and `FriendshipStatus`; SQL files are in `src/test/resources/sql/{api,auth}` |
| `jupiter/allure` | `EnvironmentFile`, the `environment.properties` of an Allure results directory |
| `jupiter/stand` | Waiting for the stand without JUnit: `StandService` with `TcpService` and `HttpService`, `StandReadiness`; `StandReadinessTest`, `TcpServiceTest` and `HttpServiceTest` check them on fake services |
| `utils` | Stateless static helpers, e.g. `RandomDataUtils` |
| `ui` | `page` (Page Objects) and `component` (shared page parts) |
| `test` | `BaseWebTest` and the tests, grouped by feature (`auth`, `profile`, `people`, ...) and `demo`, which shows every place for `@ExtendWith` |

Layering: `jupiter` has no SQL and no locators; `data` has no JUnit; tests have no locators and no SQL; no `ThreadLocal` for test state, it lives in the extension's `Store`.

## Running

Every UI test class carries its own `@Tag("ui")` (it is not inherited from `BaseWebTest`), so a new UI test class must add the tag itself, otherwise `e2e` skips it.
```bash
./gradlew :rfr-e2e:installPlaywright          # once: Chromium for Playwright
./gradlew :rfr-e2e:e2e                        # UI tests (tag ui) after waiting for the stand
./gradlew :rfr-e2e:test                       # tests without the ui tag; no stand needed
./gradlew :rfr-e2e:test --tests '<TestClass>' # one test; with --tests the ui tag is not excluded
```
The stand is awaited by `StandReadinessExtension` before the first class tagged `ui`, so both `e2e` and a UI test started from the IDE wait for it; there is no separate wait task.

`UserExtensionTest` (tag `extension`) needs MySQL but no browser. Its nested sample classes are enabled only when the configuration parameter `rfr.testkit` is `true`, which the test sets itself, so they never run on their own. `@ProbeTarget` also carries the tag `probe`, and both `test` and `e2e` exclude it, with or without `--tests`: sample classes need the tag `ui` for `StandReadinessExtension`, and without this exclusion they would show up as skipped tests in every run. The exclusion does not touch EngineTestKit, which runs the samples itself.

## Static users

A test that cannot create its user on the fly takes a `@StaticUser TestUser` parameter, and `UsersQueueExtension` lends it one of the pooled users `static-user-1`, `static-user-2` and `static-user-3` (password `static-secret`) from a JVM-wide queue and gives it back after the test. A test waits for a free user for `rfr.staticUserTimeout` seconds. No code creates these users, and static tests must only read their data, because the users are shared by every test and every run.

Create the pooled users once in a fresh database, in both schemas (`rangiffler-auth` and `rangiffler-api`); the script can be run again without harm:
```bash
docker exec -i rfr-mysql mysql -uroot -psecret < scripts/static-users.sql
```
The pull request workflow creates them with the same script once the backends have migrated both schemas, so a local run is the only one that needs the command above. If the users are missing, tests with `@StaticUser` fail at login with "Bad credentials"; the stand wait does not check for them. The names and the password are also literals in `StaticUserFixture`, so change them in both places together.

Use `@StaticUser` on a parameter of the test method only, once per test, and add `@ExtendWith(UsersQueueExtension.class)` to every test class that uses it.

## Suite extensions

A `SuiteExtension` acts once per test run: `beforeSuite(ExtensionContext)` before the first test class, `afterSuite(ExtensionContext)` after the last one, both with the root context. `SuiteRun` is stored in the root `Store` before `beforeSuite` is called. If `beforeSuite` fails, it is not repeated, every class fails with the same cause, `afterSuite` is not called, and the failure is thrown again when the run closes. Several suite extensions start in registration order and close in reverse order. `appliesTo(ExtensionContext)` (true by default) lets an extension start on the first class it applies to instead of the first class of the run. Register an implementation only through `ServiceLoader`: list it in `src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` (auto-detection is enabled in `junit-platform.properties`), never with `@ExtendWith`. It must be safe for any test of the project. `SuiteExtensionTest` (tag `extension`, no MySQL) checks these rules through EngineTestKit, treating the interface as a black box.

## Demo tests

`test/demo` holds `DemoClassLevelTest` and `DemoExtendWithTest`. They show where `@ExtendWith` can be put (class, nested class, methods, parameters, field, composed annotation `@Demo`, superclass, interface) with `DemoExtension`, which implements all callbacks and a parameter resolver and prints the call order and the extension instance, so it is visible whether tests share it.

## Allure environment

`AllureEnvironmentExtension` is a `SuiteExtension` that applies only to classes tagged `ui` (`SuiteExtension.appliesTo`), so a run without UI tests, such as `:rfr-e2e:test`, writes no file and never opens a connection to MySQL. It writes `environment.properties` into the Allure results directory in `afterSuite`: `frontUrl`, `authUrl`, `apiUrl`, `dbUrl`, `browser`, `headless`, `commit` and the current Flyway version of each schema (`authSchemaVersion`, `apiSchemaVersion`). The directory is the system property `allure.results.directory`, by default `build/allure-results` (the module directory), which is where the Allure adapter writes. A schema version that cannot be read, for example when MySQL is down, becomes `unavailable`; the versions are read over a connection with a 2 s connect timeout, so the end of the run is not held up. A failure to write the file is reported on the error stream and does not fail the run, because the file is only a report. The file is written atomically, so parallel test processes never leave a half-written file.

Allure 3 shows `environment.properties` only in the Metadata block of the report home page. Its Variables block and the environment views come from `allurerc.mjs` instead, so `rfr-e2e/allurerc.mjs` reads the same file from `build/allure-results` (or `ALLURE_RESULTS_DIR`) and passes its pairs as the `variables` option. The Environments tab needs a declared environment, otherwise it says "No environments information available", so the config also declares one environment `local` ("Local stand") whose matcher accepts every result and which carries the same variables. The Gradle Allure plugin ignores an `allurerc.mjs` by itself and passes its own generated `allurerc.json` to the CLI, so `build.gradle` sets `allure.report.configFile` to this file; the config must not import anything (`allure` is not resolvable from the module directory) and must not set `output`, because the plugin passes `--output`.

It is listed first in `META-INF/services`. The order is the reason: if a suite extension fails in `beforeAll`, the extensions after it in the same class are never started and never closed (`SuiteExtensionTest.shouldNotStartExtensionAfterFailedOne`), so an extension placed after `StandReadinessExtension` would leave no file exactly when the stand is down. Extensions close in reverse order, so this one closes last, after all others, and its `afterSuite` still runs when another one fails to close. `ServicesOrderTest` checks the position and `AllureEnvironmentExtensionTest` runs sample classes in both orders.

## Stand readiness

`StandReadinessExtension` is a `SuiteExtension` that applies only to classes tagged `ui` (`SuiteExtension.appliesTo`), so the run starts it on the first such class. It polls MySQL (TCP), `rfr-auth` (`GET /.well-known/openid-configuration`, status below 400), `rfr-api` (`POST /graphql`, any answer) and the frontend (`GET /`, status below 400) at the addresses from `TestConfig` every 2 s until all answer or `rfr.standTimeout` is over. Every request has its own timeout, nothing waits forever. An HTTP check of `localhost` tries every address of the name and is satisfied by any of them (any other host name is requested as it is, so `Host` and the TLS certificate stay valid), because a dev server such as Vite may listen on `::1` only while Java resolves `localhost` to IPv4 first. The client speaks HTTP/1.1 only, because Java's default `Upgrade: h2c` probe makes the Vite dev server hang. On timeout the error names the silent services and the commands that start them (`docker compose up -d --wait`, `./gradlew runAll`, `cd rfr-gql-client && npm run dev`). Tests without the `ui` tag never start the wait, probes that need only MySQL included. A failed wait is a class-level failure (`initializationError`), so `build.gradle` sets `testLogging` for every `Test` task to print the message of a failure (without the stack) in the console; otherwise Gradle prints only a bare `FAILED` line.

It is listed above `MigrationCheckExtension` in `META-INF/services`, and the order is guaranteed: `ServiceLoader` returns providers in the order of the lines of a file, Jupiter registers auto-detected extensions in that order and calls `BeforeAllCallback` in the order of registration, and `SuiteRun.start()` is synchronized, so the migration check cannot begin before the wait has returned. If the wait fails, `beforeAll` of the class stops and the check does not run. `ServicesOrderTest` checks the order on the real `ServiceLoader`; `StandReadinessExtensionTest` runs `@ProbeTarget` sample classes against a fake HTTP server on an ephemeral port. A probe registers the extension with `@RegisterExtension` and its own settings, because the constructor is the only way to point it to a fake stand.

## Migration check

`MigrationCheckExtension` is a `SuiteExtension` listed in `META-INF/services`, so it runs once before all tests of a run, but only with `rfr.migrationCheck=true` (the `e2e` task sets it). It uses Flyway read-only (`SchemaMigrations`) and fails the run when the schema has migrations the backend files lack, the files have migrations the schema lacks, or a checksum differs. It compares the database with the checkout, not with the running process, so restart the backends after switching a branch. The error names the differences and the way out: switch the backends to the branch that migrated the database, or recreate it (`docker compose down -v`, then `bash localenv.sh`) and restart `rfr-auth` and `rfr-api`. `MigrationCheckExtensionTest` (tag `extension`, no MySQL) runs `@ProbeTarget` sample classes with a switch and a list of differences given through the second constructor: a broken schema stops the run, and `AllureEnvironmentExtension` still writes its file when it comes before the check, but not when it comes after it.

## Test settings

`io.student.rangiffler.config.TestConfig` is the only place that reads settings. A setting is taken from the system property `-Drfr.<key>`, then from the environment variable `RFR_<KEY>` (camelCase becomes upper snake case, so `frontUrl` is `RFR_FRONT_URL`), then from a local default, so tests run against a local environment with no extra arguments. Prerequisites: MySQL, `rfr-auth`, `rfr-api` and the frontend are running, and Chromium is installed for Playwright (the build disables Playwright's browser auto-download).

| Key | Default | Meaning |
|---|---|---|
| `rfr.frontUrl` | `http://localhost:3001` | Frontend base URL |
| `rfr.authUrl` | `http://localhost:9001` | `rfr-auth` base URL |
| `rfr.apiUrl` | `http://localhost:8081` | `rfr-api` base URL |
| `rfr.dbUrl` | `jdbc:mysql://localhost:3306` | MySQL server URL; the schema (`rangiffler-auth`, `rangiffler-api`) is appended in `Databases` |
| `rfr.dbUser` / `rfr.dbPassword` | `root` / `secret` | Database credentials |
| `rfr.browser` | `chromium` | `chromium`, `firefox` or `webkit` |
| `rfr.headless` | `true` | `false` shows the browser window |
| `rfr.browserLocale` | `en-US` | Browser locale for all UI tests (set in `PlaywrightOptions`) |
| `rfr.staticUserTimeout` | `30` | Seconds `StaticUserFixture` waits for a free pooled user before failing |
| `rfr.standTimeout` | `180` | Seconds `StandReadinessExtension` waits for MySQL, `rfr-auth`, `rfr-api` and the frontend; every request has its own 5 s timeout |
| `rfr.standCheck` | `true` | `false` turns the wait for the stand off completely |
| `rfr.commit` | `unknown` | Short commit hash for the Allure environment; Gradle sets it with `git rev-parse --short HEAD`, the tests never run git |
| `rfr.migrationCheck` | `false` | `true` makes `MigrationCheckExtension` compare the migrations applied to the local database with the files of `rfr-auth` and `rfr-api` before all tests; the `e2e` task sets it |
| `rfr.authMigrations` / `rfr.apiMigrations` | `../rfr-*/src/main/resources/db/migration/...` | Migration directories of the backends; Gradle passes absolute paths |

`rfr-e2e/build.gradle` forwards every `-Drfr.*` argument to the test JVM, and environment variables reach it by default, so pass a setting either way:
```bash
./gradlew :rfr-e2e:e2e -Drfr.headless=false
./gradlew :rfr-e2e:test --tests 'io.student.rangiffler.test.auth.LoginTest' -Drfr.browserLocale=ru-RU
RFR_FRONT_URL=http://localhost:3001 RFR_DB_PASSWORD=secret ./gradlew :rfr-e2e:e2e
```
To add a setting: declare it in `TestConfig` with `value("<key>", "<default>")` and expose it through a static accessor; no change in `build.gradle` is needed.

## Rules

- **Page Objects:** create and change them following the `rfr-page-objects` skill.
- **Playwright MCP is not a test run:** walking a scenario through Playwright MCP only explores the app and finds locators; it never confirms that a test works. Success is confirmed only by a JUnit run through Gradle (`./gradlew :rfr-e2e:test --tests '<TestClass>'`).
