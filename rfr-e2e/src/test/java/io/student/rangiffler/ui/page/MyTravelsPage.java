package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.Header;
import io.student.rangiffler.ui.component.PhotoCard;
import io.student.rangiffler.ui.component.Sidebar;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class MyTravelsPage {

    public static final String URL = TestConfig.frontUrl() + "/my-travels";

    private static final Pattern VISITED = Pattern.compile("\\d+ countr(y|ies) visited");

    private final Page page;
    private final Header header;
    private final Sidebar sidebar;
    private final Locator photos;
    private final Locator visitedButton;
    private final Locator countryList;

    public MyTravelsPage(Page page) {
        this.page = page;
        this.header = new Header(page);
        this.sidebar = new Sidebar(page);
        this.photos = page.getByRole(AriaRole.ARTICLE);
        this.visitedButton = page.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText(VISITED));
        this.countryList = page.getByRole(AriaRole.TOOLTIP);
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    public Header header() {
        return header;
    }

    public PhotoCard photo(String description) {
        return new PhotoCard(photos.filter(new Locator.FilterOptions().setHasText(description)));
    }

    @Step("Check that the feed lists exactly the photos {descriptions}")
    public MyTravelsPage shouldHavePhotos(String... descriptions) {
        assertThat(photos).hasCount(descriptions.length);
        for (String description : descriptions) {
            assertThat(photos.filter(new Locator.FilterOptions().setHasText(description))).hasCount(1);
        }
        return this;
    }

    @Step("Check that the feed does not list the photos {descriptions}")
    public MyTravelsPage shouldNotHavePhotos(String... descriptions) {
        for (String description : descriptions) {
            assertThat(photos.filter(new Locator.FilterOptions().setHasText(description))).hasCount(0);
        }
        return this;
    }

    @Step("Open the list of visited countries")
    public MyTravelsPage openCountries() {
        visitedButton.click();
        return this;
    }

    @Step("Filter the feed by the country {country}")
    public MyTravelsPage filterByCountry(String country) {
        countryList.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(country).setExact(true)).click();
        return this;
    }

    @Step("Reset the country filter")
    public MyTravelsPage resetCountryFilter() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset country filter").setExact(true)).click();
        return this;
    }

    @Step("Check that {total} countries are visited")
    public MyTravelsPage shouldHaveCountriesVisited(int total) {
        assertThat(page.getByText(total + (total == 1 ? " country visited" : " countries visited"), new Page.GetByTextOptions().setExact(true))).isVisible();
        return this;
    }

    @Step("Check that the feed is filtered by the country {country}")
    public MyTravelsPage shouldHaveCountryFilter(String country) {
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Country filter: " + country).setExact(true))).isVisible();
        return this;
    }
}
