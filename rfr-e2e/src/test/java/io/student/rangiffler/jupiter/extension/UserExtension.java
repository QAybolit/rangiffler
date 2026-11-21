package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.fixture.UserFixture;
import io.student.rangiffler.data.UserRequest;
import io.student.rangiffler.data.UserSession;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates a test user for every {@code @User} parameter before each test, passes them as parameters and removes them afterwards.
 */
public final class UserExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

    /**
     * Creates one user per {@code @User} parameter of the test method that holds a user ({@code TestUser} or {@code UserSession}), validating every request first and registering each fixture in the store as soon as it exists so a failure on a later one still closes the earlier ones.
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        List<UserRequest> requested = context.getTestMethod().stream()
                .flatMap(method -> Arrays.stream(method.getParameters()))
                .filter(UserExtension::asked)
                .map(parameter -> parameter.getAnnotation(User.class))
                .map(user -> new UserRequest(user.friends(), user.incomeInvitations(), user.outcomeInvitations()))
                .toList();
        if (!requested.isEmpty()) {
            List<UserFixture> fixtures = new ArrayList<>();
            context.getStore(NAMESPACE).put(context.getUniqueId(), fixtures);
            for (UserRequest request : requested) {
                fixtures.add(request.fixture());
            }
        }
    }

    /**
     * Closes the user fixtures created for the test in reverse order, keeping the first failure and suppressing the rest.
     */
    @Override
    public void afterEach(ExtensionContext context) {
        List<?> fixtures = context.getStore(NAMESPACE).remove(context.getUniqueId(), List.class);
        RuntimeException failure = null;
        if (fixtures != null) {
            for (Object fixture : fixtures.reversed()) {
                try {
                    UserFixture.class.cast(fixture).close();
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

    /**
     * Supports {@code TestUser} parameters annotated with {@code @User}.
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.isAnnotated(User.class)
                && parameterContext.getParameter().getType().equals(TestUser.class);
    }

    /**
     * Returns the user created for the parameter and fails when the annotated parameter belongs to another method than the test method, where it would silently share a test's user.
     */
    @Override
    public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (extensionContext.getTestMethod().filter(parameterContext.getDeclaringExecutable()::equals).isEmpty()) {
            throw new ParameterResolutionException("@User is supported only on a parameter of the test method");
        }
        return fixture(parameterContext, extensionContext).user();
    }

    /**
     * Returns the fixture that was created for the {@code @User} parameter, found by its position among the {@code @User} parameters of the test method.
     */
    static UserFixture fixture(ParameterContext parameterContext, ExtensionContext extensionContext) {
        long position = Arrays.stream(extensionContext.getRequiredTestMethod().getParameters())
                .limit(parameterContext.getIndex())
                .filter(UserExtension::asked)
                .count();
        List<?> fixtures = extensionContext.getStore(NAMESPACE).get(extensionContext.getUniqueId(), List.class);
        if (fixtures == null) {
            throw new ParameterResolutionException("@User needs UserExtension, which creates the user before the test");
        }
        return UserFixture.class.cast(fixtures.get((int) position));
    }

    /**
     * Tells whether the parameter asks for a user: it carries {@code @User} and has a type that holds a user, so an annotation on any other type, such as {@code GuestSession}, creates nothing and is rejected by the resolver of that type.
     */
    private static boolean asked(Parameter parameter) {
        return parameter.isAnnotationPresent(User.class)
                && (parameter.getType().equals(TestUser.class) || parameter.getType().equals(UserSession.class));
    }
}
