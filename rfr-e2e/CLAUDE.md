# CLAUDE.md (rfr-e2e)

Guidance for writing and running the end-to-end tests in this module. The project-wide rules in the root `CLAUDE.md` (working agreement, indentation, Javadoc) apply here as well.

## Structure

One `src/test` source set, package `io.student.rangiffler`:

| Package | Contents |
|---|---|
| `config` | `TestConfig`, the only place that reads settings |
| `jupiter` | JUnit infrastructure: `annotation` (`@User` with `friends`, `incomeInvitations`, `outcomeInvitations`), `extension` (`UserExtension`, and `UserExtensionTest`, which runs nested sample classes through EngineTestKit) |
| `data` | Test data and its owners: `TestUser`, `UserFixture` with `BaseUserFixture` and the decorators `UserWithFriendsFixture`, `UserWithIncomeInvitationsFixture`, `UserWithOutcomeInvitationsFixture` (shared partner logic in `Partners`), `UserRequest`; `data/db` has `Databases`, `Jdbc` and the repositories (`ApiUserRepository`, `AuthUserRepository`), `Friendship` and `FriendshipStatus`; SQL files are in `src/test/resources/sql/{api,auth}` |
| `utils` | Stateless static helpers, e.g. `RandomDataUtils` |
| `ui` | `page` (Page Objects) and `component` (shared page parts) |
| `test` | `BaseWebTest` and the tests, grouped by feature (`auth`, `profile`, `people`, ...) |

Layering: `jupiter` has no SQL and no locators; `data` has no JUnit; tests have no locators and no SQL; no `ThreadLocal` for test state, it lives in the extension's `Store`.

## Running

Every UI test class carries its own `@Tag("ui")` (it is not inherited from `BaseWebTest`), so a new UI test class must add the tag itself, otherwise `e2e` skips it.
```bash
./gradlew :rfr-e2e:installPlaywright          # once: Chromium for Playwright
./gradlew :rfr-e2e:e2e                        # UI tests (tag ui) after waiting for the stand
./gradlew :rfr-e2e:test                       # tests without the ui tag; no stand needed
./gradlew :rfr-e2e:test --tests '<TestClass>' # one test; with --tests the ui tag is not excluded
```
`e2e` runs `waitForStand` first (`scripts/wait-for-stand.sh`), which waits for MySQL, `rfr-auth`, `rfr-api` and the frontend for up to `WAIT_TIMEOUT` seconds (default 180).

`UserExtensionTest` (tag `extension`) needs MySQL but no browser. Its nested sample classes are enabled only when the configuration parameter `rfr.testkit` is `true`, which the test sets itself, so they never run on their own.

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
