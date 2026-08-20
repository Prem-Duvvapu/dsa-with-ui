# DSA Visualizer — handoff prompts for remaining work

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
> Snapshot taken at: 433 catalogued / 8 traced, on branch
> `fix/trace-contract-and-phase-0`. If those numbers no longer match
> `GET /api/problems/stats`, treat this file with suspicion.

Five prompts. **Z first** (it gates every later merge). **A must land before C.**
B and C can run in parallel by different agents. D is last.

| | Prompt | Depends on | Size |
|---|---|---|---|
| Z | CI workflow gating merges to main | — | small |
| A | Scale the test harness | — | small |
| B | Frontend restructure onto the v2 API | — | medium |
| C | Migrate the catalogue onto the tracer contract | A | very large |
| D | Retire the legacy layer | B, C | small |

---

## SHARED CONTEXT — paste at the top of every prompt

```
Repo: /mnt/c/Users/Hp/OneDrive/Desktop/dsa-with-ui
Branch to start from: fix/trace-contract-and-phase-0
Backend: Spring Boot 3.2.3 / Java 17, port 8923.  Frontend: React 18 + Vite, dev 5180, docker 5174.
Build: `cd backend && mvn test` (308 tests, green)  ·  `cd frontend && npx vitest run` (16 tests, green)

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
        InputSpec inputSpec();                  // declared inputs, bounds, defaults
        String annotatedCode();                 // Java source carrying `// @a name` anchors
        void run(Inputs in, StepEmitter emit);  // RUNS THE REAL ALGORITHM
    }

  - Tracers are Spring @Components, discovered by TracerRegistry, indexed by id().
    They MUST be stateless — one instance serves all concurrent requests.
  - TracerRegistry has NO FALLBACK. Unknown id => 404. Never substitute another trace.
  - Duplicate ids fail application startup.

  StepEmitter is fluent and names code lines by ANCHOR, never by line number:
        emit.using("Array");                    // dsType for the canvas
        emit.at("loop.compare")
            .say("i = %d: sum %d beats best %d", i, sum, best)
            .var("i", i).var("best", best)
            .array(nums, i)                     // or .grid / .list / .tree / .nodes / .edges
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

CURRENT NUMBERS (pinned by ProblemsApiTest — update the test if you change them)
    440 id registrations across 18 services, 433 unique, 7 claimed by two services.
    8 traced: two-sum, kadane-algo, binary-search-1d, tree-preorder, tree-inorder,
              reverse-linked-list, bfs-traversal, number-of-islands.

STILL PRESENT ON PURPOSE
  - The 18 legacy per-topic controllers (/api/arrays/..., /api/trees/...) still serve the
    old paths. The frontend still uses them. Do not delete until prompt D.
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

## PROMPT Z — CI workflow (do this first; it gates everything after)

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

## PROMPT A — Scale the test harness

```
Task: make the tracer test harness workable at 400+ tracers, then add the coverage
tooling the plan calls for.

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

VERIFY
  - `cd backend && mvn test` green, and the total is >= 308.
  - Prove the harness still works: temporarily rewrite KadaneTracer.run to emit two fixed
    steps ignoring its input. traceRespondsToItsInput must fail and name kadane-algo.
    Restore it. Show me the failure output.
  - Confirm adding a tracer now requires touching only its own file plus its golden file.
```

---

## PROMPT B — Frontend restructure onto the v2 API

```
Task: restructure the React frontend onto the v2 API and add the input editor. Keep
React + Vite; restructure, do not rewrite.

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
  - Do not change the backend at all. If you need an API change, stop and say so.

VERIFY
  - `npx vitest run` green; add tests for useTrace (abort + stale response), the input
    panel (renders from a mocked inputSpec, surfaces field errors), the untraced 501
    state, and a smoke render per canvas.
  - `npx vite build` clean.
  - Full keyboard-only traversal of the catalogue, which is impossible today.
  - Check at 320px width.
  - Run against a live backend (`cd backend && mvn spring-boot:run`) and confirm editing
    an input changes the animation, the highlighted code line, and the variables panel.
```

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
