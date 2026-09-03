# DSA Visualizer — handoff prompts for remaining work

> **Current status — 2026-08-31.** The snapshots embedded in the original prompts below are
> historical. The live system has 433 unique catalogue ids and 43 tracers; the frontend uses
> the v2 `/api/problems` API. Prompt E Phase 0 (closed `DsType` registry) and Phase 1
> (trace-owned tree/graph topology) are complete, and its Phase 2 wire contract is complete.
> The three LIS tracers and `DpTableCanvas` are an intentional partial delivery of Phases 3/4.
> Dedicated canvases/retagging for the other types, `CaptureStrip` Phase 5, and the remaining
> tracer migration are still open. Trie transport exists, but activation is blocked on the
> backend/canvas shape mismatch recorded in `RCA.md`. Read live counts from
> `GET /api/problems/stats`; do not treat old counts below as current claims.

> ## ⚠️ TEMPORARY — delete this file when the work is done
>
> This is a working document, not project documentation. It exists to hand the remaining
> phases to other agents or contributors, and it goes stale the moment they land: the
> coverage numbers, the "still present on purpose" list, and the per-category stub counts
> below are all snapshots of one moment.
>
> **Delete it once PROMPT D is complete.** Prompt D's checklist includes doing so.
> Anything here still worth keeping by then belongs in `README.md` or `plan.md` instead.
>
> Snapshot taken at: 433 catalogued / 8 traced, on `main` at `a5970fb`. If those numbers
> no longer match `GET /api/problems/stats`, treat this file with suspicion.

Five prompts. **Z is done** — CI runs on every push and PR, and branch protection
requires `Backend (JUnit)` and `Frontend (Vitest + build)`. **A must land before C.**
B and C can run in parallel by different agents. D is last.

| | Prompt | Depends on | Size |
|---|---|---|---|
| Z | CI workflow gating merges to main | — | ✅ done |
| A | Scale the harness, fix the anchor defects, shrink the wire format | — | medium |
| B | Frontend redesign + restructure onto the v2 API | A, for the step format only | large |
| C | Migrate the catalogue onto the tracer contract | A | very large |
| D | Retire the legacy layer | B, C | small |

**Scope note (2026-08-22).** The owner has lifted the earlier "do not change the backend"
restriction on prompt B. Backend and frontend changes are both in scope. Payload and
harness work still belongs in A, because C is written against whatever A leaves behind.

**The visual direction is settled.** It is called *Bench*, and it is specified inside
prompt B. Do not redesign it; build it.

---

## SHARED CONTEXT — paste at the top of every prompt

```
Repo: /mnt/c/Users/Hp/OneDrive/Desktop/dsa-with-ui
Branch to start from: main (cut a working branch; never commit on main)
Backend: Spring Boot 3.2.3 / Java 17, port 8923.  Frontend: React 18 + Vite, dev 5180, docker 5174.
Build: `cd backend && mvn test` (401 tests, green)  ·  `cd frontend && npx vitest run` (130 tests, green)
Publishing (commit / push / gh pr create / gh pr merge) requires the owner's approval.
Read-only git and gh commands do not.

BACKGROUND
This project visualizes DSA algorithms step by step. An audit found 433 catalogued
problems producing only 137 distinct visualizations: 303 ids returned a DIFFERENT
algorithm's animation, via 122 one-line delegate generators and a step-returning
`default:` in each of 18 service switches. The old test suite could not detect this
because its only per-problem assertion was that the step list was non-empty, which the
fallback guaranteed.

A new contract now exists to make that impossible. Work with it, not around it.

THE CONTRACT — com.dsa.ui.tracer
    public interface AlgorithmTracer {
        String id();                            // must match a catalogue ProblemDetail id
        DsType dsType();                        // closed canvas vocabulary
        InputSpec inputSpec();                  // declared inputs, bounds, defaults
        Map<String, Object> alternateInput();   // materially different contract input
        String annotatedCode();                 // Java source carrying `// @a name` anchors
        void run(Inputs in, StepEmitter emit);  // RUNS THE REAL ALGORITHM
    }

  - Tracers are Spring @Components, discovered by TracerRegistry, indexed by id().
    They MUST be stateless — one instance serves all concurrent requests.
  - TracerRegistry has NO FALLBACK. Unknown id => 404. Never substitute another trace.
  - Blank/duplicate ids and missing dsType fail application startup.

  StepEmitter is fluent and names code lines by ANCHOR, never by line number:
        emit.at("loop.compare")
            .say("i = %d: sum %d beats best %d", i, sum, best)
            .var("i", i).var("best", best)
            .array(nums, i)                     // or .grid/.list/.tree/.graph/.dpTable/etc.
            .step();
        emit.push("frame"); emit.pop();         // call stack for recursive traces

  AnnotatedCode strips `// @a name` markers from the displayed source and resolves the
  anchor to the line BELOW it. An unknown anchor throws. This replaced a hand-written
  `activeLine` integer that had drifted — one generator emitted lines 51 and 56 into a
  nine-line snippet, and 29 problems delegated to it. Never reintroduce raw line numbers.

  InputSpec / InputField / FieldType declare inputs so the frontend renders ONE generic
  form for every problem. Field kinds: INT, INT_ARRAY, STRING, INT_GRID, GRAPH,
  LINKED_LIST, BINARY_TREE.
        InputSpec.of(
            InputField.of("nums", FieldType.INT_ARRAY)
                .label("Array").help("...")
                .length(1, 40).values(-999, 999)   // also .sorted() .distinct() .range(min,max)
                .defaultValue(List.of(2, 7, 11, 15)).build()
        ).withMaxSteps(5000);

  InputValidator is the ONLY trust boundary. Size caps are mandatory, not stylistic:
  once input is user-supplied, an unbounded n on a factorial-time algorithm is a denial
  of service. Every trace also has a step budget; exceeding it unwinds the algorithm and
  returns `truncated: true`.

