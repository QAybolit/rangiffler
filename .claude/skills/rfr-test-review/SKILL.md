---
name: rfr-test-review
description: Review rfr-e2e test code for ownership of test resources. Use for diffs in jupiter/ and data/, new @User preconditions and UserFixture decorators, and for failures that may come from leaked data or shared state.
---

# Review test resource ownership

A resource is anything a test gets and must give back: DB rows, Page, BrowserContext. For each one find the owner, when it is acquired, where it is kept and when it is released.

## Rules

1. **The owner is the decorator tree.** The Store holds only the root `UserFixture` from `UserRequest.fixture()`, and `UserExtension.afterEach` closes it. Each decorator owns its `origin` and its own partner fixtures. Extension fields, statics and `ThreadLocal` own nothing, except the pool in rule 9.
2. **Closable after any failed step.** `create()` is not atomic (account, profile, partners, friendship). Either register in the Store before `create()` ends, or roll back inside `create()` as `BaseUserFixture` and the decorators do; `close()` must work after a failure at any step.
3. **Symmetry of create and close.** A decorator deletes exactly what it created, by username, in reverse order: own partners, then `origin`. Partner users and their friendships are created and closed through `Partners`, so check that a decorator does not bypass it. Steps are independent and the first failure is kept with the rest suppressed. No broad `DELETE` or `TRUNCATE`.
4. **A new precondition is a new decorator.** Compose it in `UserRequest.fixture()`; no flag in a fixture and no branch in `UserExtension`. The extension knows only `UserRequest` and `UserFixture`.
5. **One `@User`, one `TestUser` parameter.** The Store key is the test's unique id, not the parameter index, and only the first `@User` parameter is read. Related users live inside `TestUser` and the fixture tree, not in extra parameters. An annotation in a place the extension does not support (a second parameter, a lifecycle method) must fail with a clear error before any resource is acquired, never silently share or skip.
6. **Browser resources belong to `@UsePlaywright`.** `BaseWebTest` only declares it with `PlaywrightOptions` and keeps the injected `Page`; tests and extensions never create or close Playwright, Browser, BrowserContext or Page.
7. **Layers.** `jupiter` has no SQL and no locators; `data` has no JUnit; SQL lives only in `data/db`, which does not depend on `data`; `test` has no locators and no SQL.
8. **Evidence.** Back every finding with a probe in `jupiter/extension/UserExtensionTest` or `UsersQueueExtensionTest` (EngineTestKit, `userExists` from `data/db`, sample classes with `StoreProbe`) or a concrete code line. Propose no change without a cause found. A probe must prove it ran: assert the number of failed or passed sample tests, and check a failure by its message, not only by its class. Never hardcode the pool size in a probe.
9. **Borrowed resources.** A user taken from the static pool (`StaticUserFixture`) is borrowed, not created: `close()` gives back only what this fixture took, exactly once, and a stale `close()` must not touch a user another test now holds. Waiting is bounded by a timeout and fails with a clear error. Nothing is deleted from the DB, and the user's state is not reset, so a test must not change a pooled user.

## Answer format

- For each resource: owner, acquisition, storage, release (borrowed or created).
- For each finding: file and line, failure scenario, how to reproduce.
- Three lists: confirmed by a run, from reading the code, not checked.
- If there are no defects, say so and list the limits of the solution.
