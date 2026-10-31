package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.data.UserRequest;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Arrays;

/**
 * Creates a {@code @User} test user before each test, passes it as a parameter and removes it afterwards.
 */
public final class UserExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

    /**
     * Creates the user when the test method has a {@code @User} parameter.
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        Arrays.stream(context.getRequiredTestMethod().getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(User.class))
                .findFirst()
                .map(parameter -> new UserRequest(parameter.getAnnotation(User.class)))
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
     * Returns the user created for the current test.
     */
    @Override
    public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensionContext.getStore(NAMESPACE).get(extensionContext.getUniqueId(), UserFixture.class).user();
    }
}
