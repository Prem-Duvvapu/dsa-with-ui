# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Run from the repo root unless noted. The root `package.json` scripts wrap everything in
`wsl --exec` because they are meant to be typed from Windows PowerShell — if your shell is
already inside WSL, call `mvn`/`npm` directly as below.

```bash
# Backend (Java 17, Maven)
cd backend && mvn test                                  # full backend suite
cd backend && mvn test -Dtest=TracerContractTest         # one test class
cd backend && mvn test -Dtest=ProblemsApiTest#missingVersusNotYetTraced
cd backend && mvn spring-boot:run                        # http://localhost:8923

# Frontend (React 18 + Vite)
cd frontend && npm ci
cd frontend && npx vitest run                            # full frontend suite
cd frontend && npx vitest run src/designTokens.test.js   # one file
cd frontend && npx vitest run -t 'renders Header'        # one test by name
cd frontend && npm run dev                               # http://localhost:5180, proxies /api → 8923
cd frontend && npx vite build                            # what CI builds

# Both tiers
./start.sh                                               # backend 8923 + frontend 5180; Ctrl+C stops both (Linux/WSL/macOS)
docker-compose up --build                                # frontend on http://localhost:5174
```

There is **no `pom.xml` at the repo root** — the Maven project is `backend/pom.xml`. Running
`mvn spring-boot:run` from the root fails with `No plugin found for prefix 'spring-boot'`.
Chain steps with `&&`, never `|`.

Ports are load-bearing and appear in four places that must agree: `vite.config.js` (5180),
`docker-compose.yml` (5174→80), `CorsConfig.java` (allows both), and `CorsPolicyTest`.

CI (`.github/workflows/ci.yml`) runs `mvn -B test`, the launcher process-tree smoke test,
`npx vitest run`, and `npx vite build` on every branch push and on PRs to `main`.

## Branch discipline

**Never edit, commit, or push on `main`.** Always cut a working branch from `main` first —
documentation-only changes included. Merges into `main` go through a pull request so CI
runs; committing directly bypasses that gate.

```bash
git switch main && git pull && git switch -c fix/<topic>
```

If you notice you have already modified files while on `main`, run `git switch -c <branch>`
before committing — uncommitted changes carry across.

**Publishing needs approval.** Ask before any `git`/`gh` command that writes or publishes:
`commit`, `push`, `merge`, `rebase`, `reset`, `tag`, `gh pr create`, `gh pr merge`, or
`gh api` with a non-GET method. Read-only commands (`status`, `log`, `diff`, `show`,
`branch`, `pull`, `fetch`, `gh run list`, `gh pr view`) need no approval — just run them.
Do the work and describe the change set, then let the user decide when it lands.

**Keep pull requests small to medium, and land them one at a time.** A phase of work is
planned as a numbered PR sequence, not shipped as one large branch — each PR reviewable
on its own, merged, pulled, then the next one starts. This keeps `main` continuously
green, since CI gates every merge.

## Architecture

### One layer. The migration is finished.

**The legacy layer is gone.** There is exactly one API — `/api/problems`, served by
`ProblemsController` over `tracer/` and `catalog/`. The eighteen `/api/{topic}/...`
controllers are deleted and those routes now 404;
`ProblemsApiTest.legacyRoutesNoLongerExist` asserts that, so reintroducing one is a test
failure rather than a quiet regression.

**What survives, and why it looks legacy.** The eighteen `service/*Service` classes are
still there and still named after topics, but they are **catalogue providers only**: each
implements `catalog/ProblemProvider` and owns the `ProblemDetail` metadata for its topic,
which `ProblemCatalog` merges into one id-keyed view. All 431 problems' titles,
descriptions, categories, default structures and `dsType`s live in their `initProblems()`.
They have no `generateSteps`, no `switch (problemId)`, and no `ExecutionStep` import — that
half was the legacy trace layer and it is deleted. `ProblemProviderContractTest` is
parameterized over all eighteen and replaces the eighteen copy-pasted `*ServiceTest`
classes.

Deleting a service therefore deletes that topic's catalogue. It is not dead code.

**Tracer layer (`tracer/`, `catalog/`, `ProblemsController`).** Everything. This is where
all work goes.

### Why the tracer layer exists

