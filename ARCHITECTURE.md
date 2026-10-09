# Architecture

> **Every number here is read from the running system, not from another document.**
> Regenerate them with `curl -s localhost:8923/api/problems/stats`. If a figure below
> disagrees with that endpoint, the endpoint is right.
>
> Verified against the local API 2026-10-09: **431 problems, 431 traced, 0 untraced,
> 0 duplicate ids, no orphaned tracers.** Renderer counts below describe canonical
> catalogue entries; an explicitly selected solution approach can use another renderer.

## The shape of it

One Spring Boot service, one React bundle, no database. Everything served is computed from
code: the catalogue is built at startup from eighteen provider classes, and every animation
is produced by running the real algorithm on demand.

```mermaid
flowchart TB
    subgraph browser["Browser — React 18 + Vite"]
        Router["AppRouter<br/>/problem/:id"]
        App["ProblemWorkspace<br/>Playground · Code · Analysis"]
        Hooks["useProblemSession · useTrace<br/>draft · atomic run · sharing · playback"]
        Registry["canvas/registry.js<br/>dsType → renderer"]
        Canvas["16 renderer components/variants"]
        Code["CodeViewer<br/>highlights the active line"]
    end

    subgraph api["Spring Boot — :8923"]
        PC["ProblemsController<br/>/api/problems"]
        Cat["ProblemCatalog<br/>merges 18 providers"]
        Reg["SolutionApproachRegistry + TracerRegistry<br/>selected executable · canonical tracers"]
        Run["TraceRunner<br/>+ InputValidator"]
    end

    subgraph data["Data, all in code"]
        Svc["18 *Service classes<br/>431 ProblemDetail"]
        Tr["tracer/impl<br/>431 AlgorithmTracer"]
    end

    Router --> App --> Hooks -->|"GET /api/problems<br/>POST /{id}/execute"| PC
    App --> Registry --> Canvas
    App --> Code
    PC --> Cat --> Svc
    PC --> Run --> Reg --> Tr
```

**There is no persistence layer, and none is needed.** No user data, no writes, no sessions.
Persistent per-user preferences/progress/presets live in browser storage, with denied/
corrupt storage handled by adapters. The URL identifies the problem, successful run's
input/approach, step and view. Unsaved drafts and the current run belong to the mounted
problem session; they are not server-side sessions or database records.

## The request that matters

Both `GET` and `POST /api/problems/{id}/execute` can run real algorithms. GET uses declared
defaults and a bounded default-trace cache; POST runs caller input and is not cached.
Both use the same rate limit, validation, selected tracer and trace budgets.

```mermaid
sequenceDiagram
    participant U as Browser
    participant F as ExecuteRateLimitFilter
    participant C as ProblemsController
    participant V as InputValidator
    participant T as AlgorithmTracer
    participant E as StepEmitter

    U->>F: POST /{id}/execute
    F->>F: token bucket, 60/min/client
    Note over F: 429 + Retry-After past that
    F->>C: allowed
    C->>C: resolve canonical problem + requested approach
    Note over C: 404 unknown · 501 catalogued but untraced<br/>never another problem's steps
    C->>V: validate against InputSpec
    Note over V: unknown fields rejected, not ignored<br/>every bound enforced, all errors in one 400
    V->>T: run(Inputs, StepEmitter)
    T->>E: emit.at("anchor").say(...).step()
    Note over E: 5000-step budget → truncated: true
    E-->>U: TraceResponse — approach, source, type, complexity, steps, anchors, resolvedInput
```

Two caps are mandatory rather than optional, and both exist because a caller setting `n = 20`
on a factorial-time problem could otherwise take the server down: a **per-field size ceiling**
in the `InputSpec`, and global trace budgets (normally5000 steps and2,000,000 JSON bytes).
New exponential approaches have separately measured input caps; canonical caps are not
permission to advertise unrestricted recursion. Unknown approach/encoding requests are400.

## The tracer contract

The load-bearing idea. A tracer cannot be written without declaring how it is to be checked.

```java
public interface AlgorithmTracer {
    String id();                              // "kadane-algo"
    DsType dsType();                          // closed canvas vocabulary
    InputSpec inputSpec();                    // declared inputs, bounds, defaults
    Map<String, Object> alternateInput();     // a MATERIALLY different input
    String annotatedCode();                   // Java source carrying // @a anchors
    void run(Inputs in, StepEmitter emit);    // executes the algorithm for real
}
```

`alternateInput()` is abstract so it cannot be skipped, and it is what makes
`traceRespondsToItsInput` possible: run every tracer on two different inputs and fail if the
traces match. **A canned narration cannot survive that**, which is the whole point — the
original incident was 303 problems returning another algorithm's trace under 90 green tests
whose only per-problem assertion was `!steps.isEmpty()`.

`annotatedCode()` carries `// @a name` markers rather than line numbers, because a line
number drifts the moment the displayed code changes. `AnnotatedCode` resolves names to
numbers, strips the markers, and rejects duplicate, unnamed or dangling anchors.

## Routing a trace to a picture

`dsType` is a closed backend enum. It selects the canvas, and a contract test on each side
stops the two tiers drifting.

```mermaid
flowchart LR
    Tracer["AlgorithmTracer<br/>dsType()"] --> Enum["DsType<br/>17 values"]
    Enum --> Wire["contracts/ds-types.json"]
    Wire --> Reg["CANVAS_BY_DSTYPE"]
    Reg --> Canvases["16 renderer components/variants"]

    Enum -.->|"DsTypePayloadContractTest<br/>the promised field is really emitted"| Tracer
    Wire -.->|"registry.test.js<br/>every value has a renderer"| Reg
```

