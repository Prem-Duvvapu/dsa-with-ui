package com.dsa.ui.tracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(TraceRunner.class);

    /**
     * How long a trace may take before it is worth hearing about.
     *
     * <p>/execute is the only endpoint that runs real algorithms, so it is the only place
     * where a slow problem can quietly become a slow service. There was no logging, no
     * timing and no metrics anywhere: when something was slow in production there was
     * nothing to look at. This is the minimum that fixes that — every run is counted at
     * DEBUG, and anything past this threshold is reported at WARN with the id, so the
     * problem names itself.
     */
    private static final long SLOW_TRACE_MILLIS = 250;

    public ExecutionTrace run(AlgorithmTracer tracer, Map<String, Object> suppliedInput) {
        long startedAt = System.nanoTime();
        ExecutionTrace trace = execute(tracer, suppliedInput);
        long millis = (System.nanoTime() - startedAt) / 1_000_000;

        if (millis >= SLOW_TRACE_MILLIS) {
            log.warn("slow trace id={} steps={} truncated={} millis={}",
                    tracer.id(), trace.getSteps().size(), trace.isTruncated(), millis);
        } else if (log.isDebugEnabled()) {
            log.debug("trace id={} steps={} truncated={} millis={}",
                    tracer.id(), trace.getSteps().size(), trace.isTruncated(), millis);
        }
        // Truncation is worth a line at any speed: the caller got an incomplete animation
        // and the step budget is the only thing standing between /execute and a hang.
        if (trace.isTruncated()) {
            log.warn("trace truncated id={} reason={} maxSteps={}",
                    tracer.id(), trace.getTruncationReason(), trace.getMaxSteps());
        }
        return trace;
    }

    private ExecutionTrace execute(AlgorithmTracer tracer, Map<String, Object> suppliedInput) {
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
