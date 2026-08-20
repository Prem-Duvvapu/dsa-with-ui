package com.dsa.ui.tracer;

/**
 * Thrown to unwind a running algorithm once its trace hits the spec's ceiling.
 *
 * <p>Control flow, not a fault. {@link TraceRunner} catches it and returns the steps
 * gathered so far marked {@code truncated}. Stopping the algorithm matters: a caller can
 * ask N-Queens for n=12 or permutations for n=10, and simply declining to record further
 * steps would leave the CPU burning through a factorial search nobody will ever see.
 */
public class StepBudgetExceededException extends RuntimeException {

    public StepBudgetExceededException(int limit) {
        super("Trace exceeded its " + limit + "-step budget");
        // No stack trace: this fires on a hot path and is never a debugging signal.
        super.fillInStackTrace();
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