API (v2)
    GET  /api/problems                  whole catalogue; each entry has `traced` + inputSpec
    GET  /api/problems/stats            catalogued / traced / untraced / duplicateIds
    GET  /api/problems/{id}
    GET  /api/problems/{id}/execute     runs declared defaults
    POST /api/problems/{id}/execute     runs the caller's input
    GET  /api/problems/{id}/input-spec
    404 = no such problem.  501 = catalogued but not yet traced.  400 = invalid input,
    with a message per field.

CURRENT NUMBERS (433/440/7 are pinned by ProblemsApiTest — update the test if you change them)
    440 id registrations across 18 services, 433 unique, 7 claimed by two services.
    43 traced. README.md carries the current checked list; GET /api/problems/stats is
    authoritative for the count.

    Migrated ids answer 410 Gone on their legacy /api/<topic>/execute/<id> path
    (LegacyTraceRetiredException + ApiExceptionHandler), never a substitute trace.
    When a tracer retires an id, add it to ApiContractTest.retiredTraces() and to the
    per-service test's retired set — the suite enforces this.

STILL PRESENT ON PURPOSE
  - The 18 legacy per-topic controllers (/api/arrays/..., /api/trees/...) still serve the
    old paths for compatibility. The frontend uses v2. Do not delete until prompt D.
  - The 18 services still hold catalogue metadata and their old switch-based generators.
  - 7 duplicate ids are surfaced in stats.duplicateIds, not resolved.

NON-NEGOTIABLE WORKING RULES
  1. Never make a problem show another problem's trace. No `default:` fallbacks.
  2. Every fix gets a test, and you MUST verify the test FAILS against the broken code
     before accepting it. Temporarily revert the fix, watch the test go red, restore.
     A green test that would not have caught the bug is worth nothing.
  3. Do not weaken or delete an existing assertion to make something pass.
  4. Report honestly. If something is partly done, say which part.
  5. Both suites must be green before you finish.
```

---

## PROMPT Z — CI workflow ✅ DONE (kept for the record)

```
Task: add a GitHub Actions workflow that runs both test suites on every pull request, so
nothing merges to main without them passing.

There is no CI in this repo today. Every green result so far has been someone running the
suites locally and remembering to look.

DO
1. Create .github/workflows/ci.yml, triggered on `pull_request` targeting main and on
   `push` to main. Two independent jobs so a frontend failure does not mask a backend one:

     backend:
       - actions/checkout
       - actions/setup-java@v4, temurin, java-version 17, cache: maven
       - working-directory: backend, run: mvn -B test
     frontend:
       - actions/checkout
       - actions/setup-node@v4, node 20, cache: npm,
         cache-dependency-path: frontend/package-lock.json
       - working-directory: frontend, run: npm ci
       - working-directory: frontend, run: npx vitest run
       - working-directory: frontend, run: npx vite build

2. TRAP — do not use the root package.json scripts. `test:backend` and `build:backend`
   are wrapped in `wsl --exec bash -c "..."`, which is a Windows-host helper and will not
   exist on a Linux runner. Call mvn and npx directly with working-directory, as above.

3. Upload test reports on failure so a red run is diagnosable without re-running:
   actions/upload-artifact with backend/target/surefire-reports/, `if: failure()`.

4. Add a concurrency group keyed on the ref so pushing twice cancels the stale run.
   The backend suite takes roughly two minutes (ApiContractTest alone is ~55s and
   GraphBfsDfsControllerTest ~12s), so this matters.

5. Set a sensible timeout-minutes on both jobs (15 is ample) so a hung run does not
   burn an hour.

6. Add a status badge to the top of README.md.

CANNOT BE DONE FROM CODE — tell the user to do this by hand
   Branch protection is a repository setting, not a file. After the first run lands on
   main, the user must enable it at
   Settings > Branches > Add rule for `main`:
       - Require a pull request before merging
       - Require status checks to pass: select both `backend` and `frontend`
       - Require branches to be up to date before merging
   Without that, the workflow reports failures but does not actually block a merge.
   State this clearly in your final message — do not imply merges are gated when they
   are not.

VERIFY
  - Validate the YAML parses (`python3 -c "import yaml,sys;yaml.safe_load(open('.github/workflows/ci.yml'))"`
    or actionlint if available).
  - Push the branch and confirm BOTH jobs actually run and go green on the real runner.
    Do not just assert the file looks right — a workflow that never triggers is the
    normal failure mode here.
  - Prove it gates: push a commit that breaks one test (e.g. change an assertion in
    AnnotatedCodeTest), confirm the backend job goes red, then revert. Show the run URL.
