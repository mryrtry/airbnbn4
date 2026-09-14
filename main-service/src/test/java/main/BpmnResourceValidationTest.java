package main;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BpmnResourceValidationTest {

    private static final List<String> BPMN_RESOURCES = List.of(
            "booking-lifecycle.bpmn",
            "listing-create.bpmn",
            "listing-update.bpmn",
            "listing-delete.bpmn",
            "resolution-lifecycle.bpmn"
    );

    @TestFactory
    Stream<DynamicTest> bpmnResourcesAreValid() {
        return BPMN_RESOURCES.stream()
                .map(resource -> DynamicTest.dynamicTest(resource, () -> validate(resource)));
    }

    private void validate(String resource) {
        assertDoesNotThrow(() -> {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(input, "BPMN resource not found: " + resource);
                Bpmn.validateModel(Bpmn.readModelFromStream(input));
            }
        });
    }
}
