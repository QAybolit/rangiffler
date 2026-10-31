package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.StaticUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Arrays;

/**
 * Takes a {@code @StaticUser} test user from the pool before each test, passes it as a parameter and gives it back afterwards.
 */
public final class UsersQueueExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UsersQueueExtension.class);

    /**
     * Takes the user when the test method has a {@code @StaticUser} parameter.
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        if (Arrays.stream(context.getRequiredTestMethod().getParameters()).anyMatch(parameter -> parameter.isAnnotationPresent(StaticUser.class))) {
            context.getStore(NAMESPACE).put(context.getUniqueId(), StaticUserFixture.create());
        }
    }

    /**
     * Gives the user back to the pool, if one was taken for the test.
     */
    @Override
    public void afterEach(ExtensionContext context) {
        UserFixture fixture = context.getStore(NAMESPACE).remove(context.getUniqueId(), UserFixture.class);
        if (fixture != null) {
            fixture.close();
        }
    }

    /**
     * Supports {@code TestUser} parameters annotated with {@code @StaticUser}.
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.isAnnotated(StaticUser.class)
                && parameterContext.getParameter().getType().equals(TestUser.class);
    }

    /**
     * Returns the user taken for the current test.
     */
    @Override
    public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensionContext.getStore(NAMESPACE).get(extensionContext.getUniqueId(), UserFixture.class).user();
    }
}