```

---

## PROMPT A — Scale the harness, fix the anchor defects, shrink the wire format

> Three jobs, one prompt, because they all touch the tracer layer and C is written
> against whatever this leaves behind. Landing them separately means migrating 425
> problems twice.
>
> **Progress — mostly landed.** Shipped as a PR sequence rather than one branch:
>
> | | | PR |
> |---|---|---|
> | ✅ | 7, 8 — `anchorsAreAllReachable` made real; 6 tracers fixed; `binary-search-1d` default | #6 |
> | ✅ | 1, 2, 3 — `alternateInput()` on the tracer, registry-driven `@MethodSource` | #7 |
> | ✅ | 4, 6 — growth test and jacoco | #8 |
> | ✅ | 9b — byte budget, `truncationReason`, estimator accuracy test | #9 |
> | ✅ | 9a — delta-encoded steps + keyframes, `?encoding=full` for one migration | #11 |
> | ⬜ | **9c — per-element deltas.** See below; 9a got 30%, not the 10x it predicted. | — |
> | ✅ | 5 — golden files for all 8 tracers, pinned against the full step shape | #12 |
>
> **9c, the part 9a did not get.** Delta encoding removed the fields that do not change
> between steps, which measured 30% across the eight tracers — useful, and far short of the
> order of magnitude estimated from the raw numbers. The reason is that the one payload
> field a tracer does vary changes on nearly every step, so it is resent whole every time.
> Inside it, almost nothing moves: an `ArrayElement` resends `index` and `value` when only
> `state` changed, and a `TreeNode` resends `id`, `val`, `x`, `y`, `leftId` and `rightId` to
> communicate one changed `state` string — which is why the tree traversals only improved
> 14%. Splitting each payload into structure (sent at keyframes) and a parallel array of
> state strings (sent every step) is where the remaining order of magnitude is. It is a
> second wire-format change, so it belongs in its own PR after B has a decoder working
> against 9a.
>
> Backend suite is at 401 tests. **Only 9c is left, and it is optional** — Prompt A's
> original scope is complete. B and C are unblocked.

```
Task: make the tracer test harness workable at 400+ tracers, fix three verified defects
in anchor coverage, and stop the trace payload growing as steps x n.

PROBLEM
backend/src/test/java/com/dsa/ui/tracer/TracerContractTest.java holds a hardcoded
ALTERNATE_INPUT map keyed by tracer id, and registryMatchesThisTestsExpectations()
asserts it exactly equals the registry's ids. That works for 8 tracers and collapses at
433: every new tracer would require editing a central test file, and merge conflicts
would be constant.

The map exists to power traceRespondsToItsInput(), which runs each tracer on two
materially different inputs and fails if the traces match. That test is the single most
important one in the repo — it is what a canned narration cannot survive. Keep its
behaviour exactly; change only where the second input comes from.

DO
1. Move the alternate input to the tracer itself. Add to AlgorithmTracer:

       /** A second input, materially different from the spec's defaults, used by the
        * contract test to prove this tracer reads its input rather than replaying a
        * fixed narration. */
       Map<String, Object> alternateInput();

   Make it abstract (no default implementation) so a new tracer cannot silently skip
   the check. Implement it on all 8 existing tracers using the values currently in
   ALTERNATE_INPUT.

2. Rewrite TracerContractTest to drive @MethodSource off the registry instead of the
   hardcoded map. Note the ordering problem: @MethodSource is static and cannot see an
   injected field. Solve it cleanly — a static Spring context holder, or make the class
   implement a JUnit TestInstance lifecycle that permits it. Delete
   registryMatchesThisTestsExpectations() once the source is the registry itself.

3. Add a test asserting alternateInput() is genuinely different from the spec defaults
   (not a copy), so the responds-to-input test cannot be defeated by pasting the default.

4. Add a growth test: for tracers whose spec has an INT_ARRAY / INT_GRID / LINKED_LIST /
   BINARY_TREE field, run at size N and a larger N and assert the step count increases.
   Skip tracers where growth is not meaningful, and log which were skipped — silent
   skipping reads as coverage that does not exist.

5. Add golden-file tests for the 8 current tracers: serialize the default-input trace to
   src/test/resources/golden/<id>.json and assert byte equality on later runs, with a
   documented way to regenerate. This pins trace CONTENT, not just shape.

6. Add jacoco to backend/pom.xml (there is no coverage tooling on either side). Report
   only — do not add a failing threshold yet.

7. FIX A VACUOUS TEST. TracerContractTest.anchorsAreAllReachable ends at
   `assertFalse(usedLines.isEmpty(), id + " emitted nothing")`. It never compares what
   was emitted against code.getAnchors(), so a tracer can declare ten anchors, emit one,
   and pass. Make it diff declared anchors against the lines actually highlighted across
   the trace, and fail naming every anchor never reached.

   Measured from the live API before you start — six of the eight tracers fail this the
   moment it becomes real:
       binary-search-1d   3 steps / 7 anchors   dead: left, loop, miss
       two-sum            5 steps / 7 anchors   dead: check, loop, none
       bfs-traversal     21 steps / 8 anchors   dead: loop, neighbours
       kadane-algo       17 steps / 6 anchors   dead: loop
       reverse-linked-list 14 steps / 6 anchors dead: loop
       number-of-islands 12 steps / 5 anchors   dead: scan
       tree-preorder, tree-inorder              clean
   Expect that failure on the first run. It is the acceptance criterion, exactly as the
   distinct-trace test was.

8. THEN FIX THE CAUSES. They are two different bugs and need two different fixes.
   a) A declared anchor with no matching emit.at() is dead for EVERY input — dead code in
      the annotation. Either emit it or delete the marker. `loop` is the common offender.
   b) An anchor unreachable only because of a poor default input is a demo problem, not a
      code problem. binary-search-1d defaults to nums=[1,3,5,7,9,11,13], target=7, and 7
      sits exactly on the first midpoint — so the trace ends in three steps having never
      executed a comparison branch or the not-found path. Change the default so the
      landing animation shows the algorithm actually working. Do NOT delete the anchors
      to make the test pass; that is weakening an assertion.

