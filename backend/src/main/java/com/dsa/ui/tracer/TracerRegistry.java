package com.dsa.ui.tracer;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Every {@link AlgorithmTracer} in the application, indexed by problem id.
 *
 * <p>Deliberately has no fallback. Lookup returns empty for an unregistered id and the
 * API answers 404 — which is the whole point of the rewrite. The previous per-service
 * {@code switch} statements ended in {@code default: return generateSomethingElse()},
 * so a missing implementation looked exactly like a working one.
 *
 * <p>Duplicate ids fail application startup rather than letting bean ordering decide
 * which implementation wins.
 */
@Component
public class TracerRegistry {

    private final Map<String, AlgorithmTracer> byId = new TreeMap<>();

    public TracerRegistry(List<AlgorithmTracer> tracers) {
        Map<String, String> owners = new LinkedHashMap<>();
        for (AlgorithmTracer tracer : tracers) {
            String id = tracer.id();
            if (id == null || id.isBlank()) {
                throw new IllegalStateException(
                        tracer.getClass().getName() + " declares a blank problem id");
            }
            if (tracer.dsType() == null) {
                throw new IllegalStateException(
                        tracer.getClass().getName() + " declares an unknown dsType");
            }
            String existing = owners.get(id);
            if (existing != null) {
                throw new IllegalStateException("Two tracers claim problem id '" + id + "': "
                        + existing + " and " + tracer.getClass().getName());
            }
            owners.put(id, tracer.getClass().getName());
            byId.put(id, tracer);
        }
    }

    public Optional<AlgorithmTracer> find(String problemId) {
        return Optional.ofNullable(byId.get(problemId));
    }

    public boolean isTraced(String problemId) {
        return byId.containsKey(problemId);
    }

    public Set<String> tracedIds() {
        return byId.keySet();
    }

    public List<AlgorithmTracer> all() {
        return List.copyOf(byId.values());
    }

    public int size() {
        return byId.size();
    }
}
