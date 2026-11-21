package io.student.rangiffler.data;

import com.microsoft.playwright.Page;

/**
 * A page of its own browser context where the test user is already signed in.
 */
public record UserSession(Page page, TestUser user) {
}