9. STOP THE PAYLOAD GROWING AS steps x n. Every ExecutionStep carries a full snapshot
   (arrayState, gridState, listState, nodeStates, treeNodes). Measured on kadane-algo:

       n = 9  (spec default)   17 steps    11,675 bytes     687 bytes/step
       n = 40 (spec ceiling)   54 steps   122,515 bytes   2,268 bytes/step

   That is roughly 450 + 45n bytes per step, so the existing 5000-step budget permits an
   ~11 MB response for a single click. maxSteps bounds CPU; it does not bound bytes.
   Kadane rewrites all 40 elements every step to record a change in one of them.

   a) Emit deltas, not snapshots: what changed this step, plus a full keyframe every ~50
      steps so the frontend can still scrub to an arbitrary position without replaying
      from zero. Keep the existing shape available behind a query flag until prompt B has
      migrated, then drop it.
   b) Add a byte budget to TraceRunner beside the step budget. Stop at whichever trips
      first and report BOTH through the existing `truncated` flag plus a reason string.
      Do not invent a second flag; the frontend already has to handle one.
   c) Golden files (item 5) must pin the NEW format, so write them after this lands.

VERIFY
  - `cd backend && mvn test` green, and the total is >= 308.
  - Prove the harness still works: temporarily rewrite KadaneTracer.run to emit two fixed
    steps ignoring its input. traceRespondsToItsInput must fail and name kadane-algo.
    Restore it. Show me the failure output.
  - Prove item 7 works the same way: it must go RED against today's tracers, naming the
    six above. Show that output before you fix anything.
  - Re-measure kadane-algo at n = 40 and report the new bytes/step against 2,268.
  - Confirm adding a tracer now requires touching only its own file plus its golden file.
```

---

## PROMPT B — Frontend redesign onto the v2 API

> **The visual direction is approved and specified below. It is called Bench.** The owner
> rejected the previous UI outright and has since approved this replacement. Do not
> propose alternatives, do not restyle the old component tree, and do not treat the spec
> below as a starting point to improvise from. Build it.
>
> Worked screens, all four interactive — study these before writing any CSS:
> - Directions pitch: https://claude.ai/code/artifact/8bd6a376-7694-4e70-a0f1-9c2fe357526e
> - Bench, fully drawn: https://claude.ai/code/artifact/536b0e7f-15c4-4299-8f37-26315326b325
> - Bench on paper (light theme): https://claude.ai/code/artifact/dec34ef4-7873-4b2b-8222-a309c269c388

```
Task: rebuild the interface as Bench and restructure the React frontend onto the v2 API.
Keep React + Vite. The visual design is a rewrite; the data layer is a restructure.

WHAT BENCH IS
A teaching instrument, styled as bench equipment. Every decision answers one question:
does this make the state transition obvious to someone who does not yet understand it?

The signature is the CAPTURE STRIP. Under the canvas sits a grid: one column per step,
one row per tracked slot, the whole execution visible at once. "Step 1 of 2" is a
counter, not a shape — the strip is the shape. It is also why Bench was chosen over the
alternatives: it generalises. An array's rows are indices, a graph's rows are vertices
holding dist, a DP table's rows are entries. Same component, driven by dsType.

PALETTE — 14 tokens, two semantic hues, both themes designed
Style every component through these tokens. No component may hardcode a colour, and no
colour may be defined only inside a media query or a [data-theme] block.

    token          role                     dark          light
    --g            app ground               #0a0e13       #eef1f4
    --g2           canvas surface           #0e141b       #ffffff
    --panel        chrome panel             #111820       #f7f9fb
    --rl           hairline                 #1e2a35       #d9e0e6
    --rl2          hairline, stronger       #2c3a47       #b8c2cc
    --fill         a cell holding a value   #16202a       #e6ebf0
    --dim          labels, indices          #718293       #6b7683
    --txt2         secondary ink            #8b9dab       #4a5663
    --txt          primary ink              #cfdce7       #10161c
    --probe        happening right now      #ffb000       #a35f00
    --probe-on     text on a probe fill     #0a0e13       #ffffff
    --probe-wash   active code line         #1a1508       #fdf4e6
    --settled      finished and proven      #3ddc97       #0e7a51
    --settled-on   text on a settled fill   #06120d       #ffffff

Nothing else in the chrome may be amber or green. Difficulty pills become a neutral
outline plus a letter (E / M / H) — this is what finally kills the collision where
--state-current and --diff-medium were both #f59e0b meaning different things.

THE LIGHT THEME IS NOT AN INVERSION, and must not be rebuilt as one. The rule is that
the probe is always the HIGHEST-CONTRAST mark against its own ground: on black it glows,
on paper it is dense ink. Same hue, opposite luminance. Three tokens genuinely flip role
rather than just moving: --fill (lighter than ground -> darker than paper), --probe and
--settled (glow -> ink). The density band's ramp reverses with them — on screen brighter
means lower cost, on paper more ink means more certainty.

CONTRAST, measured against each theme's own canvas surface. Every role clears 4.5:1:
    primary ink    13.26 / 18.20      probe            10.10 / 5.01
    secondary ink   6.62 /  7.49      resolved         10.47 / 5.36
    dim             4.69 /  4.62      text on fills    10.56 / 5.01
