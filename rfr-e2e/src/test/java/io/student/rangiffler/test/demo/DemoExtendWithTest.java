package io.student.rangiffler.test.demo;

import io.student.rangiffler.jupiter.annotation.Demo;
import io.student.rangiffler.jupiter.extension.DemoExtension;
import io.student.rangiffler.jupiter.extension.DemoValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Places where @ExtendWith can be put")
public class DemoExtendWithTest {

    private static final List<String> CLASS_SCOPE = List.of(
            "beforeAll", "preConstructTestInstance", "postProcessTestInstance", "beforeEach", "beforeTestExecution"
    );

    private static final List<String> METHOD_SCOPE = List.of("beforeEach", "beforeTestExecution");

    @Nested
    @ExtendWith(DemoExtension.class)
    @DisplayName("on a nested class")
    class NestedClass {

        @Test
        @DisplayName("the first test is served by the extension of the nested class")
        void first(DemoValue value) {
            value.report("nested first", this);
            assertTrue(value.events().containsAll(CLASS_SCOPE), "Callbacks of the class scope were not called for the first test");
        }

        @Test
        @DisplayName("the second test is served by the same extension instance")
        void second(DemoValue value) {
            value.report("nested second", this);
            assertTrue(value.events().containsAll(CLASS_SCOPE), "Callbacks of the class scope were not called for the second test");
        }
    }

    @Nested
    @DisplayName("on a method")
    class Method {

        @Test
        @ExtendWith(DemoExtension.class)
        @DisplayName("the first method gets its own extension instance")
        void first(DemoValue value) {
            value.report("method first", this);
            assertTrue(value.events().containsAll(METHOD_SCOPE), "Callbacks of the method scope were not called for the first method");
        }

        @Test
        @ExtendWith(DemoExtension.class)
        @DisplayName("the second method gets another extension instance")
        void second(DemoValue value) {
            value.report("method second", this);
            assertTrue(value.events().containsAll(METHOD_SCOPE), "Callbacks of the method scope were not called for the second method");
        }
    }

    @Nested
    @DisplayName("on a parameterized test method")
    class ParameterizedMethod {

        @ParameterizedTest
        @ValueSource(ints = {1, 2})
        @ExtendWith(DemoExtension.class)
        @DisplayName("every invocation shares the extension of its method")
        void first(int number, DemoValue value) {
            value.report("parameterized first " + number, this);
            assertTrue(value.events().containsAll(METHOD_SCOPE), "Callbacks of the method scope were not called for the invocation " + number);
        }

        @ParameterizedTest
        @ValueSource(ints = {3, 4})
        @ExtendWith(DemoExtension.class)
        @DisplayName("another method gets another extension instance")
        void second(int number, DemoValue value) {
            value.report("parameterized second " + number, this);
            assertTrue(value.events().containsAll(METHOD_SCOPE), "Callbacks of the method scope were not called for the invocation " + number);
        }
    }

    @Nested
    @DisplayName("on a test factory method")
    class FactoryMethod {

        @TestFactory
        @ExtendWith(DemoExtension.class)
        @DisplayName("the first factory is served with its dynamic tests")
        Stream<DynamicTest> first(DemoValue value) {
            return Stream.of(DynamicTest.dynamicTest("dynamic test", () -> {
                value.report("factory first", this);
                assertTrue(value.events().containsAll(METHOD_SCOPE), "Callbacks of the method scope were not called for the first factory");
            }));
        }

        @TestFactory
        @ExtendWith(DemoExtension.class)
        @DisplayName("the second factory gets another extension instance")
        Stream<DynamicTest> second(DemoValue value) {
            return Stream.of(DynamicTest.dynamicTest("dynamic test", () -> {
                value.report("factory second", this);
                assertTrue(value.events().containsAll(METHOD_SCOPE), "Callbacks of the method scope were not called for the second factory");
            }));
        }
    }

    @Nested
    @DisplayName("on a parameter of a test method")
    class MethodParameter {

        @Test
        @DisplayName("the first parameter gets its own extension instance")
        void first(@ExtendWith(DemoExtension.class) DemoValue value) {
            value.report("method parameter first", this);
            assertEquals("parameter 0 of first", value.origin(), "Parameter was not resolved for the first test method");
        }

