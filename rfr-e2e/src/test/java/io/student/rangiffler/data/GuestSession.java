package io.student.rangiffler.data;

import com.microsoft.playwright.Page;

/**
 * A page of its own browser context where nobody is signed in.
 */
public record GuestSession(Page page) {
}