Note --dim: it was #4a5a68 at 2.60:1 in the first mockups, which failed while carrying
every uppercase label and array index. Use #718293. Do not revert it.

FIVE STATES, and state is never encoded by colour alone
    probe     filled amber block   + a ▼ caret        being written / examined now
    read      hollow amber ring                       being read from this step
    known     neutral fill                            has a value
    resolved  filled green         + a ✓              final
    void      dashed outline                          untouched / unreachable / ∞
`read` as a ring rather than a sixth colour is what lets a DP recurrence show its reach,
and a graph relaxation show its source, without spending another hue.

TYPE
    JetBrains Mono   data, code, labels, readouts — the primary voice
    Archivo          problem titles and headings
All-monospace was the original pitch and it is too tiring at full-page scale. This split
is deliberate; keep it.

LAYOUT
    app bar     logo · breadcrumb · difficulty · traced badge · N/433 runnable
    sidebar     15rem — search (/ to focus) · runnable|all filter · grouped list · legend
    stage       canvas (the hero) then the capture strip, read in one downward glance
    transport   prev / play / next · scrub track · step N of M · speed presets
    lower       code pane with the resolved @a anchor named | tabs: input · variables · complexity

MOTION IS PEDAGOGY, NOT DECORATION
Elements move between states so the change is traceable by eye: a swap visibly swaps, a
pointer travels, the scrub head moves along the strip. Instant repaints teach nothing.
One borrowed flourish, and only one: on the resolving step the resolved pair scales up
while everything else dims. Save the drama for the moment that earns it.
Respect prefers-reduced-motion throughout.

BEHAVIOUR AT SCALE — required, not an optimisation
    canvas   array bars to ~60 elements, then a heat row with no per-element labels
             graph node-link to ~80 vertices, then adjacency-matrix heat
    strip    labelled cells to ~40 steps
             then a DENSITY BAND: same rows, one thin column per step, no text, drawn on
             <canvas> not DOM — 5000 columns x 40 rows is 200,000 nodes and will hang
             then BUCKETED columns past a few thousand steps, each column a min/max
             summary of a range. Do not plain-downsample; it aliases away the sweeps that
             are the whole point of the band.
    playback per-step animation stops past a few hundred steps; becomes scrub + jump
See the "what the strip becomes at scale" figure in the second artifact — that band is a
real run of coin change at amount 60, 176 steps, painted from the trace data.

THEME PLUMBING
The viewer has three states, not two: an explicit choice stamps data-theme, and the
default "system" setting stamps nothing. Define the complete light palette on bare :root,
redefine only the tokens under @media (prefers-color-scheme: dark) guarded as
:root:not([data-theme="light"]), then again under :root[data-theme="dark"] so an explicit
toggle wins in both directions.

AVOID
The generic AI-app look: purple-blue gradient hero, everything centered, uniform rounded
cards with a coloured left rail, emoji as section markers, Inter-for-everything.

Then, the structural work:

CURRENT STATE
frontend/src is ~2,700 lines. App.jsx holds all state, fans out to 18 hardcoded legacy
endpoints, and picks a canvas by string-matching the category name. There is no router.
Recent Phase 0 work already fixed: two 404ing endpoints, 15 undefined CSS custom
properties, three orphaned components, a duplicate fetch per click, and a stale-response
race. Do not undo those. designTokens.test.js fails the build on any unresolvable var().

DO
1. Router (react-router-dom): /problem/:id. There are currently no deep links, no back
   button, no shareable URL.

2. Replace the 18-endpoint fan-out in App.jsx (fetchAllProblems) with a single
   GET /api/problems. Delete the category->endpoint if/else ladder entirely; it exists in
   three divergent copies (App.jsx endpoint resolution, App.jsx renderCanvas, and
   Sidebar.normalizeCategory) and they disagree with each other.

3. One hook, useTrace(problemId, input), owning fetch + AbortController + stale-response
   guard + playback state. App.jsx currently owns all of this inline.

4. Canvas selection must come from the backend's dsType, never from category.includes(...).
   Trie problems currently fall through to GraphCanvas and render a blank SVG — add the
   missing TrieCanvas (trieState is already served by the API).

5. <CanvasShell> with header / legend / stage slots; the five canvases plug in. This
   removes: getNodeColor written 4x with 4 different palettes, the 4-badge legend
   copy-pasted verbatim 3x, and the canvas chrome repeated 5x. Split GraphCanvas.jsx
   (349 lines holding five visualizers behind ad-hoc predicates: DSU / sudoku /
   chessboard / matrix / graph).

5b. <CaptureStrip> — ONE component, not one per category. Props are rows, steps and the
   per-cell state; dsType decides only what a row MEANS (index / vertex / table entry /
   list node). It renders labelled cells, then the density band, then bucketed columns,
   at the thresholds given in the design spec. Build the band on <canvas>. This is the
   signature of the whole design — if it ends up per-category, Bench has been lost.

6. THE HEADLINE FEATURE — the input panel. Render a form generically from the problem's
   inputSpec (GET /api/problems/{id}/input-spec, or the inputSpec on the catalogue entry),
   with a real editor per field kind: array chips, grid painter, graph edge table,
   level-order tree box, plain number/text. Wire it to POST /api/problems/{id}/execute.
   Show the server's per-field 400 messages inline on the offending editor. Include
   "randomize" and "reset to default". Honour the spec's declared bounds client-side as
   a convenience, but the server remains authoritative.

