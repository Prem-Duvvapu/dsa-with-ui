package com.dsa.ui.tracer;

/**
 * Thrown to unwind a running algorithm once its trace hits one of the spec's ceilings.
 *
 * <p>Control flow, not a fault. {@link TraceRunner} catches it and returns the steps
 * gathered so far marked {@code truncated}, carrying {@link #getReason()} so the caller can
 * say which ceiling stopped it. Stopping the algorithm matters: a caller can ask N-Queens
 * for n=12 or permutations for n=10, and simply declining to record further steps would
 * leave the CPU burning through a factorial search nobody will ever see.
 *
 * <p>There are two ceilings because they bound different things. The step budget bounds
 * CPU. It does not bound bytes: every step carries a snapshot of the data structure, so a
 * response grows as steps x n, and 5000 steps of a 40-element array is roughly 11 MB of
 * JSON for one click.
 */
public class TraceBudgetExceededException extends RuntimeException {

    private final String reason;

    private TraceBudgetExceededException(String message, String reason) {
        super(message);
        this.reason = reason;
        // No stack trace: this fires on a hot path and is never a debugging signal.
        super.fillInStackTrace();
    }

    public static TraceBudgetExceededException steps(int limit) {
        return new TraceBudgetExceededException(
                "Trace exceeded its " + limit + "-step budget",
                "Stopped at the " + limit + "-step limit. Try a smaller input.");
    }

    public static TraceBudgetExceededException bytes(long limit, int stepsSoFar) {
        return new TraceBudgetExceededException(
                "Trace exceeded its " + limit + "-byte budget after " + stepsSoFar + " steps",
                "Stopped after " + stepsSoFar + " steps at the "
                        + (limit / 1000) + " KB response limit. Try a smaller input.");
    }

    /** Plain-language explanation, safe to show a user. */
    public String getReason() {
        return reason;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
