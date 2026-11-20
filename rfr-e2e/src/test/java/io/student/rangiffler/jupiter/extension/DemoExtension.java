package io.student.rangiffler.jupiter.extension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Demo extension that implements every lifecycle callback and a parameter resolver, so a test can see where {@code @ExtendWith} takes effect.
 */
public final class DemoExtension implements
        BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback,
        AfterTestExecutionCallback, AfterEachCallback, AfterAllCallback,
        TestInstancePreConstructCallback, TestInstancePostProcessor, TestInstancePreDestroyCallback,
        ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(DemoExtension.class);

    public DemoExtension() {
        System.out.println("[demo] new DemoExtension " + id());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        record(context, "beforeAll");
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        record(context, "beforeEach");
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        record(context, "beforeTestExecution");
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        record(context, "afterTestExecution");
    }

    @Override
    public void afterEach(ExtensionContext context) {
        record(context, "afterEach");
    }

    @Override
    public void afterAll(ExtensionContext context) {
        record(context, "afterAll");
    }

    @Override
    public void preConstructTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext context) {
        record(context, "preConstructTestInstance");
    }

    /**
     * Fills the instance fields that carry {@code @ExtendWith} with a {@code DemoValue}, which is how a field registers an extension.
     */
    @Override
    public void postProcessTestInstance(Object instance, ExtensionContext context) throws Exception {
        record(context, "postProcessTestInstance");
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(ExtendWith.class) && field.getType().equals(DemoValue.class)) {
                field.setAccessible(true);
                field.set(instance, new DemoValue("field " + field.getName(), journal(context), id()));
            }
        }
    }

    @Override
    public void preDestroyTestInstance(ExtensionContext context) {
        record(context, "preDestroyTestInstance");
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(DemoValue.class);
    }

    @Override
    public DemoValue resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        record(extensionContext, "resolveParameter");
        return new DemoValue(
                "parameter " + parameterContext.getIndex() + " of " + parameterContext.getDeclaringExecutable().getName(),
                journal(extensionContext),
                id()
        );
    }

    private void record(ExtensionContext context, String event) {
        journal(context).add(event);
        System.out.println("[demo] " + event + " <- " + context.getDisplayName() + " (extension " + id() + ")");
    }

    private String id() {
        return Integer.toHexString(System.identityHashCode(this));
    }

    @SuppressWarnings("unchecked")
    private List<String> journal(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE).computeIfAbsent(
                context.getRequiredTestClass(), key -> new CopyOnWriteArrayList<String>(), CopyOnWriteArrayList.class
        );
    }
}