        @Test
        @DisplayName("the second parameter gets another extension instance")
        void second(@ExtendWith(DemoExtension.class) DemoValue value) {
            value.report("method parameter second", this);
            assertEquals("parameter 0 of second", value.origin(), "Parameter was not resolved for the second test method");
        }
    }

    @Nested
    @DisplayName("on a parameter of a lifecycle method")
    class LifecycleParameter {

        private DemoValue value;

        @BeforeEach
        void prepare(@ExtendWith(DemoExtension.class) DemoValue value) {
            this.value = value;
        }

        @Test
        @DisplayName("the first test sees the value of @BeforeEach")
        void first() {
            value.report("lifecycle first", this);
            assertEquals("parameter 0 of prepare", value.origin(), "Parameter was not resolved for @BeforeEach in the first test");
        }

        @Test
        @DisplayName("the second test gets a new value of @BeforeEach")
        void second() {
            value.report("lifecycle second", this);
            assertEquals("parameter 0 of prepare", value.origin(), "Parameter was not resolved for @BeforeEach in the second test");
        }
    }

    @Nested
    @DisplayName("on a field")
    class Field {

        @ExtendWith(DemoExtension.class)
        private DemoValue value;

        @Test
        @DisplayName("the first test gets the field filled by the extension")
        void first() {
            value.report("field first", this);
            assertEquals("field value", value.origin(), "Field was not filled by the extension in the first test");
        }

        @Test
        @DisplayName("the second test gets the field filled again")
        void second() {
            value.report("field second", this);
            assertEquals("field value", value.origin(), "Field was not filled by the extension in the second test");
        }
    }

    @Nested
    @Demo
    @DisplayName("on a composed annotation")
    class MetaAnnotation {

        @Test
        @DisplayName("the first test is served through @Demo")
        void first(DemoValue value) {
            value.report("meta first", this);
            assertTrue(value.events().containsAll(CLASS_SCOPE), "Callbacks of the class scope were not called for the first test");
        }

        @Test
        @DisplayName("the second test is served by the same extension instance")
        void second(DemoValue value) {
            value.report("meta second", this);
            assertTrue(value.events().containsAll(CLASS_SCOPE), "Callbacks of the class scope were not called for the second test");
        }
    }

    @ExtendWith(DemoExtension.class)
    abstract static class AnnotatedBase {
    }

    @Nested
    @DisplayName("on a superclass")
    class FromSuperclass extends AnnotatedBase {

        @Test
        @DisplayName("the first test is served through the superclass")
        void first(DemoValue value) {
            value.report("superclass first", this);
            assertTrue(value.events().containsAll(CLASS_SCOPE), "Callbacks of the class scope were not called for the first test");
        }

        @Test
        @DisplayName("the second test is served by the same extension instance")
        void second(DemoValue value) {
            value.report("superclass second", this);
            assertTrue(value.events().containsAll(CLASS_SCOPE), "Callbacks of the class scope were not called for the second test");
        }
    }

    @ExtendWith(DemoExtension.class)
    interface AnnotatedInterface {
    }

    @Nested
    @DisplayName("on an interface")
    class FromInterface implements AnnotatedInterface {

        @Test
        @DisplayName("the first test is served through the interface")
        void first(DemoValue value) {
            value.report("interface first", this);
            assertTrue(value.events().containsAll(CLASS_SCOPE), "Callbacks of the class scope were not called for the first test");
        }

        @Test
        @DisplayName("the second test is served by the same extension instance")
        void second(DemoValue value) {
            value.report("interface second", this);
            assertTrue(value.events().containsAll(CLASS_SCOPE), "Callbacks of the class scope were not called for the second test");
        }
    }

    static class ConstructorParameter {

        private final DemoValue value;

        ConstructorParameter(@ExtendWith(DemoExtension.class) DemoValue value) {
            this.value = value;
        }

        @Test
        @DisplayName("the first test gets the value of its own constructor call")
        void first() {
            value.report("constructor first", this);
            assertEquals("parameter 0 of " + ConstructorParameter.class.getName(), value.origin(), "Parameter was not resolved for the constructor in the first test");
        }

        @Test
        @DisplayName("the second test gets the value of another constructor call")
        void second() {
            value.report("constructor second", this);
            assertEquals("parameter 0 of " + ConstructorParameter.class.getName(), value.origin(), "Parameter was not resolved for the constructor in the second test");
        }
    }
}
