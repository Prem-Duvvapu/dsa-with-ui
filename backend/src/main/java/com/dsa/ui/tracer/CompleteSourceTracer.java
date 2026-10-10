package com.dsa.ui.tracer;

/** A tracer owning one standalone learner-facing Java solution, never a fallback sketch. */
public abstract class CompleteSourceTracer implements AlgorithmTracer {
    private volatile String source;

    public final String sourceResourcePath() {
        return "/solutions/core/" + id() + ".java";
    }

    @Override
    public final String annotatedCode() {
        String loaded = source;
        if (loaded == null) {
            loaded = SolutionSource.read(sourceResourcePath());
            source = loaded;
        }
        return loaded;
    }
}
