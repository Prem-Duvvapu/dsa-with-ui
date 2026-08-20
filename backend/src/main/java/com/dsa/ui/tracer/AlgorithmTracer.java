package com.dsa.ui.tracer;

/**
 * One problem, traced by actually running it.
 *
 * <p>This replaces the previous arrangement, where each service dispatched on a giant
 * {@code switch (problemId)} whose {@code default:} returned some other problem's steps.
 * That made an unimplemented problem indistinguishable from a working one — 303 of 440
 * catalogue entries played an unrelated animation. A tracer either exists and runs the
 * real algorithm, or it does not exist and the API answers 404.
 *
 * <p>Implementations are Spring beans, discovered by {@link TracerRegistry}. They must be
 * stateless: one instance serves every concurrent request, so all working state belongs
 * in local variables inside {@link #run}.
 */
public interface AlgorithmTracer {

    /** Must match a catalogue {@code ProblemDetail} id. */
    String id();

    /** The inputs this algorithm accepts, their bounds, and their defaults. */
    InputSpec inputSpec();

    /**
     * The Java source shown beside the animation, carrying {@code // @a name} anchors.
     * Every anchor {@link #run} emits must appear here.
     */
    String annotatedCode();

    /**
     * Runs the algorithm against validated input, emitting a step at each meaningful
     * state change. Implementations should let {@link StepBudgetExceededException}
     * propagate; {@link TraceRunner} turns it into a truncated trace.
     */
    void run(Inputs in, StepEmitter emit);
}
