package com.dsa.ui.catalog;

import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.tracer.InputSpec;

/**
 * A catalogue listing: the problem plus whether it can actually be run.
 *
 * <p>{@code traced} is the honesty flag. The catalogue lists every problem the project
 * intends to cover, but only some have a real {@link com.dsa.ui.tracer.AlgorithmTracer}.
 * Surfacing the difference lets the UI say "not yet traced" instead of animating a
 * different algorithm, and gives the migration a progress metric that cannot be faked.
 */
public final class CatalogEntry {

    private final ProblemDetail problem;
    private final boolean traced;
    private final InputSpec inputSpec;

    public CatalogEntry(ProblemDetail problem, boolean traced, InputSpec inputSpec) {
        this.problem = problem;
        this.traced = traced;
        this.inputSpec = inputSpec;
    }

    public ProblemDetail getProblem() { return problem; }

    public boolean isTraced() { return traced; }

    /** Null for untraced problems, which have no input contract yet. */
    public InputSpec getInputSpec() { return inputSpec; }

    public String getId() { return problem.getId(); }
}
