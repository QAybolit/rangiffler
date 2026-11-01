package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.data.UserRequest;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

/**
 * Creates a {@code @User} test user before each test, passes it as a parameter and removes it afterwards.
 */
public final class UserExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

    /**
     * Creates the user when the test method has a {@code @User} parameter and fails before creating anything when it has more than one.
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        List<Parameter> requested = Arrays.stream(context.getRequiredTestMethod().getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(User.class))
                .toList();
        if (requested.size() > 1) {
            throw new IllegalStateException("Test method has " + requested.size() + " @User parameters, only one is supported");
        }
        requested.stream()
                .findFirst()
                .map(parameter -> parameter.getAnnotation(User.class))
                .map(user -> new UserRequest(user.friends(), user.incomeInvitations(), user.outcomeInvitations()))
                .ifPresent(request -> context.getStore(NAMESPACE).put(context.getUniqueId(), request.fixture()));
    }

    /**
     * Closes the user fixture created for the test, if any.
     */
    @Override
    public void afterEach(ExtensionContext context) {
        UserFixture fixture = context.getStore(NAMESPACE).remove(context.getUniqueId(), UserFixture.class);
        if (fixture != null) {
            fixture.close();
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
     * Returns the user created for the current test and fails when the annotated parameter belongs to another method than the test method, where it would silently share the test's user.
     */
    @Override
    public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (!parameterContext.getDeclaringExecutable().equals(extensionContext.getRequiredTestMethod())) {
            throw new ParameterResolutionException("@User is supported only on a parameter of the test method");
        }
        return extensionContext.getStore(NAMESPACE).get(extensionContext.getUniqueId(), UserFixture.class).user();
    }
}
