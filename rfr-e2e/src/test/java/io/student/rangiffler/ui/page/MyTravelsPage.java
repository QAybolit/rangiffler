package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Page;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.Header;
import io.student.rangiffler.ui.component.Sidebar;

public final class MyTravelsPage {

    public static final String URL = TestConfig.frontUrl() + "/my-travels";

    private final Page page;
    private final Header header;
    private final Sidebar sidebar;

    public MyTravelsPage(Page page) {
        this.page = page;
        this.header = new Header(page);
        this.sidebar = new Sidebar(page);
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    public Header header() {
        return header;
    }
}