The catalogue registers 440 problems (433 unique ids) but only ever produced 137 distinct
animations. 303 ids returned *another algorithm's* trace, via one-line delegate methods and
a step-returning `default:` branch in every service switch. The test suite could not see it,
because its only per-problem assertion was `!steps.isEmpty()` — which the fallback
guaranteed.

Three rules follow from that, and they are the point of the design:

1. **No fallback, anywhere.** `TracerRegistry` returns `Optional.empty()` for an unknown id.
   `ProblemsController` answers **404** (no such problem) or **501** (catalogued but not yet
   traced). Never substitute a different problem's steps. An unknown `dsType` likewise
   renders an explicit unsupported state, never `ArrayCanvas`. The fallback that caused the
   original incident cannot recur, because the layer that held it no longer exists — the
   eighteen legacy controllers and all 80 of their step generators are deleted.
2. **`traced` is an honesty flag, not a feature flag.** `GET /api/problems/stats` reports
   `catalogued` vs `traced` vs `untraced`. The UI says "not yet traced" rather than animate
   the wrong thing. **All 431 of 431 are traced**, so `untraced` is currently 0 — the flag
   stays because it is what makes a regression visible, not because work is outstanding.
   Never quote that number from this file: run
   `curl -s localhost:8923/api/problems/stats`.
3. **Tests must detect fake work, not just crashes.** `TracerContractTest.traceRespondsToItsInput`
   runs each tracer on two materially different inputs and fails if the traces are identical
   — a canned narration cannot survive it. When you fix something, prove the new test fails
   against the old code before accepting it.

### The tracer contract

```java
public interface AlgorithmTracer {
    String id();                              // "kadane-algo"
    DsType dsType();                          // closed canvas vocabulary
    InputSpec inputSpec();                    // declared inputs, bounds, defaults
    Map<String, Object> alternateInput();     // materially different contract input
    String annotatedCode();                   // Java source carrying // @a anchors
    void run(Inputs in, StepEmitter emit);    // executes the algorithm for real
}
```

Implementations are Spring `@Component`s in `tracer/impl/`; `TracerRegistry` collects
`List<AlgorithmTracer>` and **fails application startup** on a blank/duplicate `id()` or a
missing `dsType`. `CatalogTracerMetadataTest` also joins every live tracer to the winning
catalogue entry so their canvas types cannot drift independently.

**Line anchors, not line numbers.** `activeLine` used to be an unchecked integer, and
`LinkedListService` emitted line 51 into a 9-line code block. Now a tracer writes
`emit.at("loop.compare")` and the source carries `// @a loop.compare` on the line above the
target. `AnnotatedCode` resolves names to numbers, strips the markers from the displayed
code, and rejects duplicate/unnamed/dangling anchors. The highlight therefore provably
points at code the user can see.

**`InputValidator` is the only trust boundary.** Input is caller-supplied via
`POST /api/problems/{id}/execute`, so it rejects unknown fields (rather than ignoring them),
enforces every `InputField` constraint, and collects *all* field errors into one 400. Two
caps are mandatory, not optional — a per-field size ceiling in the `InputSpec` and a global
step budget (default 5000, `truncated: true` on the response). Without them a caller sets
`n = 20` on a factorial-time problem and takes the server down.

**`ProblemCatalog`** merges the 18 services (each implements `catalog/ProblemProvider`) into
one id-keyed view. First provider to claim an id wins; collisions are surfaced in
`stats.duplicateIds` rather than hidden. Seven ids are currently claimed twice.

### Frontend

`App.jsx` loads the catalogue and executions from `/api/problems`; `useTrace` owns request
cancellation, stale-response protection, delta decoding, and playback. Canvas selection has
one source of truth: `frontend/src/canvas/registry.js`. It is keyed by the backend's 16-value
`DsType` contract, and its cross-tier test fails if the enum/fixture/registry drift.

