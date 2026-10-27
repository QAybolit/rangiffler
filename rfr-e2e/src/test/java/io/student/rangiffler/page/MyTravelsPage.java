package io.student.rangiffler.page;

import com.microsoft.playwright.Page;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;

public final class MyTravelsPage {

    public static final String URL = TestConfig.frontUrl() + "/my-travels";

    private final Page page;
    private final Header header;

    public MyTravelsPage(Page page) {
        this.page = page;
        this.header = new Header(page);
    }

    public Header header() {
        return header;
    }
}
