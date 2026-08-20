package com.dsa.ui.catalog;

import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.tracer.AlgorithmTracer;
import com.dsa.ui.tracer.TracerRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The whole catalogue in one place, merged from every {@link ProblemProvider}.
 *
 * <p>Previously the frontend fanned out to eighteen separate endpoints and stitched the
 * results together itself, which is how two of them went unnoticed for pointing at URLs
 * the backend never served. One source of truth removes that class of bug.
 *
 * <p>Ids are global here, so the cross-service collisions the per-service view hid —
 * {@code flood-fill}, {@code single-number}, {@code merge-intervals} and others exist in
 * two services with different content — become visible. The first provider to claim an
 * id wins and the collision is recorded in {@link #getDuplicateIds()} rather than
 * failing startup, because resolving them means moving problems between services, which
 * is Phase 4 work.
 */
@Component
public class ProblemCatalog {

    private final Map<String, CatalogEntry> entries = new LinkedHashMap<>();
    private final Map<String, List<String>> duplicateIds = new LinkedHashMap<>();
    private final TracerRegistry tracers;

    public ProblemCatalog(List<ProblemProvider> providers, TracerRegistry tracers) {
        this.tracers = tracers;

        for (ProblemProvider provider : providers) {
            String providerName = provider.getClass().getSimpleName();
            for (ProblemDetail problem : provider.getAllProblems()) {
                String id = problem.getId();
                if (entries.containsKey(id)) {
                    duplicateIds.computeIfAbsent(id, k -> new ArrayList<>()).add(providerName);
                    continue;
                }
                Optional<AlgorithmTracer> tracer = tracers.find(id);
                entries.put(id, new CatalogEntry(
                        problem,
                        tracer.isPresent(),
                        tracer.map(AlgorithmTracer::inputSpec).orElse(null)));
            }
        }
    }

    public List<CatalogEntry> all() {
        return List.copyOf(entries.values());
    }

    public Optional<CatalogEntry> find(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    public int size() {
        return entries.size();
    }

    public long tracedCount() {
        return entries.values().stream().filter(CatalogEntry::isTraced).count();
    }

    /**
     * Ids claimed by more than one provider, mapped to the losing providers.
     * Empty is the goal; non-empty is tracked work, not a crash.
     */
    public Map<String, List<String>> getDuplicateIds() {
        return Map.copyOf(duplicateIds);
    }

    /**
     * Tracers with no matching catalogue entry. Always a mistake — the problem would be
     * runnable but invisible, since nothing would ever list it.
     */
    public List<String> getOrphanedTracerIds() {
        return tracers.tracedIds().stream()
                .filter(id -> !entries.containsKey(id))
                .toList();
    }
}
