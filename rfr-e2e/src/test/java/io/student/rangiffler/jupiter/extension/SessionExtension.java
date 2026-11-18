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
     * Opens a context for the parameter and signs the user in when it is a {@code UserSession}.
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        validate(parameterContext, extensionContext);
        Page page = open(extensionContext);
        if (isGuest(parameterContext)) {
            return new GuestSession(page);
        }
        TestUser user = UserExtension.fixture(parameterContext, extensionContext).user();
        new SignIn().signIn(page, user.username(), user.password());
        return new UserSession(page, user);
    }

    /**
     * Closes the contexts opened for the test in reverse order, after saving the trace of each when the test failed, keeping the first failure and suppressing the rest; it runs before the cleanup of {@code UserExtension}, so a user is not removed while its browser is still open.
     */
    @Override
    public void afterEach(ExtensionContext context) {
        Opened opened = context.getStore(NAMESPACE).remove(context.getUniqueId(), Opened.class);
        if (opened != null) {
            opened.close(context);
        }
    }

    private static void validate(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (extensionContext.getTestMethod().filter(parameterContext.getDeclaringExecutable()::equals).isEmpty()) {
            throw new ParameterResolutionException("A session is supported only on a parameter of the test method");
        }
        boolean asked = parameterContext.isAnnotated(User.class);
        if (isGuest(parameterContext) && asked) {
            throw new ParameterResolutionException("@User is not allowed on GuestSession, a guest has no user");
        }
        if (!isGuest(parameterContext) && !asked) {
            throw new ParameterResolutionException("UserSession needs @User, use GuestSession for a session without a user");
        }
    }

    /**
     * Opens the context and registers it for closing before anything else, such as reading a timeout setting, can fail.
     */
    private static Page open(ExtensionContext extensionContext) {
        BrowserContext context = BrowserExtension.getOrCreateBrowser(extensionContext).newContext(PlaywrightOptions.contextOptions());
        SessionFixture fixture = SessionFixture.create(context);
        ContextTrace trace = new ContextTrace(context);
        opened(extensionContext).add(fixture, trace);
        trace.start(extensionContext.getDisplayName());
        ContextTimeouts.apply(context);
        return fixture.page();
    }

    private static boolean isGuest(ParameterContext parameterContext) {
        return parameterContext.getParameter().getType().equals(GuestSession.class);
    }

    private static Opened opened(ExtensionContext context) {
        return context.getStore(NAMESPACE).getOrComputeIfAbsent(context.getUniqueId(), key -> new Opened(), Opened.class);
    }

    /**
     * The session fixtures opened for one test with their traces, in the order of opening.
     */
    private static final class Opened {

        private final List<SessionFixture> fixtures = new ArrayList<>();
        private final List<ContextTrace> traces = new ArrayList<>();

        void add(SessionFixture fixture, ContextTrace trace) {
            fixtures.add(fixture);
            traces.add(trace);
        }

        void close(ExtensionContext context) {
            RuntimeException failure = null;
            for (int index = fixtures.size() - 1; index >= 0; index--) {
                try {
                    traces.get(index).finish(context, "trace-session-" + (index + 1) + ".zip");
                } catch (RuntimeException e) {
                    failure = keep(failure, e);
                }
                try {
                    fixtures.get(index).close();
                } catch (RuntimeException e) {
                    failure = keep(failure, e);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }

        private static RuntimeException keep(RuntimeException first, RuntimeException next) {
            if (first == null) {
                return next;
            }
            first.addSuppressed(next);
            return first;
        }
    }
}
