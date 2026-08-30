package com.dsa.ui.tracer;

import com.dsa.ui.model.DsType;

import java.util.Map;

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

    /** The only visualization type this tracer may emit. */
    DsType dsType();

    /** The inputs this algorithm accepts, their bounds, and their defaults. */
    InputSpec inputSpec();

    /**
     * A second input, materially different from the spec's defaults, used by the contract
     * test to prove this tracer reads its input rather than replaying a fixed narration.
     *
     * <p>Deliberately abstract. It lived in a map inside the test class, which meant every
     * new tracer had to edit one central file — unworkable at 433 and a permanent source of
     * merge conflicts. More importantly, a tracer must not be able to opt out of the check
     * by staying silent, so there is no default implementation.
     *
     * <p>"Materially different" means a different answer or a different branch profile, not
     * a permutation. A reordered array produces a different fingerprint while proving
     * nothing about whether the algorithm ran.
     */
    Map<String, Object> alternateInput();

    /**
     * The Java source shown beside the animation, carrying {@code // @a name} anchors.
     * Every anchor {@link #run} emits must appear here.
     */
    String annotatedCode();

    /**
     * Runs the algorithm against validated input, emitting a step at each meaningful
     * state change. Implementations should let {@link TraceBudgetExceededException}
     * propagate; {@link TraceRunner} turns it into a truncated trace.
     */
    void run(Inputs in, StepEmitter emit);
}