| dsType | n | Canvas | What the picture is *for* |
|---|---:|---|---|
| `Array` | 72 | ArrayCanvas | values and pointers |
| `DpTable` | 54 | DpTableCanvas | the table filling, with arrows from each dependency |
| `Tree` | 54 | TreeCanvas | structure and traversal order |
| `Graph` | 40 | GraphCanvas | topology and frontier |
| `LinkedList` | 36 | LinkedListCanvas | nodes, next/child/random links |
| `Matrix` | 30 | GridCanvas | the board |
| `SearchSpace` | 27 | SearchSpaceCanvas | the space **halving** — index or answer space |
| `String` | 26 | StringCanvas | characters and pointers |
| `Stack` | 25 | StackCanvas | what is on the stack |
| `RecursionTree` | 19 | RecursionTreeCanvas | the tree explored, rebuilt from `callStack` |
| `PriorityQueue` | 12 | HeapCanvas | the heap as **tree and array at once** |
| `Bits` | 12 | ArrayCanvas | bit positions |
| `Window` | 12 | WindowCanvas | the window **moving and stretching** |
| `Queue` | 4 | QueueHeroCanvas | what is queued |
| `Dsu` | 3 | DsuCanvas | parent/rank tables and components |
| `Interval` | 3 | IntervalCanvas | spans on a timeline |
| `Trie` | 2 | TrieCanvas | the prefix tree |

Several canvases **derive** what a tracer did not emit, rather than demanding tracer changes:
`RecursionTreeCanvas` rebuilds the tree from call stacks, `HeapCanvas` derives whichever of
the tree or array is missing, `WindowCanvas` finds the window from cell states. Derivation is
preferred because it cannot drift from the trace — it is computed from it.

## What holds it together

The rule this codebase is organised around: **no fallback, anywhere.** An unknown id is 404,
a catalogued-but-untraced one is 501, an unknown `dsType` renders an explicit unsupported
state, and a canvas with no data says so rather than inventing a plausible default.

```mermaid
flowchart TB
    subgraph cross["Cross-tier contracts — neither side can drift alone"]
        A["contracts/ds-types.json"]
        B["contracts/categories.json"]
    end
    subgraph back["Backend guards"]
        C["TracerContractTest<br/>anchors · distinctness · input response · call-stack drain"]
        D["DsTypePayloadContractTest<br/>the declared canvas gets its field"]
        E["GoldenTraceTest<br/>431 pinned traces"]
        F["DetailResponseContractTest<br/>every model field reaches the wire"]
        G["DuplicateProblemTest<br/>no id twice, no word-order twin"]
    end
    subgraph front["Frontend guards"]
        H["designTokens.test.js<br/>every var() defined, 4.5:1 in BOTH themes"]
        I["registry.test.js · Sidebar.categories.test.js"]
    end
    A --- I
    B --- I
```

Golden files pin trace **content** — the descriptions, variables and highlighted lines —
which every other test is blind to. Regenerating one without reading the diff records a bug
as expected; see `CLAUDE.md`.

## Scale

The bottleneck is CPU, not storage, because `/execute` runs real algorithms.

- `/api/problems` is served from a cached projection with a **weak** ETag and gzip:
  236 KB → 31 KB, then 304 on revalidation. (The ETag must stay weak — Tomcat refuses to
  compress a response carrying a strong one. `HttpCachingTest` pins this.)
- `/execute` is capped per client at 60/min, per request at 5000 steps.
- The app is stateless, so it scales horizontally. **One caveat:** rate-limit buckets are
  in-memory, so N instances enforce N × the limit. That is the first thing needing Redis.

Default traces are cached by validated `(problemId, approachId, effectiveEncoding)`;
encoding aliases cannot add arbitrary cache keys. Custom inputs are never cached.
Measured decode/render/play/seek/history performance and final release observability
remain acceptance work; caching alone is not proof of fast interactions.

## Where things live

| Path | What |
|---|---|
| `backend/src/main/java/com/dsa/ui/tracer/` | the contract, the runner, the validator |
| `backend/src/main/java/com/dsa/ui/tracer/impl/` | canonical tracer implementations |
| `backend/src/main/java/com/dsa/ui/approach/` | registered real alternatives, teaching and selected-executable identity |
| `backend/src/main/java/com/dsa/ui/service/` | 18 catalogue providers — **not dead code**, they hold every `ProblemDetail` |
| `backend/src/main/java/com/dsa/ui/catalog/` | `ProblemCatalog`, `ProblemConstraints` |
| `backend/src/test/resources/golden/` | 431 pinned traces |
| `frontend/src/canvas/registry.js` | the only dsType→renderer table |
| `frontend/src/workspace/` | the live problem page, views, approach selection and comparison |
| `frontend/src/hooks/useProblemSession.js` | session-owned draft, restoration and successful-run sharing |
| `frontend/src/trace/` | delta decoding, recursion-tree and heap derivation |
| `contracts/` | the two cross-tier fixtures |

## Related reading

`CLAUDE.md` — working rules and the tracer how-to. `RCA.md` — incident ledger; read it
before changing an affected subsystem. `AUDIT.md` — the full per-problem audit.
`REVIEW.md` — the six review gates every change goes through.
