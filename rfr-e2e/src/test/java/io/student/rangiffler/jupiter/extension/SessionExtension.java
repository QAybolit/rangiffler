package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.impl.junit.BrowserExtension;
import io.student.rangiffler.data.GuestSession;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserSession;
import io.student.rangiffler.data.fixture.SessionFixture;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.flow.SignIn;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Opens an additional browser context for every {@code UserSession} and {@code GuestSession} parameter of a test, records its trace, signs the {@code @User} user in through the UI, and closes the contexts after the test.
 */
public final class SessionExtension implements ParameterResolver, AfterTestExecutionCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(SessionExtension.class);

    private static final String SESSION = "session-";

    private static final String TRACE = "trace-";

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
        Page page = open(parameterContext, extensionContext);
        if (isGuest(parameterContext)) {
            return new GuestSession(page);
        }
        TestUser user = UserExtension.fixture(parameterContext, extensionContext).user();
        new SignIn().signIn(page, user.username(), user.password());
        return new UserSession(page, user);
    }

    /**
     * Stops the traces of the session contexts in reverse order, keeping each only when the test failed; it runs before any {@code afterEach} callback, so the contexts are still open and the Allure test case is still running.
     */
    @Override
    public void afterTestExecution(ExtensionContext context) {
        release(context, TRACE, ContextTrace.class, trace -> trace.finish(context));
    }

    /**
     * Closes the contexts opened for the test in reverse order, keeping the first failure and suppressing the rest; it runs before the cleanup of {@code UserExtension} when it is registered after it, so a user is not removed while its browser is still open.
     */
    @Override
    public void afterEach(ExtensionContext context) {
        release(context, SESSION, SessionFixture.class, SessionFixture::close);
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
     * Opens the context and registers it for closing before anything else can fail, then starts its trace before the sign-in, so a failed sign-in is recorded.
     */
    private static Page open(ParameterContext parameterContext, ExtensionContext extensionContext) {
        int index = parameterContext.getIndex();
        SessionFixture fixture = SessionFixture.create(BrowserExtension.getOrCreateBrowser(extensionContext).newContext(PlaywrightOptions.contextOptions()));
        extensionContext.getStore(NAMESPACE).put(SESSION + index, fixture);
        extensionContext.getStore(NAMESPACE).put(
                TRACE + index,
                ContextTrace.started(fixture.page().context(), "trace-session-" + index + ".zip", extensionContext.getDisplayName())
        );
        ContextTimeouts.apply(fixture.page().context());
        return fixture.page();
    }

    private static boolean isGuest(ParameterContext parameterContext) {
        return parameterContext.getParameter().getType().equals(GuestSession.class);
    }

    /**
     * Takes the resources kept under {@code prefix} and the index of each parameter of the test method, from the last parameter to the first, and applies the action to every one that was opened; the first failure is thrown after all of them, the others are suppressed.
     */
    private static <T> void release(ExtensionContext context, String prefix, Class<T> type, Consumer<T> action) {
        RuntimeException failure = null;
        for (int index = context.getTestMethod().map(Method::getParameterCount).orElse(0) - 1; index >= 0; index--) {
            T resource = context.getStore(NAMESPACE).remove(prefix + index, type);
            if (resource != null) {
                try {
                    action.accept(resource);
                } catch (RuntimeException e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