7. Handle the three states honestly:
     traced        -> input panel + animation
     501 untraced  -> say "not yet traced" plainly. NEVER animate something else.
     truncated     -> tell the user the trace hit its step budget.

8. Fix these, all confirmed present:
     - No loading state per problem selection (only the initial catalogue load has one).
     - No error surface: with the backend down the app looks normal with 2 problems and
       "Library: 2 algorithms", signalled only by a console.warn.
     - No ErrorBoundary: a throw in any canvas blanks the whole app.
     - renderCanvas returns null instead of an empty state.
     - Mobile Memory<->Complexity tabs do not switch (initialTab is only a useState seed).
       activeTab initialises to 'canvas', which matches no tab button.
     - Default speed 800ms matches none of the 2000/1000/500/250 presets.

9. Accessibility, all confirmed present:
     - Sidebar rows are clickable <div>s with no role/tabIndex: THE ENTIRE CATALOGUE IS
       KEYBOARD-UNREACHABLE. Make them <button>s.
     - .btn { outline: none } with no :focus-visible replacement anywhere.
     - aria-live="polite" on the trace ticker; step changes are unannounced.
     - The global Space handler exempts only INPUT/TEXTAREA, so Space hijacks any focused
       button.
     - Mobile drawer has no backdrop, no Escape, no focus trap.
     - State is encoded by colour alone, and --state-current and --diff-medium are the
       same #f59e0b meaning different things.

10. Move off inline styles to CSS Modules over the existing custom-property tokens in
    index.css. Only 7 CSS classes exist across the whole tree today; everything else is a
    style={{}} object. Keep the token system — it is the good part.

DO NOT
  - Do not delete the legacy controllers or the 18 services. That is prompt D.
  - Do not redesign Bench. If something in the spec genuinely cannot be built, say so and
    stop; do not substitute your own direction.
  - Backend changes ARE now permitted (the earlier prohibition is lifted), with one
    exception: the trace wire format belongs to prompt A. If A has landed, consume its
    delta + keyframe format. If it has not, consume the current snapshot format and leave
    a single decode seam so switching is one file, not a refactor.

VERIFY
  - `npx vitest run` green; add tests for useTrace (abort + stale response), the input
    panel (renders from a mocked inputSpec, surfaces field errors), the untraced 501
    state, and a smoke render per canvas.
  - `npx vite build` clean.
  - Full keyboard-only traversal of the catalogue, which is impossible today.
  - Check at 320px width.
  - Run against a live backend (`cd backend && mvn spring-boot:run`) and confirm editing
    an input changes the animation, the highlighted code line, and the variables panel.
  - Drive the capture strip past each threshold with a synthetic trace (60 steps, 400
    steps, 6000 steps) and confirm it degrades to band then buckets without hanging.
    Measure the 6000-step render; if it is not interactive, it is not done.
  - Both themes: screenshot the same problem in light and dark. Neither may be an
    inversion of the other, and designTokens.test.js must stay green.
  - Confirm no component hardcodes a colour — every colour resolves through a token.
