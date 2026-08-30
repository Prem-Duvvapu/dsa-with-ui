package com.dsa.ui.tracer;

import com.dsa.ui.model.DsType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TracerRegistryTest {

    @Test
    void rejectsATracerWithoutAKnownDsTypeAtStartup() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new TracerRegistry(List.of(new StubTracer(null))));

        assertTrue(error.getMessage().contains("unknown dsType"));
    }

    private record StubTracer(DsType dsType) implements AlgorithmTracer {
        @Override public String id() { return "stub"; }
        @Override public InputSpec inputSpec() { return InputSpec.of(); }
        @Override public Map<String, Object> alternateInput() { return Map.of(); }
        @Override public String annotatedCode() { return "// @a done\nreturn;"; }
        @Override public void run(Inputs in, StepEmitter emit) {
            emit.at("done").say("done").step();
        }
    }
}
