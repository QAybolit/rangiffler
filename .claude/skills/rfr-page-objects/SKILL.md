---
name: rfr-page-objects
description: Create or update a Playwright Java Page Object (and reusable components) for the rfr-e2e module following the project's PO rules. Use when the user asks to add a page object, a PO for a page/screen, or a UI component class for UI tests.
---

# Create Page Object

Generate Page Objects for `rfr-e2e` (Playwright Java + Allure + JUnit 6). Place them under
`rfr-e2e/src/test/java/io/student/rangiffler/page/` (reusable parts under `page/component/`).
Before writing, inspect the page in the running app (Playwright MCP snapshot, if available) to pick real locators.

## Rules

1. **Final classes.** Every Page Object and component is declared `public final class`.
2. **Composition over inheritance.** Do not extend a base page class. Reusable functionality (header, nav drawer,
   table, form, toast, etc.) lives in separate component classes that a page holds as fields and delegates to.
   The only thing a PO receives is `Page` (or a component's root `Locator`) through its constructor.
3. **Check methods contain only assertions.** Methods named `should...` / `check...` never click, fill,
   navigate or otherwise change state. They return the PO (`this`) for chaining.
4. **One business action = one PO method.** Steps that make up a single business action (e.g. filling a form)
   are merged into one method such as `fillForm(username, password, confirmPassword)`, not exposed as separate
   `setUsername()` / `setPassword()` methods. Submitting is a separate action method unless the user asks otherwise.
5. **Always `@Step`.** Every public PO method (actions and checks) has `io.qameta.allure.Step`.
6. **Log parameters.** Put method parameters into the step name with placeholders, e.g.
   `@Step("Fill registration form for user '{username}'")`. Placeholders use the Java parameter names.
   Never print secrets: omit passwords/tokens from the step text (or use a masked value).
7. **Page URL is `public static final String URL`.** Each page that has its own URL declares it, built from
   `TestConfig` (e.g. `public static final String URL = TestConfig.frontUrl() + "/profile";`). Do not hardcode hosts.
8. **Checks use Playwright conditions.** Use `PlaywrightAssertions.assertThat(...)` (`isVisible()`, `hasText()`,
   `hasValue()`, `hasURL()`, ...), which auto-retry. Do not use `Thread.sleep`, `isVisible()` booleans with
   JUnit asserts, or manual waits, unless the user explicitly asks for something else.

9. **Constructor does only assignment.** The constructor takes `Page` (or a component's root `Locator`) and does
   nothing except assign it to `private final Page page` and initialise the PO's `private final` fields
   (locators, composed components). No navigation, clicks, assertions, waits or other logic in the constructor.

10. **No page-loaded checks.** Do not add `checkPageLoaded()`, `shouldBeOpened()`, `shouldBeLoaded()` or any similar
    "page is opened/loaded" method to a PO. Web-first assertions auto-wait and retry, so a separate load check is
    redundant (a URL + heading check is still just a load check). Do not call a PO method just to wait for navigation.
    Add only checks that verify a business result (e.g. `shouldHaveUsername(...)`, `shouldHaveSuccessMessage()`)
    as regular `should...` methods, and call them from the test where that result matters.

11. **Save actions wait for the GraphQL mutation.** A PO method that submits data to the backend (e.g.
    `clickSave()` on the profile page) must wait for the response of the matching GraphQL operation (e.g.
    `UpdateUser`) with `page.waitForResponse(predicate, button::click)`, otherwise a following `reload()` or
    navigation outruns the mutation and loses the changes. Match the response by URL (`/graphql`) and the
    operation name in the request body:

    ```java
    @Step("Click 'Save' and wait for UpdateUser mutation")
    public ProfilePage clickSave() {
      page.waitForResponse(
          response -> response.url().endsWith("/graphql")
              && response.request().postData() != null
              && response.request().postData().contains("UpdateUser"),
          saveButton::click
      );
      return this;
    }
    ```

## Conventions

- Locators are `private final Locator` fields initialised in the constructor / field initialiser; prefer
  `getByRole`, `getByPlaceholder`, `getByText`, `getByLabel` over CSS/XPath.
- Navigation methods return the next PO (`new MyTravelsPage(page)`), other actions return `this`.
- Tests using POs live in feature packages under `io.student.rangiffler.test` (e.g. `auth`, `profile`) and extend `io.student.rangiffler.test.BaseWebTest`. Test data comes from `utils/RandomDataUtils`,
  URLs from `config/TestConfig`.

## Example

```java
package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeoplePage {

  public static final String URL = TestConfig.frontUrl() + "/people";

  private final Page page;
  private final Header header;
  private final Locator searchInput;

  public PeoplePage(Page page) {
    this.page = page;
    this.header = new Header(page);
    this.searchInput = page.getByPlaceholder("Search people");
  }

  public Header header() {
    return header;
  }

  @Step("Search people by '{query}'")
  public PeoplePage search(String query) {
    searchInput.fill(query);
    searchInput.press("Enter");
    return this;
  }

  @Step("Check that people list contains '{username}'")
  public PeoplePage shouldContainPerson(String username) {
    assertThat(page.getByText(username)).isVisible();
    return this;
  }
}
```

## Workflow

1. Identify the page/component, its URL and the business actions the tests need.
2. Look at the live page for accurate locators.
3. Write the final class following the rules above; extract shared UI into a component and compose it.
4. Compile and, if a test uses the PO, run it:
   `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 ./gradlew :rfr-e2e:test --tests '<TestClass>'`
5. Report which rules, if any, an existing PO violates; do not silently rewrite unrelated POs.