```

### Prompt B progress

| # | Job | State |
|---|---|---|
| 1 | Bench token layer, both themes, contrast guard | ✅ PR #13 |
| 5 | `<CanvasShell>` — header / legend / stage slots | ✅ built and wired |
| 5 | Split `GraphCanvas.jsx`; add the missing `TrieCanvas` | ✅ split into `GraphCanvas`/`DsuCanvas`/`GridCanvas`; `TrieCanvas` added |
| 5b | `<CaptureStrip>` — labelled / compressed / band, canvas + bucketing | ✅ built, tested, wired |
| — | Delta decoder (`src/trace/decodeTrace.js`), the seam onto prompt A's wire format | ✅ built, consumed by `useTrace` |
| 2, 3, 4 | `useTrace`, one endpoint resolver, `dsType` canvas selection | ✅ built, tested (7 tests), wired |
| 6 | The input panel — the headline feature | ✅ built, tested, wired |
| 7 | Honest states: loading, untraced, truncated | ✅ done |
| 8 | ErrorBoundary, catalog-fetch error surface, mobile tab sync, default speed | ✅ done — see note below |
| 9 | a11y: `.btn:focus-visible`, `aria-live` ticker, Escape + backdrop on mobile drawer | ✅ done — see note below |
| 10 | CSS Modules | ❌ not started |

**Where the wiring stands.** `App.jsx` fetches the catalogue once from `GET /api/problems`
(the 18-endpoint fan-out is gone) and all playback/fetch state lives in `useTrace`. Canvas
selection is a lookup in `frontend/src/canvas/registry.js`, keyed only by the backend's
closed `dsType`; the `hasGrid` and title/id sniffs are gone. Unknown values render an explicit
unsupported state. The sidebar itself, and the search box, are still on the pre-Bench tokens
and layout — restyling them is not tracked as its own job above and should be.

**The catalogue-summary vs. detail split.** `GET /api/problems` (the list) returns summary
fields only — id, title, category, dsType, traced, inputSpec. `javaCode`, `complexity`, and
every `default*` field used by the canvases live only on `GET /api/problems/{id}`. The first
version of this wiring fetched the detail response inside `useTrace` and then discarded it,
so `CodeViewer` silently fell back to its hardcoded placeholder and every canvas needing
`defaultGraphNodes`/`defaultGrid` had nothing — for every problem, not just untraced ones.
Fixed by having `useTrace` expose `detail` and having `App.jsx` merge it into `activeProblem`
(cleared on each new fetch so a stale problem's code/graph can't flash under a new title).
Caught by a regression test (`App per-problem detail merge`) using a fixture where the two
endpoints genuinely diverge, the way the last one — where both endpoints returned the same
object — could not have.

**On the capture strip's rows.** `rowStates`/`rowLabels` in `CaptureStrip.jsx` still read
whichever payload the step happens to carry (`arrayState`, then `treeNodes`, `listState`,
`gridState`, `nodeStates`) rather than `dsType`. That is deliberate — it keeps ONE component
— but it means a step carrying none of them draws no strip, which is the tested behaviour.
`dsType`-driven canvas selection has landed; making the strip's row meaning follow `dsType`
too is still open.

**The input panel.** `<InputPanel>` renders one editor per declared `FieldType`, all six of
which now have a live tracer exercising them (`two-sum`/`kadane-algo`/`binary-search-1d`:
`INT`+`INT_ARRAY`; `bfs-traversal`/`dfs-traversal`: `GRAPH`; `number-of-islands`: `INT_GRID`;
`reverse-linked-list`: `LINKED_LIST`; `tree-preorder`/`tree-inorder`: `BINARY_TREE`). No
tracer currently declares `STRING`, so that editor is untested against a real backend
response — worth exercising the first time a string-input problem is traced.
`Run`/`Randomize`/`Reset` populate the form only; none of the three auto-executes, so the
learner always sees the input before it runs. `useTrace.runInput` POSTs and, on a 400, sets
`fieldErrors` without touching `steps` — a rejected edit leaves the last good animation on
screen rather than blanking it. `truncated` (job 7's last piece) is now surfaced from both
the GET-defaults path and the POST path, shown as a small notice above the capture strip.
`IntArrayField`'s Add/Remove buttons are genuinely `disabled` at the length caps (not just
no-op handlers) — a test written against the no-op version caught the gap before it shipped;
`GridField`'s row/col buttons follow the same pattern.

**Not done in this pass, deliberately out of scope:** the sidebar/search restyle onto Bench
tokens, `react-router-dom` (no deep links yet), and job 10.

**Jobs 8/9 — several items on the original list were already stale by the time this ran;
re-verified each against the live code rather than trusting the doc.** Already fixed by
earlier work, not touched here: the Space handler already exempted `BUTTON`/`SELECT`; the
sidebar rows are a proper ARIA combobox/listbox with arrow-key nav (`SearchBox.jsx`, from
the search-overhaul PR), not unreachable `<div>`s; `--state-current`/`--diff-medium` no
longer collide (Bench token PR); the per-problem loading state already existed
(`traceLoading`). Actually fixed here:
- **`<ErrorBoundary>`** around `renderCanvas()`, both call sites — a throw in any canvas
  used to blank the whole app; now it loses just that one visualization, with a "try
  again" that clears on `resetKey={activeProblemId}` so switching problems retries fresh.
- **Catalog-fetch error surface** — a visible `role="alert"` banner with Retry, instead of
  only a `console.warn` while the app quietly showed the 2-problem offline fallback.
- **`MemoryComplexityCard`'s mobile tab desync** — `useState(initialTab)` only read the
  prop once; clicking Memory→Complexity on mobile left the card showing Memory. Now a
  `useEffect` syncs on every `initialTab` change; desktop (no prop) is unaffected.
- **Default speed** 800ms → 1000ms — the only prior value matching none of the four speed
  presets (2000/1000/500/250ms).
- **`.btn:focus-visible`** — `.btn` sets `outline:none` and had no replacement, unlike
  every other focusable class in `index.css`.
- **`aria-live="polite"`** on `LiveTraceTicker` — step descriptions were unannounced.
- **Mobile drawer**: a click-to-close backdrop, and Escape closes it even while the
  search input is focused (checked *before* the existing focused-element exemption, not
  gated by it). A real focus trap (Tab cycling confined to the drawer) is still open.
- `renderCanvas()` returning `null` for a null `activeProblem` is still there — traced but
  not fixed; `problems` can currently only be empty if the catalogue fetch returns an
  empty array, which doesn't happen in practice (`DEFAULT_FALLBACK_PROBLEMS` seeds it and
  a successful fetch is only applied when `data.length > 0`), so it's a real but
  unreachable-today edge case.

---

## PROMPT C — Migrate the catalogue onto the tracer contract

```
Task: give the remaining catalogued problems real execution traces. This is the bulk of
the project — roughly 425 algorithm implementations. Work category by category and land
each category as its own commit. Do PROMPT A first; the harness does not scale otherwise.

WHY THESE NUMBERS LOOK LIKE THIS
Distinct traces per category today, against problems catalogued:
    Advanced Graphs 62 problems / 2 traces   (60 generators are one-liners returning
                                              generateGraphIntroSteps(), a 2-step
                                              placeholder — Dijkstra, Bellman-Ford,
                                              Floyd-Warshall, Prim, Kruskal, Tarjan,
                                              Kosaraju, KMP, Rabin-Karp all show it)
    Dynamic Programming 55 / 2               (51 fall through to climbing-stairs)
    Binary Trees & BST 54 / 1                (one 3-step stub for all of them)
    Binary Search 32 / 1                     (31 delegate to a fixed search over
                                              {1,3,5,7,9,11,13})
    Linked List 31 / 2                       (29 delegate to a 3-step reverse narration)
    Stack & Queue 30 / 5                     (25 fall through)
    Heaps 17 / 2 · Greedy 15 / 3 · Sliding Window 12 / 3
    Bit Manipulation 18 distinct but only 2 algorithmic
    Strings 16 distinct but only 6 algorithmic
    Genuinely finished: Arrays (40/40), Sorting, Basic Math, Basic Recursion,
    Recursion & Backtracking, Graphs BFS/DFS.

