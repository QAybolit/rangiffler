---
name: rfr-test-review
description: Review rfr-e2e test code for ownership of test resources. Use for diffs in jupiter/ and data/, new @User preconditions and UserFixture decorators, and for failures that may come from leaked data or shared state.
---

# Review test resource ownership

A resource is anything a test gets and must give back: DB rows, Page, BrowserContext. For each one find the owner, when it is acquired, where it is kept and when it is released.

## Rules

1. **The owner is the decorator tree.** The Store holds only the root `UserFixture` from `UserRequest.fixture()`, and `UserExtension.afterEach` closes it. Each decorator owns its `origin` and its own partner fixtures. Extension fields, statics and `ThreadLocal` own nothing.
2. **Closable after any failed step.** `create()` is not atomic (account, profile, partners, friendship). Either register in the Store before `create()` ends, or roll back inside `create()` as `BaseUserFixture` and the decorators do; `close()` must work after a failure at any step.
3. **Symmetry of create and close.** A decorator deletes exactly what it created, by username, in reverse order: own partners, then `origin`. Steps are independent and the first failure is kept with the rest suppressed. No broad `DELETE` or `TRUNCATE`.
4. **A new precondition is a new decorator.** Compose it in `UserRequest.fixture()`; no flag in a fixture and no branch in `UserExtension`. The extension knows only `UserRequest` and `UserFixture`.
5. **One `@User`, one `TestUser` parameter.** The Store key is the test's unique id, not the parameter index, and only the first `@User` parameter is read. Related users live inside `TestUser` and the fixture tree, not in extra parameters.
6. **Browser resources belong to `@UsePlaywright`.** `BaseWebTest` only declares it with `PlaywrightOptions` and keeps the injected `Page`; tests and extensions never create or close Playwright, Browser, BrowserContext or Page.
7. **Layers.** `jupiter` has no SQL and no locators; `data` has no JUnit; SQL lives only in `data/db`, which does not depend on `data`; `test` has no locators and no SQL.
8. **Evidence.** Back every finding with a probe in `jupiter/extension/UserExtensionTest` (EngineTestKit, `userExists` from `data/db`, sample classes with `StoreProbe`) or a concrete code line. Propose no change without a cause found.

## Answer format

- For each resource: owner, acquisition, storage, release.
- For each finding: file and line, failure scenario, how to reproduce (which probe, or which `create()` step to make fail).
- Three lists: confirmed by a run, from reading the code, not checked.
- If there are no defects, say so and list the limits of the solution.
