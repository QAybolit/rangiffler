package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.extension.MigrationCheckExtension;
import io.student.rangiffler.jupiter.extension.StandReadinessExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.Extension;

import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the extensions in the order in which {@code ServiceLoader} finds them, which is the order in which JUnit registers and calls them.
 */
@DisplayName("Auto-detected extensions are found in the registration order")
class ServicesOrderTest {

    @Test
    @DisplayName("StandReadinessExtension comes before MigrationCheckExtension")
    void shouldWaitForStandBeforeMigrationCheck() {
        List<String> found = ServiceLoader.load(Extension.class).stream().map(provider -> provider.type().getName()).toList();
        int readiness = found.indexOf(StandReadinessExtension.class.getName());

        assertTrue(
                readiness >= 0 && readiness < found.indexOf(MigrationCheckExtension.class.getName()),
                "The migration check is found before the wait for the stand or one of them is not registered"
        );
    }
}