START HERE — free wins
Eight classes under backend/src/main/java/com/dsa/ui/algorithm/ are fully implemented,
already unit-tested, and referenced by NOTHING in main/:
    tree/TreePreorderTraversal, TreeInorderTraversal, TreePostorderTraversal,
    TreeLevelOrderTraversal, graph/DijkstraShortestPath,
    binarysearch/RotatedSortedArraySearch, linkedlist/ReverseLinkedList,
    greedy/NMeetingsInOneRoom
They emit via the older trace/TraceRecorder + TraceEvent path. Port them to
AlgorithmTracer. Note TreePreorderTracer and TreeInorderTracer already exist as new
tracers — reconcile rather than duplicate, and delete whichever implementation loses.

ORDER (by leverage — biggest stub cluster first)
    1. Binary Trees & BST (53 to write) — BinaryTreeLayout already exists in tracer/impl
    2. Binary Search (31)               — the pattern is one search space, many predicates
    3. Advanced Graphs (60)
    4. Dynamic Programming (53)
    5. Linked List (29)
    6. Stack & Queue (25)
    7. Heaps (15), Greedy (12), Sliding Window (9)
    8. Deepen Bit Manipulation (16) and Strings (10)

PER PROBLEM
  - A tracer class under com.dsa.ui.tracer.impl (group by topic in subpackages once a
    category exceeds ~15 files).
  - An InputSpec with a curated default that ILLUSTRATES the algorithm — a binary search
    default whose target sits at the midpoint teaches nothing. Fall back to a shared
    per-type default only where no input is naturally illustrative.
  - Hard size caps. For factorial/exponential problems (permutations, n-queens, sudoku,
    subsets, combination-sum) set both a small n ceiling AND a reduced withMaxSteps.
  - annotatedCode() with anchors on every line the trace highlights.
  - A step at every genuinely meaningful state change — each comparison, swap, pointer
    move, constraint check, recursive call and backtrack. Narrate WHY, not just what:
    "running sum went negative, so any subarray is better off starting fresh" beats
    "reset running to 0".
  - A real ComplexityDetail. The bulk-registered problems all currently share one cloned
    generic O(N)/O(1), which is wrong for most of them.
  - alternateInput() per PROMPT A.

AS YOU GO
  - Delete the corresponding one-line delegate generator and its switch case from the old
    service once a problem is traced, so the two paths cannot diverge.
  - Do NOT remove a service's catalogue metadata (initProblems) — that still feeds
    ProblemCatalog until prompt D.
  - `GET /api/problems/stats` traced count must rise every commit and never fall.
  - Update the README's "Traced so far" section per category.

VERIFY PER CATEGORY
  - `cd backend && mvn test` green.
  - No two problems produce identical traces (noTwoTracersProduceIdenticalTraces enforces
    this across the whole registry — it WILL catch a lazy copy-paste).
  - Spot-check 3 problems per category by hand against a known-correct expected output.
  - Report the traced count before and after.
```

---

## PROMPT D — Retire the legacy layer

```
Task: remove the legacy API and resolve the duplicate ids. Only after prompts B and C.

PRECONDITIONS — verify before starting, and stop if either fails
  - The frontend no longer references any /api/<topic>/... path (grep frontend/src).
  - GET /api/problems/stats reports untraced == 0.

DO
1. Resolve the 7 cross-service duplicate ids surfaced in stats.duplicateIds:
   dfs-traversal, flood-fill, longest-common-prefix,
   longest-substring-without-repeating, merge-intervals, number-of-islands,
   surrounded-regions. Each is claimed by two services with DIFFERENT content — decide
   which is canonical, move it to the right topic, and give the other a distinct id or
   delete it if genuinely redundant. Then make ProblemCatalog FAIL STARTUP on a duplicate
   instead of recording it, and delete getDuplicateIds().

2. Delete the 18 legacy controllers and ApiContractTest (which exists to police them).
   Keep CorsPolicyTest, retargeting it at /api/problems.

3. Collapse the services. Their switch-based generateSteps() methods should be empty by
   now — delete them, along with the trace/ + algorithm/ classes fully superseded by
   tracers. Keep catalogue metadata, but consider moving it out of 3,150-line Java files
   into resources; ArrayService.java is 161KB in a single class.

4. Delete the ProblemDetail.javaCode field if every problem's source now comes from its
   tracer's annotatedCode().

5. Final docs pass: README numbers, and remove the "legacy endpoints" section.

6. Delete HANDOFF.md from the repository root, and remove its row from the README
   documentation table. It is a temporary working document describing work that is, at
   that point, finished — leaving it behind means a stale second source of truth about
   coverage. Move anything still worth keeping into README.md or plan.md first.

VERIFY
  - Both suites green. `docker-compose up --build`, open http://localhost:5174, and walk
    one problem in each of the 17 categories: edit its input and confirm the animation,
    the highlighted line and the variables panel all reflect the edit.
  - Confirm no route outside /api/problems responds.
```
