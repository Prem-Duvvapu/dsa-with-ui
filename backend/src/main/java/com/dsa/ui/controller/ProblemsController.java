package com.dsa.ui.controller;

import com.dsa.ui.catalog.CatalogEntry;
import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.tracer.AlgorithmTracer;
import com.dsa.ui.tracer.ExecutionTrace;
import com.dsa.ui.tracer.wire.TraceResponse;
import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.TraceRunner;
import com.dsa.ui.tracer.TracerRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The v2 API: one catalogue, and problems you can run against your own input.
 *
 * <p>Replaces the eighteen-endpoint fan-out. The frontend previously had to know every
 * base path and stitch the catalogues together, which is how two endpoints spent months
 * pointing at URLs that did not exist.
 *
 * <p>The legacy per-topic controllers still serve their old paths so the frontend can
 * migrate independently; they are removed once it has.
 */
@RestController
@RequestMapping("/api/problems")
public class ProblemsController {

    private final ProblemCatalog catalog;
    private final TracerRegistry tracers;
    private final TraceRunner runner;

    public ProblemsController(ProblemCatalog catalog, TracerRegistry tracers, TraceRunner runner) {
        this.catalog = catalog;
        this.tracers = tracers;
        this.runner = runner;
    }

    /** The whole catalogue as lightweight summaries — full detail is one request away. */
    @GetMapping
    public List<Map<String, Object>> list() {
        return catalog.all().stream().map(ProblemsController::summarize).toList();
    }

    /**
     * Coverage, straight from the code. The README has historically quoted four
     * different catalogue sizes, none matching the source; this is the number that
     * cannot drift.
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("catalogued", catalog.size());
        out.put("traced", catalog.tracedCount());
        out.put("untraced", catalog.size() - catalog.tracedCount());
        out.put("duplicateIds", catalog.getDuplicateIds());
        out.put("orphanedTracerIds", catalog.getOrphanedTracerIds());
        return out;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        CatalogEntry entry = require(id);
        ProblemDetail p = entry.getProblem();

        Map<String, Object> out = new LinkedHashMap<>(summarize(entry));
        out.put("description", p.getDescription());
        out.put("complexity", p.getComplexity());
        out.put("defaultArray", p.getDefaultArray());
        out.put("defaultGrid", p.getDefaultGrid());
        out.put("defaultGraphNodes", p.getDefaultGraphNodes());
        out.put("defaultGraphEdges", p.getDefaultGraphEdges());
        out.put("defaultTreeNodes", p.getDefaultTreeNodes());
        out.put("defaultList", p.getDefaultList());
        out.put("defaultTrie", p.getDefaultTrie());

        // A traced problem's source comes from its tracer, with anchors stripped, so the
        // code on screen is provably the code the highlighted lines refer to.
        tracers.find(id).ifPresentOrElse(
                t -> out.put("javaCode",
                        com.dsa.ui.tracer.AnnotatedCode.parse(t.annotatedCode()).getDisplayCode()),
                () -> out.put("javaCode", p.getJavaCode()));

        return out;
    }

    /**
     * Runs the problem against its declared default input.
     *
     * <p>{@code ?encoding=full} returns the pre-delta shape, where every step is a complete
     * snapshot. Kept for one migration so the frontend can move on its own schedule.
     */
    @GetMapping("/{id}/execute")
    public TraceResponse executeDefaults(@PathVariable String id,
                                         @RequestParam(required = false) String encoding) {
        return TraceResponse.of(runner.runDefaults(tracer(id)), encoding);
    }

    /** Runs the problem against caller-supplied input. */
    @PostMapping("/{id}/execute")
    public TraceResponse execute(@PathVariable String id,
                                 @RequestBody(required = false) Map<String, Object> input,
                                 @RequestParam(required = false) String encoding) {
        return TraceResponse.of(runner.run(tracer(id), input == null ? Map.of() : input), encoding);
    }

    /** The input contract on its own, for a client that wants to build a form first. */
    @GetMapping("/{id}/input-spec")
    public InputSpec inputSpec(@PathVariable String id) {
        return tracer(id).inputSpec();
    }

    private static Map<String, Object> summarize(CatalogEntry entry) {
        ProblemDetail p = entry.getProblem();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", p.getId());
        out.put("title", p.getTitle());
        out.put("category", p.getCategory());
        out.put("striverSheetSection", p.getStriverSheetSection());
        out.put("difficulty", p.getDifficulty());
        out.put("dsType", p.getDsType());
        out.put("traced", entry.isTraced());
        out.put("inputSpec", entry.getInputSpec());
        return out;
    }

    private CatalogEntry require(String id) {
        return catalog.find(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No problem with id '" + id + "'"));
    }

    /**
     * 404 when the problem does not exist, 501 when it exists but has no tracer yet.
     * Keeping those distinct is the point: the UI can say "not yet traced" honestly
     * instead of showing an unrelated algorithm, which is what the old fallback did.
     */
    private AlgorithmTracer tracer(String id) {
        require(id);
        return tracers.find(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "'" + id + "' is in the catalogue but has no execution trace yet"));
    }
}
