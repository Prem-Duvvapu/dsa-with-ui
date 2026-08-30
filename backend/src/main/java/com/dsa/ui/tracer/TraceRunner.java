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
        StepEmitter emitter = new StepEmitter(
                code, spec.getMaxSteps(), spec.getMaxBytes(), tracer.dsType());

        boolean truncated = false;
        String truncationReason = null;
        try {
            tracer.run(inputs, emitter);
        } catch (TraceBudgetExceededException e) {
            // Either ceiling reports through the same flag. A second boolean would mean
            // every caller had to learn there are two ways for a trace to be incomplete.
            truncated = true;
            truncationReason = e.getReason();
        }

        return new ExecutionTrace(
                tracer.id(),
                emitter.collected(),
                truncated,
                truncationReason,
                spec.getMaxSteps(),
                spec.getMaxBytes(),
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
