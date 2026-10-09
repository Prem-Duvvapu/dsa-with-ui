package com.dsa.ui.controller;

import com.dsa.ui.catalog.CatalogEntry;
import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.catalog.StatementCatalog;
import com.dsa.ui.approach.SolutionApproach;
import com.dsa.ui.approach.SolutionApproachRegistry;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.tracer.AlgorithmTracer;
import com.dsa.ui.tracer.ExecutionTrace;
import com.dsa.ui.tracer.wire.TraceResponse;
import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.TraceRunner;
import com.dsa.ui.tracer.TracerRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final StatementCatalog statements;
    private final SolutionApproachRegistry approaches;

    public ProblemsController(ProblemCatalog catalog, TracerRegistry tracers, TraceRunner runner,
                              StatementCatalog statements, SolutionApproachRegistry approaches) {
        this.catalog = catalog;
        this.tracers = tracers;
        this.runner = runner;
        this.statements = statements;
        this.approaches = approaches;
    }

    /**
     * The whole catalogue as lightweight summaries — full detail is one request away.
     *
     * <p>Computed once. The catalogue is assembled at startup from eighteen providers and
     * never changes afterwards, so re-projecting 433 maps on every request was work with no
     * possible different answer. Held in a volatile field rather than synchronized: two
     * threads racing on first call both build the same immutable list, and the loser's copy
     * is simply discarded.
     */
    private volatile List<Map<String, Object>> cachedSummaries;

    /**
     * The ETag for that projection, computed once with it.
     *
     * <p>Spring's {@code ShallowEtagHeaderFilter} was the obvious way to do this and it is
     * the wrong one here: it buffers the response and sets Content-Length itself, which
     * stops Tomcat compressing at all. Measured - the catalogue came back 236 KB with the
     * filter in place no matter what Accept-Encoding asked for. Hashing the immutable
     * projection once and answering If-None-Match here keeps compression working on the
     * 200s, and is cheaper besides: the filter re-hashed the body on every request.
     */
    private volatile String cachedEtag;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        List<Map<String, Object>> summaries = cachedSummaries;
        if (summaries == null) {
            // Unmodifiable because this list is now shared across every request. summarize()
            // hands back a mutable LinkedHashMap, and one caller mutating an entry would
            // corrupt the catalogue for everyone afterwards. Collections.unmodifiableMap
            // rather than Map.copyOf: a summary legitimately holds nulls (inputSpec is null
            // for an untraced problem) and Map.copyOf rejects them.
            summaries = catalog.all().stream()
                    .map(ProblemsController::summarize)
                    .map(Collections::unmodifiableMap)
                    .toList();
            cachedSummaries = summaries;
            cachedEtag = etagFor(summaries);
        }
        String etag = cachedEtag;

        if (etag != null && etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok()
                // no-cache + must-revalidate, not a max-age: the catalogue changes only on
                // deploy, but when it does a client holding a stale copy must find out at
                // once rather than after an arbitrary window.
                .cacheControl(CacheControl.noCache().mustRevalidate())
                .eTag(etag)
                .body(summaries);
    }

    /**
     * A stable hash of the rendered catalogue, so a deploy that changes it changes this.
     *
     * <p><strong>Weak</strong> (the {@code W/} prefix), and that is load-bearing rather than
     * stylistic. Tomcat refuses to compress a response carrying a STRONG ETag, because a
     * strong tag identifies an exact byte sequence and gzip changes those bytes. With a
     * strong tag this endpoint came back 236 KB uncompressed while every other endpoint
     * compressed normally - the one response that most needed it was the only one excluded.
     * A weak tag says "semantically the same catalogue", which is all revalidation needs
     * and which permits the transformation.
     */
    private static String etagFor(List<Map<String, Object>> summaries) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(new ObjectMapper().writeValueAsBytes(summaries));
            StringBuilder hex = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return "W/\"" + hex + "\"";
        } catch (Exception e) {
            // No ETag is correctness-preserving: the client simply re-downloads.
            return null;
        }
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
    public Map<String, Object> detail(@PathVariable String id,
                                     @RequestParam(required = false) String approach) {
        CatalogEntry entry = require(id);
        ProblemDetail p = entry.getProblem();
        SolutionApproach selected = entry.isTraced() ? selected(id, approach) : null;
        // Preserve 501 for an explicit approach on an untraced problem.
        if (selected == null && approach != null) selected(id, approach);

        Map<String, Object> out = new LinkedHashMap<>(summarize(entry));
        out.put("description", p.getDescription());
        // The SOURCE problem's constraints. Distinct from the inputSpec field constraints
        // below, which are this visualiser's own caps; the UI labels them apart.
        out.put("constraints", p.getConstraints());
        // The full statement, when one has been written: own-words paragraphs, examples whose
        // outputs StatementContractTest proves against the tracer, the source's constraints
        // (which then replace the shorter ProblemConstraints list) and links to the original.
        statements.find(id).ifPresent(st -> {
            out.put("statement", st.statement());
            out.put("examples", st.examples().stream().map(e -> {
                Map<String, Object> example = new LinkedHashMap<>();
                example.put("input", e.input());
                example.put("output", e.output());
                example.put("explanation", e.explanation());
                return example;
            }).toList());
            if (st.constraints() != null && !st.constraints().isEmpty()) out.put("constraints", st.constraints());
            out.put("sources", st.sources());
        });
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
        if (selected != null) {
            var t = selected.tracer();
            out.put("javaCode", com.dsa.ui.tracer.AnnotatedCode.parse(t.annotatedCode()).getDisplayCode());
            // An approach's alternate exercises its own branches, not another solution's.
            out.put("alternateInput", t.alternateInput());
            out.put("inputSpec", t.inputSpec());
            out.put("dsType", t.dsType());
            out.put("complexity", selected.complexity());
            out.put("approachId", selected.id());
            out.put("teaching", selected.teaching());
            out.put("defaultApproachId", approaches.resolve(id, null).id());
            out.put("approaches", approaches.available(id).stream().map(SolutionApproach::summaryView).toList());
        } else {
            out.put("javaCode", p.getJavaCode());
            out.put("alternateInput", null);
            out.put("defaultApproachId", null);
            out.put("approaches", List.of());
        }

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
                                         @RequestParam(required = false) String encoding,
                                         @RequestParam(required = false) String approach) {
        // Cached, because this is the hot path and it is deterministic. Most traffic is
        // "open a problem, press play", which re-ran the same 431 algorithms over and over
        // to produce byte-identical answers. A custom input still runs for real - see the
        // POST below, which is deliberately NOT cached.
        //
        // TraceResponse is safe to share: its fields are final and nothing
        // mutates one after construction.
        SolutionApproach selected = selected(id, approach);
        String effectiveEncoding = effectiveEncoding(encoding);
        return defaultTraces.computeIfAbsent(new DefaultTraceKey(id, selected.id(), effectiveEncoding),
                key -> TraceResponse.of(runner.runDefaults(selected.tracer()), effectiveEncoding, selected));
    }

    /**
     * Default traces, keyed by validated problem, approach and effective encoding.
     *
     * <p>Bounded by registered approach pairs times the two supported encodings. Null and
     * case aliases normalize before lookup; arbitrary caller strings cannot create keys.
     *
     * <p>Never populated from {@code POST /execute}. A cache keyed on caller-supplied input
     * is a memory-exhaustion vector, and the rate limiter exists precisely because that path
     * has to do real work every time.
     */
    private record DefaultTraceKey(String problemId, String approachId, String encoding) {}
    private final Map<DefaultTraceKey, TraceResponse> defaultTraces = new ConcurrentHashMap<>();

    /** Runs the problem against caller-supplied input. */
    @PostMapping("/{id}/execute")
    public TraceResponse execute(@PathVariable String id,
                                 @RequestBody(required = false) Map<String, Object> input,
                                 @RequestParam(required = false) String encoding,
                                 @RequestParam(required = false) String approach) {
        SolutionApproach selected = selected(id, approach);
        String effectiveEncoding = effectiveEncoding(encoding);
        return TraceResponse.of(runner.run(selected.tracer(), input == null ? Map.of() : input),
                effectiveEncoding, selected);
    }

    /** The input contract on its own, for a client that wants to build a form first. */
    @GetMapping("/{id}/input-spec")
    public InputSpec inputSpec(@PathVariable String id, @RequestParam(required = false) String approach) {
        return selected(id, approach).tracer().inputSpec();
    }

    private SolutionApproach selected(String id, String approach) {
        tracer(id); // Unknown problem is 404; an untraced problem is 501, before approach lookup.
        return approaches.resolve(id, approach);
    }

    private static String effectiveEncoding(String encoding) {
        if (encoding == null || TraceResponse.DELTA.equalsIgnoreCase(encoding)) return TraceResponse.DELTA;
        if (TraceResponse.FULL.equalsIgnoreCase(encoding)) return TraceResponse.FULL;
        throw new UnsupportedEncodingException(encoding);
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