`frontend/src/canvas/registry.js`'s `CANVAS_BY_DSTYPE` map routes the backend's 17
`dsType` values to fifteen canvas components: `ArrayCanvas`, `WindowCanvas`,
`SearchSpaceCanvas`, `GridCanvas`, `DpTableCanvas`, `TreeCanvas`, `GraphCanvas`,
`LinkedListCanvas`, `StackCanvas`, `QueueHeroCanvas`, `HeapCanvas`, `TrieCanvas`,
`RecursionTreeCanvas`, `DsuCanvas` and `IntervalCanvas`. `String` and `Bits` still route to
`ArrayCanvas`: `Bits` deliberately — `StepEmitter.bits()` renders a fixed 32-wide bit track
over it rather than a new canvas (see README's Bit Manipulation notes) — while `String` has
no dedicated canvas yet. This is explicit mapping, not an unknown-type fallback; an unlisted
`dsType` renders an explicit unsupported state. Trie transport and its canonical
backend/canvas node shape are active and guarded by the Trie canvas tests; see resolved
`RCA-012` in `RCA.md`.

Styling is CSS custom properties in `index.css` plus inline styles; only a handful of CSS
classes exist. `designTokens.test.js` is a static guard that fails the build on any `var()`
or `className` referencing something `index.css` does not define — a rewrite once deleted 15
tokens while 5 components still used them, and CSS silently drops unresolvable declarations.

## Adding a tracer

1. New `@Component` in `tracer/impl/` implementing `AlgorithmTracer`; the `id()` must match
   an existing catalogue id (otherwise `ProblemCatalog.getOrphanedTracerIds()` flags it),
   and `dsType()` must match that catalogue entry.
2. Anchor the code you return from `annotatedCode()`, emit only those names, and emit
   *every* one of them — `anchorsAreAllReachable` fails on a marker nothing highlights.
3. Implement `alternateInput()` — a *materially different* input, not a permutation. It is
   abstract on the interface so a tracer cannot skip it, and `alternateInputDiffersFromDefaults`
   rejects one pasted from the spec defaults. `TracerContractTest` is driven off the registry
   and names no tracer, so this is the only file you touch.
4. Emit the structure that `dsType()` promises; never retag metadata without supplying the
   corresponding trace payload and canvas behavior.
5. `mvn test`. The contract tests cover step numbering, anchor resolution, defaults
   validating against their own spec, and cross-registry trace distinctness.
6. Generate its golden file, then **read it**:
   `mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true`. Golden files pin trace
   *content* — the descriptions, variables and highlighted lines — which every other test
   is blind to. Regenerating one without reading the diff records a bug as expected.

## Pinned numbers

`ProblemsApiTest` asserts `431` unique ids and `0` duplicates. These are intentional
tripwires — if a change moves them, update the assertions deliberately and update the
`README.md` coverage table in the same commit.

The duplicate count is **zero and must stay zero**. It was 7 for a long time: two graph
services catalogued the same problems, and four more pairs escaped that count entirely by
differing only in word order (`rotten-oranges` / `rotting-oranges`).
`DuplicateProblemTest` now fails on both shapes.

## Documentation map

- `ARCHITECTURE.md` — **start here.** The system as it stands, with diagrams: the request
  path, the tracer contract, dsType→canvas routing, and where the guards sit.
- `plan.md` — the v2 tracing architecture. Accurate; the source of the current design.
- `AUDIT.md` — full per-problem audit of the catalogue across its topics, with the
  findings fixed so far and the two left open for an owner decision.
- `REVIEW.md` — six review gates every change goes through (backend, frontend, product,
  security, performance, docs), each built from a failure this codebase has actually had.
  Run it alongside `dsa-review` before a PR lands.
- `references.md` — UI/UX research and design tokens. `PROJECT_CONTEXT.md` — pedagogical
  principles.
- `RCA.md` — recurring-incident ledger. Consult it before changing an affected subsystem;
  update it when a defect is introduced or discovered, including the RED-first guard.
- `PROMPT-E-canvases.md` / `PROMPT-F-visual-fidelity.md` / `PROMPT-J-full-roadmap.md` —
  historical implementation prompts for the canvas build-out and the tracer migration's
  later batches. Kept for their design rationale (the `SEARCH_SPACE`/`DP_TABLE` variant
  reasoning, the Bench token system, the per-batch verification discipline), not as a
  live worklist — their status headers say what has since shipped. `HANDOFF.md` and
  `PROJECT_COMPLETION_PLAN.md`, the two working documents these superseded, were deleted
  once the work they tracked (the tracer migration and the legacy-layer retirement) was
  complete, per their own stated deletion criteria.
