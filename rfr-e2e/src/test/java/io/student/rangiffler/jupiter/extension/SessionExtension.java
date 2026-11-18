package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.impl.junit.BrowserExtension;
import io.student.rangiffler.data.GuestSession;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserSession;
import io.student.rangiffler.data.fixture.SessionFixture;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.flow.SignIn;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens an additional browser context for every {@code UserSession} and {@code GuestSession} parameter of a test, signs the {@code @User} user in through the UI, and closes the contexts after the test.
 */
public final class SessionExtension implements ParameterResolver, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(SessionExtension.class);

    /**
     * Supports the parameters of the types {@code UserSession} and {@code GuestSession}.
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type.equals(UserSession.class) || type.equals(GuestSession.class);
    }

    /**
     * Opens the context, registers it for closing before anything else, such as reading a timeout setting, can fail, and signs the user in when the parameter asks for one.
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (!parameterContext.getDeclaringExecutable().equals(extensionContext.getRequiredTestMethod())) {
            throw new ParameterResolutionException("A session is supported only on a parameter of the test method");
        }
        boolean asked = parameterContext.isAnnotated(User.class);
        boolean guest = parameterContext.getParameter().getType().equals(GuestSession.class);
        if (guest && asked) {
            throw new ParameterResolutionException("@User is not allowed on GuestSession, a guest has no user");
        }
        if (!guest && !asked) {
            throw new ParameterResolutionException("UserSession needs @User, use GuestSession for a session without a user");
        }
        BrowserContext context = BrowserExtension.getOrCreateBrowser(extensionContext).newContext(PlaywrightOptions.contextOptions());
        SessionFixture fixture = SessionFixture.create(context);
        opened(extensionContext).add(fixture);
        ContextTimeouts.apply(context);
        if (guest) {
            return new GuestSession(fixture.page());
        }
        TestUser user = UserExtension.fixture(parameterContext, extensionContext).user();
        signIn(fixture.page(), user);
        return new UserSession(fixture.page(), user);
    }

    /**
     * Closes the contexts opened for the test in reverse order, keeping the first failure and suppressing the rest; it runs before the cleanup of {@code UserExtension}, so a user is not removed while its browser is still open.
     */
    @Override
    public void afterEach(ExtensionContext context) {
        Opened opened = context.getStore(NAMESPACE).remove(context.getUniqueId(), Opened.class);
        if (opened != null) {
            opened.close();
        }
    }

    private static void signIn(Page page, TestUser user) {
        new SignIn().signIn(page, user.username(), user.password());
    }

    private static Opened opened(ExtensionContext context) {
        return context.getStore(NAMESPACE).getOrComputeIfAbsent(context.getUniqueId(), key -> new Opened(), Opened.class);
    }

    /**
     * The session fixtures opened for one test, in the order of opening.
     */
    private static final class Opened {

        private final List<SessionFixture> fixtures = new ArrayList<>();

        void add(SessionFixture fixture) {
            fixtures.add(fixture);
        }

        void close() {
            RuntimeException failure = null;
            for (SessionFixture fixture : fixtures.reversed()) {
                try {
                    fixture.close();
                } catch (RuntimeException e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
