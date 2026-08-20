package com.dsa.ui.tracer;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validates input, runs a tracer, and packages the result.
 *
 * <p>Parsing the annotated source on every request keeps tracers free to build their
 * code string dynamically; it is a few microseconds against traces that take orders of
 * magnitude longer to generate.
 */
@Component
public class TraceRunner {

    public ExecutionTrace run(AlgorithmTracer tracer, Map<String, Object> suppliedInput) {
        InputSpec spec = tracer.inputSpec();
        Inputs inputs = InputValidator.validate(spec, suppliedInput);

        AnnotatedCode code = AnnotatedCode.parse(tracer.annotatedCode());
        StepEmitter emitter = new StepEmitter(code, spec.getMaxSteps());

        boolean truncated = false;
        try {
            tracer.run(inputs, emitter);
        } catch (StepBudgetExceededException e) {
            truncated = true;
        }

        return new ExecutionTrace(
                tracer.id(),
                emitter.collected(),
                truncated,
                spec.getMaxSteps(),
                code.getDisplayCode(),
                code.getAnchors(),
                inputs.asMap()
        );
    }

    /** Runs against the spec's declared defaults. */
    public ExecutionTrace runDefaults(AlgorithmTracer tracer) {
        return run(tracer, Map.of());
    }
}
