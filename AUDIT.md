# Full-project audit — 433 problems, 18 topics *(as of 2026-09-12)*

**Date:** 2026-09-12   **Branch:** `audit/full-project-sweep`   **Baseline commit:** `6faea6f`

Every number below was read from the running backend or from the source, never from another
document. Reproduce with the commands in [§9](#9-how-to-reproduce).

> ### Read this first — the catalogue has changed since this audit
>
> This is a **record of one moment**, and its figures are deliberately left as they were
> measured. The catalogue has moved since:
>
> | | At audit (2026-09-12) | Now (2026-09-13) |
> |---|---|---|
> | Problems | 433 | **431** |
> | Topics | 18 | **17** |
> | Duplicate ids | 7 | **0** |
>
> Eleven problems were catalogued twice — seven under identical ids and four more under
> word-order variants (`rotten-oranges` / `rotting-oranges`) that `duplicateIds` could not
> see. All eleven came from `AdvancedGraphService` and `GraphBfsDfsService` overlapping, so
> those two topics were merged into one **Graphs** topic, which removed the cause rather
> than the symptom. Two of the four near-duplicate pairs survive on purpose, teaching the
> same problem two standard ways; see [F10](#4b-status--what-has-been-fixed).
>
> Wherever this document says "Advanced Graphs" or "Graph BFS/DFS", read **Graphs**. The
> per-topic tables in [§2](#2-coverage--confirmed-complete) and [F2](#f2-93-problems-never-highlight-some-of-their-code-on-the-default-input)
> are the audit-time split and are kept for the record.
>
> **Never quote a coverage number from this file.** Run
> `curl -s localhost:8923/api/problems/stats`.

---

## 1. Executive summary

The migration is **functionally complete and green**, and the audit found **no problem that
animates another problem's algorithm through the v2 API**. That was the defect this
architecture was built to kill, and on `/api/problems` it is dead.

```
GET /api/problems/stats
  catalogued 433   traced 433   untraced 0   orphanedTracerIds []   duplicateIds 7
```

| Suite | Result |
|---|---|
| `mvn test` (backend) | **6434 passed**, 0 failures, 352 skipped |
| `npx vitest run` (frontend) | **220 passed** in 28 files |
| `npx vite build` | clean, 289 kB / 85 kB gzip |

**But the test suites are green while 106 of 433 problems (24.5%) carry a defect**, and
16 of 18 topics fail the repo's own `audit_topic.py` sweep. That gap is the finding. It is
the same shape as the original incident recorded in `CLAUDE.md`: 303 broken problems once
sat under 90 green tests because the only per-problem assertion was `!steps.isEmpty()`.
The assertions have improved enormously since, but they still cannot see a dead anchor, an
unexercised branch, or a legacy `default:` that is once again serving substitute steps.

| # | Finding | Severity | Scope |
|---|---|---|---|
| [F1](#f1-live-fallbacks-in-12-legacy-services) | 12 legacy services still return another algorithm's steps from `default:` | **High** | 12 services, 5 live ids |
| [F2](#f2-93-problems-never-highlight-some-of-their-code-on-the-default-input) | 93 problems never highlight some code on the *default* input (contract holds) | Low — **fixed** | 93 problems |
| [F3](#f3-64-dead-legacy-generators) | 64 unreachable legacy generator methods | **Medium** | 8 services |
| [F4](#f4-nine-recursion-traces-freeze-mid-unwind) | 9 recursion traces end with a non-empty call stack | **Medium** | 9 problems |
| [F5](#f5-nine-of-seventeen-dstypes-have-no-payload-check) | 9 of 17 `DsType`s have no payload contract check | **Medium** | 170 problems unchecked |
| [F6](#f6-claudemd-understates-coverage-by-389-problems) | `CLAUDE.md` says "44 of 433 traced" — it is 433 | **Medium** | docs |
| [F7](#f7-dsucanvas-silently-falls-back-to-a-fake-state) | `DsuCanvas` silently renders a fabricated DSU on key drift | **Medium** | 3 problems |
| [F8](#f8-one-near-identical-trace-pair) | `postfix-to-prefix` / `prefix-to-postfix` traces 0.983 similar | **Low** | 2 problems |
| [F9](#f9-set-unset-rightmost-bit-demonstrates-half-its-problem) | `set-unset-rightmost-bit` emits 1 step, teaching half the problem | **Low** | 1 problem |
| [F10](#f10-seven-duplicate-catalogue-ids-remain-unresolved) | 7 duplicate catalogue ids, pinned rather than resolved | **Low** | 7 ids |

**Verdict by the `audit-topic` rubric:** 2 topics complete (Sorting Algorithms, Tries &
Prefixes), 16 topics **not done**. No topic is "honestly partial" — coverage is 100%
everywhere; every failure is a quality defect, not a missing tracer.

---

## 2. Coverage — confirmed complete

All 433 catalogued ids resolve to a tracer, all 433 have a golden file, and every tracer id
is unique.

| Check | Expected | Actual | Status |
|---|---|---|---|
| Catalogued ids | 433 | 433 | OK |
| Traced | 433 | 433 | OK |
| Untraced | 0 | 0 | OK |
| Distinct `id()` in `tracer/impl/` | 433 | 433, no duplicates | OK |
| Golden files in `src/test/resources/golden/` | 433 | 433 | OK |
| `orphanedTracerIds` | empty | empty | OK |
| Duplicate catalogue ids | 7 (pinned) | 7 | see [F10](#f10-seven-duplicate-catalogue-ids-remain-unresolved) |

Per-topic totals — all 100% traced:

| Topic | n | Clean | Flagged |
|---|---:|---:|---:|
| Advanced Graphs | 53 | 30 | 23 |
| Arrays | 40 | 30 | 10 |
| BST | 16 | 6 | 10 |
| Binary Search | 32 | 18 | 14 |
| Binary Trees | 38 | 35 | 3 |
| Bit Manipulation | 18 | 16 | 2 |
| Dynamic Programming | 55 | 53 | 2 |
| Graph BFS/DFS | 7 | 2 | 5 |
| Greedy Algorithms | 14 | 9 | 5 |
| Heaps & PriorityQueue | 17 | 16 | 1 |
| Learn the Basics | 14 | 6 | 8 |
| Linked List | 31 | 29 | 2 |
| Recursion & Backtracking | 25 | 20 | 5 |
| Sliding Window | 12 | 11 | 1 |
| **Sorting Algorithms** | 5 | 5 | **0** |
| Stack & Queue | 30 | 14 | 16 |
| Strings | 24 | 24 | **0** |
| **Tries & Prefixes** | 2 | 2 | **0** |
| **Total** | **433** | **327** | **106** |

Strings and Arrays are clean on the sweep but are the two largest contributors to [F3](#f3-64-dead-legacy-generators) — 15 and 22 dead generators respectively.

---

## 3. Findings

### F1. Live fallbacks in 12 legacy services

**Severity: High.** This is the original defect, alive again in the legacy tier.

`CLAUDE.md` rule 1 is "No fallback, anywhere." The v2 API honours it. Twelve of the
eighteen legacy services do not: their `switch (problemId)` still ends in a `default:` that
**returns another algorithm's steps**.

```java
// AdvancedGraphService.java:94
default: return generateGraphIntroSteps();
// BinarySearchService.java:69
default: return generateBs1dSteps();
// GraphBfsDfsService.java:39
default: return generateIslandsSteps();
```

Because every id is now traced, a `default:` that returns steps has no legitimate consumer
left — it exists only to serve a wrong animation to whatever id someone forgets to retire.
Five ids are being served that way **right now**:

| id | Legacy endpoint | Status | Actually serves |
|---|---|---|---|
| `dfs-traversal` | `/api/graphs/advanced/execute/…` | **200** | graph-intro steps |
| `dijkstra-min-heap` | `/api/graphs/advanced/execute/…` | **200** | graph-intro steps |
| `number-of-islands` | `/api/graphs/advanced/execute/…` | **200** | graph-intro steps |
| `number-of-islands` | `/api/graphs/bfs-dfs/execute/…` | **200** | `generateIslandsSteps()` (explicit `case`, line 31) |
| `binary-search-1d` | `/api/binarysearch/execute/…` | **200** | `generateBs1dSteps()` |

`GraphBfsDfsService` additionally keeps three explicit step-returning cases —
`number-of-islands` (:31), `flood-fill` (:33), `surrounded-regions` (:38) — that were never
retired.

State of all 18 services:

| Service | `default:` |
|---|---|
| ArrayService, SlidingWindowService, SortingService | throws `LegacyTraceRetiredException` — correct |
| DpService, LinkedListService, StackQueueService | no `default:`; throws unconditionally — correct |
| **AdvancedGraph, BasicMath, BasicRecursion, BinarySearch, BitManipulation, GraphBfsDfs, Greedy, Heap, RecursionBacktracking, String, Tree, Trie** | **returns steps — 12 services** |

**Fix.** Replace every step-returning `default:` with
`throw new LegacyTraceRetiredException(problemId);`, and retire the three explicit
`GraphBfsDfsService` cases. Then repoint `ProblemsApiTest.legacyEndpointsUnaffected`
(`ProblemsApiTest.java:174`) — it currently asserts `/api/arrays/problems` is 200 and
`/api/dp/execute/longest-common-subsequence` is 410, so it survives this change, but the
per-topic service tests must gain `assertThrows` coverage for the newly retired ids.

RED-first proof, per `prove-the-test-fails`: assert 410 for
`/api/graphs/advanced/execute/dijkstra-min-heap` and watch it fail with 200 against today's
code before applying the fix.

---

### F2. 93 problems never highlight some of their code on the default input

**Severity: Low** — corrected down from Medium after verification; see the correction note.

> **Correction.** An earlier draft of this finding called these "dead anchors" and implied a
> broken contract. That was wrong. `TracerContractTest.anchorsAreAllReachable` checks
> reachability across the **default *or* the alternate** input, and it passes for all 433
> problems — so **no anchor is unreachable**, and nothing is invisible to CI. Verified by
> re-running that test restricted to the default input: it fails on exactly 93 problems,
> matching the sweep, and passes for all 433 when the alternate is included.
>
> What is real: on the **default** input — the only thing a learner sees before touching the
> input panel — those 93 problems show code lines the animation never highlights. That is a
> question about *default choice*, not a contract violation.

Per topic:

| Topic | Problems with code unhighlighted on defaults |
|---|---:|
| Advanced Graphs | 20 / 53 |
| Stack & Queue | 15 / 30 |
| Binary Search | 13 / 32 |
| Arrays | 10 / 40 |
| BST | 10 / 16 |
| Greedy Algorithms | 5 / 14 |
| Graph BFS/DFS, Recursion & Backtracking | 4 each |
| Binary Trees | 3 / 38 |
| Bit Manipulation, Dynamic Programming, Linked List | 2 each |
| Heaps, Learn the Basics, Sliding Window | 1 each |
| Sorting, Strings, Tries | 0 |

**These are not 93 independent bugs.** They cluster into two dominant causes, and the
repeated anchor names are the tell that one batch of tracers shares one mistake:

**Cause A — the failure branch is never exercised (23 instances).** The default input always
succeeds, so "not found" / "unreachable" / "no cycle" never runs:
`unreachable` ×3, `noCycle` ×4, `miss` ×4, `cycleDetected` ×3, `impossible` ×2,
`exhausted` ×2, plus `none`, `invalid`, `stranded`, `insufficient`, `noSegmentation`,
`deadEnd`, `orphan`, `unreached`.

> `two-sum` is the clean illustration. Its 5 steps highlight lines 2, 4, 6, 8 — the
> `none` anchor on line 10 is unreachable because the default input always finds a pair.
> The learner never sees what failure looks like.

**Cause B — empty-container guards on the four stack/queue implementation problems
(11 instances).** `stack-array-impl` (`peekEmpty`, `underflow`), `stack-ll-impl`
(`peekEmpty`, `popEmpty`), `stack-queue-impl` (`popEmpty`, `topEmpty`), `queue-array-impl`
(`frontEmpty`, `underflow`), `queue-ll-impl` (`frontEmpty`, `underflow`),
`queue-stack-impl` (`peekEmpty`). One bug written six times: no default input ever pops an
empty container.

**Fixed, by option 3 below.** Three options were on the table, all pedagogical trade-offs
rather than bug fixes:

1. **Leave them.** The alternate input already demonstrates the other branch, and the input
   panel is how a learner reaches it. Closes F2 as working-as-intended.
2. **Change the defaults** for the subset where the unexercised branch *is* the lesson.
   Costs a golden regeneration each, risks a worse first impression, and requires 93
   per-problem judgements that nobody had a stated principle for.
3. **Surface it in the UI.** Fixes all 93 without compromising a single default.

Option 3 shipped. The code panel now marks any `// @a` anchored line the current run never
visited — a dotted rule, a `◦` gutter glyph, dimmed text, and a header count reading
"N branches not taken". The data was already on the wire: the trace carries its anchors,
and nothing had ever read them.

The point is the reframing. An unhighlighted branch left unmarked reads as if the animation
skipped something. Marked, it says the true thing — **this branch exists and your input did
not take it** — which turns the gap into the lesson, since which branches run is a property
of the data rather than of the visualiser. It also points at the input editor, so the
learner has somewhere to go.

Verified against the real problems this finding named: `two-sum` marks `none`,
`stack-array-impl` marks `underflow` and `peekEmpty`, `undirected-cycle-dfs` marks
`cycleDetected`.

Full per-problem list: [Appendix A](#appendix-a--every-problem-by-topic).

---

### F3. 64 dead legacy generators

**Severity: Medium.** Unreachable code that keeps a wrong animation alive in the repository,
ready to be wired back in — exactly what `audit-topic` §5 warns about.

| Service | Dead / total |
|---|---|
| `ArrayService` | **22 / 22** — every generator is unreachable |
| `StringService` | 15 / 16 |
| `AdvancedGraphService` | 11 / 13 |
| `BasicMathService` | 6 / 7 |
| `BasicRecursionService` | 6 / 7 |
| `SlidingWindowService` | 2 / 2 |
| `GreedyService` | 1 / 3 (`generateJobSequencingSteps`) |
| `TrieService` | 1 / 2 (`generateLcpSteps`) |
| **Total** | **64** |

`ArrayService` and `SlidingWindowService` are the cleanest cases: both already throw from
`default:`, so *all* their generators are dead and can be deleted outright with no
behavioural change. In `AdvancedGraphService` all 11 dead methods are one-line delegates to
`generateGraphIntroSteps()` — the exact "delegate method" pattern that produced the original
303-problem incident.

**Fix.** Delete them. Do `ArrayService` and `SlidingWindowService` first (zero risk,
zero coupling), then the rest **after** F1 lands, since retiring a `default:` turns more
generators dead and the two changes should be sequenced to keep each PR small.

---

### F4. Nine recursion traces freeze mid-unwind

**Severity: Medium.** The last step has a non-empty `callStack`, so the sidebar freezes
showing frames the viewer never watches drain.

| id | Topic | Depth left |
|---|---|---:|
| `subsequences-patterns-theory` | Recursion & Backtracking | 4 |
| `palindrome-string-recursion` | Learn the Basics | 3 |
| `bst-delete` | BST | 1 |
| `factorial-number` | Learn the Basics | 1 |
| `fibonacci-recursion` | Learn the Basics | 1 |
| `print-1-to-n` | Learn the Basics | 1 |
| `print-n-to-1` | Learn the Basics | 1 |
| `reverse-array-recursion` | Learn the Basics | 1 |
| `sum-first-n` | Learn the Basics | 1 |

**Learn the Basics is 7 of the 9** — one batch, one omission. The pushes and pops balance;
what is missing is a summary step emitted after unwinding. `Subsets2Tracer` already does
this correctly (emits a closing step from `run()` after `backtrack` returns, ending at
depth 0) and is the pattern to copy.

The `depth 4` case is different in kind and should be judged separately —
`subsequences-patterns-theory` may be truncating its narration, not just missing a closer.

**Fix.** Emit a terminal step from `run()` after the recursion returns, stating the result.
Regenerate the 9 goldens and read each diff.

---

### F5. Nine of seventeen `DsType`s have no payload check

**Severity: Medium.** This is the hole that lets the other findings hide.

`DsTypePayloadContractTest.REQUIRED` maps only 8 of 17 `DsType` values to a required field.
Every other type hits `assumeTrue(requirement != null, …)` and **skips** — which is a large
share of the 352 skipped backend tests.

| Checked (8) | Unchecked (9) |
|---|---|
| `MATRIX`, `LINKED_LIST`, `TREE`, `GRAPH`, `DP_TABLE`, `TRIE`, `STACK`, `QUEUE` | **`ARRAY`, `BITS`, `STRING`, `DSU`, `HEAP`, `INTERVAL`, `RECURSION_TREE`, `SEARCH_SPACE`, `WINDOW`** |

**170 of 433 problems** declare an unchecked type. As `audit-topic` §4 puts it: an
`ARRAY`-tagged tracer that never calls `.array(...)` passes every test and renders nothing.

**Good news:** I ran that check empirically against all 170. Only **3** fail — the DSU
trio in [F7](#f7-dsucanvas-silently-falls-back-to-a-fake-state). The remaining 167 do emit
`arrayState`/`callStack`. So this is a **latent** hole, not an active outage: the contract
is unenforced, but the tracers currently honour it anyway.

**Fix.** Add the missing `REQUIRED` entries so the invariant is enforced rather than
coincidental — `ARRAY`/`BITS`/`STRING`/`HEAP`/`INTERVAL`/`SEARCH_SPACE`/`WINDOW` →
`arrayState`, `RECURSION_TREE` → `callStack`, `DSU` → its own predicate (see F7). Confirm
each addition goes RED against a deliberately blanked tracer before accepting it.

---

### F6. `CLAUDE.md` understates coverage by 389 problems

**Severity: Medium** — `CLAUDE.md` is the file whose instructions override default
behaviour, so a stale claim there misleads every future session.

```
CLAUDE.md:98   "Currently **44 of 433** are traced."     →  actual: 433 of 433
```

The same section's framing ("The UI is meant to say 'not yet traced'") describes a state
that no longer exists — there are zero untraced problems.

Reconciled against `/api/problems/stats`:

| Document | State |
|---|---|
| `README.md` | **Accurate.** Line 9 already says "all 433 with real execution traces." |
| `PROJECT_COMPLETION_PLAN.md` | **Accurate.** Line 306 records 433/433, line 476 pins the expectation. |
| `CLAUDE.md:98` | **Stale — fix required.** |
| `HANDOFF.md:10` | Stale framing: calls out Tries (2/2) and Strings (24/24) as the fully-traced topics, which was true at the time and is now true of all 18. |

`HANDOFF.md` is marked "temporary — delete when the migration completes". The migration
*is* complete (433/433); its Prompt C (migrate ~425 problems) is done. Prompts A, B and D
are not, so the file should be reduced to those rather than deleted.

**Fix.** Update `CLAUDE.md:98` to "all **433 of 433** are traced", rewrite the surrounding
honesty-flag paragraph in the past tense, and prune `HANDOFF.md` to the open prompts.

---

### F7. `DsuCanvas` silently falls back to a fake state

**Severity: Medium.** A no-fallback repo with a fallback in the render path.

`DsuCanvas.jsx:18-21` reads four **magic string keys** out of `step.variables` and defaults
each to a hardcoded literal:

```jsx
const parentStr  = variables['parent[]']      || '[0, 1, 2, 3, 4, 5, 6, 7]';
const rankStr    = variables['rank[]']        || '[0, 0, 0, 0, 0, 0, 0, 0]';
const dsuSetsStr = variables['Disjoint Sets'] || '{1}, {2}, {3}, {4}, {5}, {6}, {7}';
const dsuOpStr   = variables['Operation']     || 'Initialize DSU(7)';
```

If a tracer renames a key — or emits a step without it — the canvas draws a **fabricated
7-element DSU** that looks plausible and is entirely fictional. Nothing fails; the user is
shown a lie. This is structurally the same failure as the old `default:` branch, moved to
the frontend.

It is also why the three DSU problems (`disjoint-set-dsu`, `accounts-merge`,
`most-stones-removed`) emit no structural payload at all: their state is smuggled through
`variables` as human-readable strings and re-parsed with a regex in the browser.

**Fix.** Two steps, in order:
1. Make the fallback loud — render an explicit "DSU state unavailable" panel instead of a
   fabricated default, matching how an unknown `dsType` already renders an unsupported
   state. Add a `DsuCanvas` test asserting the fabricated default never appears.
2. Properly, give DSU a real payload field (or a documented `variables` contract pinned by
   a cross-tier test like the `DsType` registry test), so the string-parsing round trip is
   not the transport.

---

### F8. One near-identical trace pair

**Severity: Low.**

```
postfix-to-prefix  <->  prefix-to-postfix    similarity 0.983
```

`TracerContractTest.noTwoTracersProduceIdenticalTraces` only fires on *exact* equality, so
this passes while being pedagogically interchangeable. This is the only pair above 0.98 in
the whole catalogue — a good result across 433 problems.

**Fix, per `audit-topic` §6:** do **not** perturb a value until the ratio drops. Make the
two narrate different things — the postfix→prefix conversion should talk about scanning
left-to-right and prepending operators, the prefix→postfix about scanning right-to-left and
appending them.

---

### F9. `set-unset-rightmost-bit` demonstrates half its problem

**Severity: Low**, but it is the single weakest trace in the catalogue.

```
1 step.  anchors: {setRightmostUnset: 3, unsetRightmostSet: 5}   lines used: [3]
  L3  "N + 1 = 12 + 1 = 13 carries a 1 into the rightmost 0 bit. N | (N + 1) = 12 | 13 = 13."
```

The problem is *set and unset* the rightmost bit. The trace performs only the set half; the
`unsetRightmostSet` anchor on line 5 is dead, so the `n & (n-1)` half — the part the title
promises and the part that matters — is never shown.

**Fix.** Emit the second operation as its own step. Per `audit-topic` §6, say explicitly
what `n & (n-1)` means *for this question*, since `check-power-of-2` and
`count-set-bits` hinge on the same expression.

For context, the 55 other traces at ≤6 steps were spot-checked and are mostly legitimate —
`check-number-odd` at 2 steps (`N & 1`, verdict) uses both its anchors and is genuinely a
two-step problem. Shallowness alone is not a defect; a dead anchor alongside it is.

---

### F10. Seven duplicate catalogue ids remain unresolved

**Severity: Low** — surfaced rather than hidden, which is the designed behaviour, but pinned
at 7 for long enough that it reads as permanent.

| id | Loser (claimed second) |
|---|---|
| `number-of-islands`, `surrounded-regions`, `flood-fill`, `dfs-traversal` | `GraphBfsDfsService` |
| `merge-intervals` | `GreedyService` |
| `longest-substring-without-repeating` | `StringService` |
| `longest-common-prefix` | `TrieService` |

Three of these (`number-of-islands`, `dfs-traversal`, `flood-fill`) overlap with
[F1](#f1-live-fallbacks-in-12-legacy-services) — the duplicate registration is *why* two
controllers answer for the same id.

**Fix.** Decide per id whether the losing registration should be deleted or the problem
genuinely belongs in both categories under distinct ids. Either way the pinned `7` in
`ProblemsApiTest` moves deliberately, with the `README.md` coverage table updated in the
same commit.

---

## 4. What is solidly right

Worth recording, because the audit was looking for these and did not find them:

- **No v2 fallback.** No id on `/api/problems` serves another problem's steps. 404/501
  discipline holds. `orphanedTracerIds` is empty.
- **No truncation.** Not one of the 433 traces hits the 5000-step budget on its defaults.
- **No duplicate tracer ids.** 433 distinct `id()` values across 385 files.
- **Golden coverage is total.** 433 golden files for 433 problems.
- **`dsType`s are per-problem, not blanket.** The bulk-registration smell from the
  Recursion & Backtracking incident has not recurred: R&B now spans 6 types
  (`Stack` 12, `String` 5, `Matrix` 4, `Bits` 2, `Graph` 1, `Array` 1). The four
  single-type topics — BST/`Tree`, Binary Trees/`Tree`, Linked List/`LinkedList`,
  Tries/`Trie` — are legitimately homogeneous.
- **Trace distinctness holds.** Exactly one near-identical pair in 433 problems.
- **Canvas registry is complete.** All 17 `DsType` values route to a renderer in
  `canvas/registry.js`; the cross-tier test guards drift.

---

## 4b. Status — what has been fixed

All on branch `audit/full-project-sweep`, one commit per finding, each proven RED first
where a new assertion was involved.

| Finding | State | Commit |
|---|---|---|
| F1 — legacy fallbacks | **Fixed.** All 18 services throw; 7 live id/route pairs closed. Guarded by `everyLegacyExecuteRouteIsRetired` over each controller's own catalogue. | `07d4053` |
| F3 — dead generators | **Fixed.** All 80 deleted (1667 lines). | `8d212cd` |
| F6 — stale docs | **Fixed.** `CLAUDE.md` + `HANDOFF.md` corrected. | `8959eff` |
| F5 — payload contract | **Fixed.** 9 missing `REQUIRED` entries added; skipped 170 → 0. | `0082292` |
| F7 — DsuCanvas fallback | **Fixed.** Renders an explicit unavailable state; 3 RED-first guards. | `0082292` |
| F4 — mid-unwind recursion | **Fixed.** New `traceEndsWithAnEmptyCallStack` contract test caught **12**, not 9 — the per-topic sweep missed `cycle-directed-dfs`, `directed-cycle-dfs`, `dfs-traversal`. All 12 closed, goldens read. | `8c3ff36` |
| F8 — near-identical pair | **Fixed.** Similarity 0.983 → 0.247 by changing what each narrates. | `8dff996` |
| F9 — 1-step trace | **Fixed.** Now 3 steps showing the carry/borrow mechanism. | `8dff996` |
| F2 — defaults | **Fixed.** The code panel marks branches the current input never took, rather than changing 93 defaults. | `codeviewer` |
| F10 — duplicate ids | **Fixed.** All 11 duplicated problems resolved and the two graph topics merged; 433 → 431, duplicates 7 → 0. | `210f3ac` |

Three additional defects were found *by the fixes*, not by the original sweep:

- **`traceEndsWithAnEmptyCallStack` found 3 more mid-unwind traces** than the topic sweep,
  because the sweep is per-topic and these were graph traversals nobody classed as recursion.
- **`cycle-directed-dfs` and `directed-cycle-dfs` are near-duplicate tracers** — identical
  `run()` bodies differing only in default graph size. The topic sweep compares within a
  topic, so a cross-topic pair like this is invisible to it. Not yet addressed; it is the
  same class as F8.
- **Three stale hand-maintained retirement lists** (a 430-entry `RETIRED_IDS` and per-topic
  `retired` sets in 8 service tests) were what let F1's stragglers hide. Replaced with
  assertions over the whole catalogue.

**The legacy layer has since been deleted outright** (§7 Q1, answered: yes). That removes
F1's and F3's subject matter entirely rather than maintaining the repairs:

- 18 legacy controllers deleted — `/api/{topic}/...` routes now **404**, asserted by
  `ProblemsApiTest.legacyRoutesNoLongerExist`.
- `generateSteps` stripped from all 18 services; `LegacyTraceRetiredException` and its
  handler deleted.
- 11 orphaned `algorithm/` classes and 2 `trace/` classes deleted. `algorithm/trie/
  ImplementTrie` survives — `ImplementTrieTracer` uses it.
- 18 copy-pasted `*ServiceTest` classes and `ApiContractTest` replaced by one
  `ProblemProviderContractTest` (72 cases) over the providers Spring actually registers.

**Scope correction:** the 18 `service/*Service` classes could *not* be deleted. They are
the catalogue — every one of the 433 `ProblemDetail`s lives in their `initProblems()`, and
`ProblemCatalog` merges them via `ProblemProvider`. They survive as catalogue providers
with no step generation. Deleting one deletes that topic's catalogue.

### Work that followed the audit

The findings above are all closed. What came after them, in order, each with its own commit:

| Area | Change |
|---|---|
| Docs & process | `REVIEW.md` — six review gates; `RCA-025`…`RCA-033` recorded in `RCA.md`; this file reframed as a record |
| Problem statement | Descriptions and source constraints surfaced; 17 descriptions that stated the *technique* rewritten to state the *question* |
| Workspace | Code moved beside the canvas; input editor, complexity card and statement made collapsible and persisted |
| Player | Media-player keyboard with a discoverable `?` overlay; view preferences persist |
| Theme | Light/dark/system control; 94 canvas literals tokenised, fixing a real contrast defect in the dark theme |
| Onboarding | First-run welcome; a guided tour anchored to `data-tour` attributes with a test asserting every step targets something real |
| Engineering | `aria-live` data summary + top-level error boundary; gzip + weak ETag + cached projection (236 KB → 31 KB); skip-erosion and wire-contract guards; `App.jsx` split; `/execute` rate limited |
| Catalogue | 11 duplicated problems resolved; the two graph topics merged into one |
| Canvases | `WindowCanvas`, `SearchSpaceCanvas`, `HeapCanvas` built; recursion trees derived from call stacks; DP dependencies and recurrences completed |
| Portability | `start.sh` fixed for macOS — no `setsid`, and bash 3.2 has no `wait -n` |

Suites at the end of that work: backend **6415 pass / 0 fail**, frontend **333 pass /
44 files**, `vite build` clean. Live: `/api/problems/stats` reports **431/431/0** with no
duplicate ids, every legacy route 404s, and traces render.

See `ARCHITECTURE.md` for the system as it now stands.

### Per-topic sweeps

The findings above came from a repo-wide pass. A second pass walks one topic at a time with
`.claude/skills/audit-topic`, which looks for what a per-problem check cannot see. Each
topic below has been swept; what it found is listed, and the appendix tables are the
original snapshot rather than the current state.

| Topic | What the sweep found |
|---|---|
| Arrays | `next-permutation`'s default was the *last* permutation, so the algorithm skipped its own body — and no default with a pivot can satisfy `stepCountGrowsWithInput` (RCA-019) |
| Stack & Queue | the stack blinked empty between every push — a structure restated only on the steps that changed it (RCA-034) |
| Binary Trees / BST | clean on its own terms; the RCA-035 check run alongside it found seven canvases drawing catalogue defaults over the caller's input, 1506 steps in 142 problems |
| Linked List | doubly linked lists drawn with only half their pointers |
| Graphs | Kahn's-algorithm cycle default skipped the entire algorithm |
| Binary Search | five separate defects, below |
| Dynamic Programming | three, below — none in the arithmetic, all in what the picture showed |
| Greedy Algorithms | three, below — the same "canvas pointed at the wrong structure" shape, found deliberately |
| Heaps & PriorityQueue | four, below — including two tracers not running the algorithm on screen beside them |
| Recursion & Backtracking | three, below — two defaults that never backtracked, and two problems with no visible recursion |

**Binary Search — 32/32 traced, five findings, all fixed.** None was visible to any existing
test, and three of the five were a blank or lying picture rather than a wrong trace:

1. `count-occurrences`, `first-last-occurrence` and `floor-ceil-sorted-array` rendered
   **"No search range for this step." on every step of every run** — they named only the
   bound that had just moved, and `SearchSpaceCanvas` reads the pair off one step on purpose
   (RCA-036).
2. `median-2-sorted-arrays` and `kth-element-2-sorted-arrays` stated a range that never
   moved: both defaults validated on their first partition. Their cells row was
   `concat(a, b)` built *after* the swap, so cell 0 changed which array it meant mid-trace.
3. `SearchSpaceCanvas` decided per step whether the range indexed the cells or named
   candidate answers, and flipped mid-animation — `aggressive-cows` began calling a distance
   an index once `high` fell below the cell count.
4. `binary-search-1d` targeted the last element, so the canonical binary search only ever
   moved right.
5. `search-rotated-sorted` never took its sorted-right-half branch, and
   `search-rotated-sorted-2` never took the duplicate-shrink branch that is the only thing
   distinguishing it from `search-rotated-sorted`.

Guarded by `SearchSpaceContractTest` (both bounds on one step; the default range must move)
and a `SearchSpaceCanvas` mode-stability test, each proven RED first.

**Dynamic Programming — 55/55 traced, three findings, all fixed.** The mechanical sweep came
back almost clean: every id traced, every legacy route 404, two dead anchors (`zero`,
`odd`) that are early-exit guards their alternates reach and that no single run can reach
alongside the main body. Every table animates — across all 55, exactly one pair of
consecutive frames is identical. The defects were all in what the canvas showed:

1. The **recurrence panel blinked out** on 164 of 1133 table steps across 26 problems
   (`print-lis` on 30 of its 47). `formula` is a constant of the problem and `substitution`
   is one step's arithmetic; the canvas demanded both from the current step (RCA-037).
2. `knapsack-01` and `unbounded-knapsack` **drew unwritten `int[][]` memory as settled
   values** — item 4 at capacity 5 read `0` on the first frame and finishes at `13`, in the
   same glyph as a computed cell (RCA-038).
3. `max-rectangle-area-all-ones` declared `MATRIX` and drew a board that never changes,
   while **60 of its 62 steps** narrate a column-height histogram no Matrix hero can render
   (RCA-039).

Two probes were run and rejected rather than acted on, and both are recorded so they are not
retried: "a cell that later changes value must not have been presented as settled" flags
five correct tracers whose `dp[]` legitimately starts at a lower bound of 1, and "the
declared dsType's field must not be out-counted by another structure field" flags 33 tracers
that are almost all correct.

**Greedy Algorithms — 14/14 traced, three findings, all fixed.** The sweep was clean: every
id traced, every legacy route 404, and the five dead anchors (`skip`, `stuck` ×2, `already`,
`invalid`) are all guards that fire only once the algorithm has effectively finished, each
reached by its alternate. Every step of every problem carries its `arrayState`, so there is
no RCA-034 blinking anywhere in the topic. What the audit found instead:

1. `insert-interval` labels every cell it emits `"[a,b]"`, merges along a timeline, and is
   named in `IntervalCanvas`'s own header — and was tagged `ARRAY`, so it drew a bar chart
   whose bar heights were each interval's *end time* (RCA-041).
2. `lru-page-replacement` emits its recency queue on **13 of 16 steps**, and `ArrayCanvas`
   never references `queueOrStackState`. Computed, never drawn (RCA-041).
3. `IntervalCanvas` ended its resolution chain with four hardcoded intervals — another
   problem's defaults, with invented states. Unreachable from both live Interval tracers,
   deleted while it was still cheap (RCA-040).

Also deleted two dead `board()` helpers, copy-pasted between `MinimumPlatformsTracer` and
`AssignCookiesTracer` and called by neither.

**Left open, deliberately:** `minimum-platforms` draws only its arrival array, with the
departure pointer `j` living in a variable string. The two-pointer sweep is therefore half
visible. The honest fix is a merged event timeline — all 2n arrivals and departures in time
order, each labelled — which is a tracer rewrite rather than a retag, and is listed here so
it is chosen rather than forgotten.

**Heaps & PriorityQueue — 17/17 traced, four findings, all fixed.** The sweep flagged one
dead anchor (`violation`, a terminal failure branch its alternate reaches) and nothing else.
The findings came from reading what the canvas was actually being handed:

1. **Five heaps were drawn fully sorted.** `HeapCanvas` puts slot `i`'s children at `2i+1`
   and `2i+2` and prints the index under each slot, so `Collections.sort(snapshot)` before
   emitting drew a perfectly ordered tree at every step — teaching that a priority queue
   keeps all its elements ordered, which is the commonest misconception about heaps
   (RCA-042). `java.util.PriorityQueue` does not expose its array and `toArray()` is
   documented as unordered, so sorting was the reachable way to get a deterministic order.
   `ArrayHeap` now keeps the array explicitly.
2. **A genuinely empty heap was reported as missing data** on 12 steps across 5 problems,
   `task-scheduler` on 8 of its 15 — where "nothing is schedulable, the CPU sits idle"
   beside an empty heap *is* the lesson (RCA-043).
3. **`merge-k-sorted-lists` scanned three heads linearly** while its code panel showed
   `PriorityQueue.poll()`, and never said "heap" in eighteen steps (RCA-044).
4. **`min-cost-connect-sticks` kept a sorted `ArrayList`** with `remove(0)` beside the same
   `PriorityQueue.poll()` code panel (RCA-044).

The near-miss is worth as much as the findings: (2) has the exact shape of RCA-034, and
applying that fix — carrying the last heap forward — would have shown `task-scheduler` a
heap the algorithm had just drained, on precisely the steps where its emptiness is the
point. **Absence and emptiness need opposite fixes and are indistinguishable from the
payload alone**; only reading the tracer separates them.

**Recursion & Backtracking — 25/25 traced, three findings, all fixed.** The call-stack
check came back clean: all 26 steps across the topic that show no stack are the closing
summary, with `callStack` explicitly `[]` after the recursion has unwound — honest, and the
same trap as the heaps one, checked the same way. The findings:

1. **`sudoku-solver` and `m-coloring` never backtracked on their defaults.** Three blanks in
   a finished grid, and K4-minus-an-edge with three colors: eighteen and ten steps, no dead
   end, no undo, in the topic named after the undo (RCA-045). New defaults, chosen by
   simulating each tracer's own search order, reach every anchor and retreat twice and once
   respectively.
2. **`n-queens` and `sudoku-solver` pushed no call frames** — the only two in the topic —
   so `MemoryComplexityCard`'s live call-stack section was empty for the two flagship
   backtracking problems (RCA-046). Adding frames immediately failed the F4 guard on
   sudoku, whose last step came from inside the recursion.
3. **`word-break`'s dead `memoHit` is correct and stays.** `memo.put(start, true)` runs on
   the way out of a successful call and every ancestor returns immediately, so a `true`
   entry can never be consulted: the memo only ever serves `false`, and every real-word
   input that hits it returns `false`. RCA-045 records the reasoning.

The lesson for the sweep: **"DEAD ANCHORS ['undo']" and "DEAD ANCHORS ['absent']" print
identically**, and one is a coverage nit while the other is the problem failing to
demonstrate itself. Read what the anchor is for.

---

## 5. Recommended fix sequence

Small PRs, landed one at a time, each green before the next — per `CLAUDE.md` branch
discipline.

| PR | Scope | Finding | Risk |
|---|---|---|---|
| 1 | Retire `default:` in the 12 services; retire the 3 explicit `GraphBfsDfsService` cases; add `assertThrows` coverage | F1 | Low — RED-first provable |
| 2 | Delete 24 dead generators in `ArrayService` + `SlidingWindowService` | F3 | **None** — already unreachable |
| 3 | Delete the remaining 40 dead generators | F3 | Low — do after PR 1 |
| 4 | Fix `CLAUDE.md:98`; prune `HANDOFF.md` to open prompts | F6 | None |
| 5 | Add the 9 missing `DsTypePayloadContractTest.REQUIRED` entries | F5 | Low |
| 6 | `DsuCanvas` loud-failure + DSU payload contract | F7 | Medium — touches transport |
| 7 | Emit terminal step for the 9 mid-unwind recursions (7 are one batch) | F4 | Low |
| 8 | Dead anchors, batched by cause: empty-container guards (6 problems), then failure branches (~17), then the long tail | F2 | Medium — 93 problems, many goldens |
| 9 | Differentiate `postfix-to-prefix` / `prefix-to-postfix`; fix `set-unset-rightmost-bit` | F8, F9 | Low |
| 10 | Resolve the 7 duplicate ids | F10 | Medium — moves a pinned number |

PR 8 is the large one and should be split per topic, starting with Advanced Graphs (20) and
Stack & Queue (15).

**Every PR in this list must, per repo rule:** prove the new assertion fails against the old
code first (`prove-the-test-fails`), regenerate affected goldens with
`mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true` and **read the diff**, confirm
`git status` shows only the expected fixtures, then full suite green.

---

## 6. Suggested `RCA.md` entries

Two of these are recurring classes, which is the bar `RCA.md` sets:

- **A retired topic leaves its `default:` returning steps.** Recurred in 12 services.
  Guard: a test asserting every legacy `switch` ends in a throw, not a return.
- **A tracer declares an anchor for a branch its default input never reaches.** Recurred in
  93 problems, with the same anchor name repeating across batches written together.
  Guard: strengthen `anchorsAreAllReachable` to assert *reachability*, not resolution.
- **A canvas defaults missing state to a plausible literal** (F7) — the no-fallback rule
  applied to the render path.

---

## 7. Open questions for the owner

1. **Is the legacy layer still wanted at all?** It is 100% superseded — every id is traced
   and the frontend uses v2 exclusively. `HANDOFF.md` Prompt D proposes retiring it. Doing
   that deletes F1 and F3 entirely (12 services, 64 dead methods) instead of repairing
   them. That is the single highest-leverage decision available here, and it makes PRs 1–3
   unnecessary.
2. ~~**Should a default input demonstrate the failure branch?**~~ **Answered.** The UI now
   marks branches the current input never took, so no default had to change and all 93
   problems are covered. The principle worth stating, and now stated in F2: *which branches
   run is a property of the data, so say so rather than hiding the gap or engineering it
   away.*
3. **Do the 7 duplicate ids represent real dual-category problems?** That determines
   whether F10 is a cleanup or a permanent, documented state.

---

## 8. Scope and limits of this audit

What this audit **did** verify, mechanically, for all 433 problems: traced status, catalogue
`dsType` vs tracer `dsType`, anchor reachability, step counts, budget headroom, legacy
endpoint status, duplicate ids, trace distinctness, call-stack balance, and payload presence
for the 170 problems on unchecked `DsType`s.

What it **did not** verify, and what remains genuinely unknown:

- **Whether each of the 433 descriptions is true.** No mechanical check can judge whether
  "i = 1: nums[1] = 7, so we need 9 - 7 = 2" is correct narration. That is the
  `review-trace-simulation` reading pass, 433 problems deep, and it was not performed here.
  I spot-read roughly a dozen traces; all were accurate.
- **Whether the 433 golden files were ever read.** `CLAUDE.md` warns that regenerating a
  golden without reading the diff "records a bug as expected". Golden content was not
  reviewed.
- **Whether each `alternateInput()` is materially different** beyond what
  `alternateInputDiffersFromDefaults` already enforces.
- **Visual rendering.** No canvas was rendered and inspected; F7 was found by reading
  source, not by looking at the screen.

---

## 9. How to reproduce

```bash
# backend, in one shell
cd backend && mvn spring-boot:run

# coverage truth — never quote a doc
curl -s http://localhost:8923/api/problems/stats | python3 -m json.tool

# the sweep, per topic (exit 1 on any finding)
python3 .claude/skills/audit-topic/audit_topic.py                      # list categories
python3 .claude/skills/audit-topic/audit_topic.py "Advanced Graphs"

# live legacy fallbacks (F1)
for f in backend/src/main/java/com/dsa/ui/service/*Service.java; do
  echo "$f: $(grep -nE '^\s*default:' $f | head -1)"; done

# dead legacy generators (F3)
python3 - <<'PY'
import re,glob
for f in sorted(glob.glob('backend/src/main/java/com/dsa/ui/service/*Service.java')):
    src=open(f).read()
    defs=re.findall(r'private\s+List<ExecutionStep>\s+(\w+)\s*\(\)', src)
    dead=[m for m in defs if len(re.findall(r'\b'+m+r'\s*\(\)', src))<=1]
    if dead: print(f, len(dead), dead)
PY

# suites
cd backend && mvn test
cd frontend && npm ci && npx vitest run && npx vite build
```

> `npm ci` is **required** before the frontend commands. Without `node_modules`, `npx`
> fetches the latest `vite`/`vitest`, which need Node ≥ 20.19 and fail on this machine's
> Node 20.9 with `does not provide an export named 'styleText'`. That failure looks like a
> broken test suite and is not one.

---

## Appendix A — every problem, by topic

433 rows. `legacy` is the HTTP status of the old per-topic endpoint: **410** = correctly
retired, **200** = still serving substitute steps ([F1](#f1-live-fallbacks-in-12-legacy-services)).


### Advanced Graphs — 53 problems, 30 clean, 23 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `accounts-merge` | Dsu | 11 | 410 | — |
| 2 | `alien-dictionary` | Graph | 15 | 410 | dead anchors ['cycleDetected'] |
| 3 | `articulation-points` | Graph | 27 | 410 | dead anchors ['rootAp'] |
| 4 | `bellman-ford` | Graph | 30 | 410 | dead anchors ['negativeCycleDetected'] |
| 5 | `bfs-dfs-intro` | Graph | 29 | 410 | — |
| 6 | `bipartite-graph-dfs` | Graph | 10 | 410 | dead anchors ['conflict'] |
| 7 | `cheapest-flights-k-stops` | Graph | 16 | 410 | — |
| 8 | `city-smallest-neighbors` | Matrix | 20 | 410 | — |
| 9 | `connected-components-intro` | Graph | 20 | 410 | — |
| 10 | `connected-matrix` | Matrix | 58 | 410 | — |
| 11 | `course-schedule-1` | Graph | 14 | 410 | — |
| 12 | `course-schedule-2` | Graph | 14 | 410 | — |
| 13 | `cycle-directed-bfs` | Graph | 3 | 410 | dead anchors ['decrement', 'enqueue', 'poll'] |
| 14 | `cycle-directed-dfs` | Graph | 10 | 410 | dead anchors ['backtrack', 'noCycle'] |
| 15 | `cycle-undirected-bfs` | Graph | 10 | 410 | dead anchors ['noCycle'] |
| 16 | `cycle-undirected-dfs` | Graph | 14 | 410 | dead anchors ['cycleDetected'] |
| 17 | `dfs-traversal` | Graph | 24 | 200 | **duplicate id**; **legacy still serves** (/api/graphs/advanced) |
| 18 | `dijkstra-min-heap` | Graph | 23 | 200 | **legacy still serves** (/api/graphs/advanced) |
| 19 | `dijkstra-pq-theory` | Graph | 24 | 410 | — |
| 20 | `disjoint-set-dsu` | Dsu | 14 | 410 | — |
| 21 | `find-eventual-safe-states` | Graph | 13 | 410 | — |
| 22 | `flood-fill` | Matrix | 32 | 410 | **duplicate id**; dead anchors ['alreadyPainted'] |
| 23 | `floyd-warshall` | Matrix | 23 | 410 | — |
| 24 | `graph-intro` | Graph | 17 | 410 | — |
| 25 | `graph-rep-cpp` | Graph | 12 | 410 | — |
| 26 | `graph-rep-java` | Graph | 10 | 410 | — |
| 27 | `kahn-algo-bfs` | Graph | 19 | 410 | — |
| 28 | `kosaraju-scc` | Graph | 20 | 410 | — |
| 29 | `kruskals-mst` | Graph | 10 | 410 | — |
| 30 | `making-large-island` | Matrix | 12 | 410 | dead anchors ['allLand'] |
| 31 | `min-multiplications-reach-end` | Queue | 53 | 410 | dead anchors ['exhausted'] |
| 32 | `most-stones-removed` | Dsu | 8 | 410 | — |
| 33 | `mst-theory` | Graph | 29 | 410 | — |
| 34 | `nearest-cell-1` | Matrix | 68 | 410 | — |
| 35 | `network-connected-ops` | Graph | 5 | 410 | dead anchors ['insufficient'] |
| 36 | `network-delay-time` | Graph | 18 | 410 | — |
| 37 | `num-provinces` | Graph | 13 | 410 | — |
| 38 | `number-of-enclaves` | Matrix | 11 | 410 | dead anchors ['follow'] |
| 39 | `number-of-islands` | Matrix | 12 | 200 | **duplicate id**; **legacy still serves** (/api/graphs/advanced) |
| 40 | `number-of-islands-2` | Matrix | 11 | 410 | dead anchors ['sameSet'] |
| 41 | `number-of-ways-destination` | Graph | 29 | 410 | — |
| 42 | `path-min-effort` | Matrix | 19 | 410 | — |
| 43 | `prims-mst` | Graph | 22 | 410 | — |
| 44 | `rotten-oranges` | Matrix | 57 | 410 | dead anchors ['stranded'] |
| 45 | `shortest-path-binary-maze` | Matrix | 17 | 410 | dead anchors ['unreachable'] |
| 46 | `shortest-path-dag` | Graph | 14 | 410 | dead anchors ['unreached'] |
| 47 | `shortest-path-undirected` | Graph | 13 | 410 | — |
| 48 | `surrounded-regions` | Matrix | 12 | 410 | **duplicate id**; dead anchors ['spread'] |
| 49 | `swim-in-rising-water` | Matrix | 42 | 410 | — |
| 50 | `tarjan-bridges` | Graph | 19 | 410 | — |
| 51 | `topo-sort-dfs` | Graph | 19 | 410 | — |
| 52 | `word-ladder-1` | Graph | 15 | 410 | dead anchors ['unreachable'] |
| 53 | `word-ladder-2` | Graph | 24 | 410 | dead anchors ['unreachable'] |

### Arrays — 40 problems, 29 clean, 11 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `check-sorted-ii` | Array | 8 | 410 | — |
| 2 | `count-inversions` | Array | 28 | 410 | — |
| 3 | `count-subarrays-given-sum` | Array | 10 | 410 | — |
| 4 | `count-subarrays-xor-k` | Array | 15 | 410 | — |
| 5 | `find-missing-number` | Array | 11 | 410 | — |
| 6 | `four-sum` | Array | 12 | 410 | dead anchors ['kSkipDupLeft', 'kSkipDupRight'] |
| 7 | `kadane-algo` | Array | 17 | 410 | — |
| 8 | `largest-element` | Array | 7 | 410 | — |
| 9 | `largest-subarray-sum-0` | Array | 18 | 410 | dead anchors ['wholePrefix'] |
| 10 | `leaders-in-array` | Array | 11 | 410 | — |
| 11 | `left-rotate-k` | Array | 10 | 410 | — |
| 12 | `left-rotate-one` | Array | 6 | 410 | — |
| 13 | `linear-search` | Array | 5 | 410 | dead anchors ['not-found'] |
| 14 | `longest-consecutive-sequence` | Array | 14 | 410 | — |
| 15 | `longest-subarray-sum-k` | Array | 15 | 410 | — |
| 16 | `longest-subarray-sum-k-positives` | Array | 23 | 410 | — |
| 17 | `majority-element` | Array | 9 | 410 | — |
| 18 | `majority-element-ii` | Array | 9 | 410 | dead anchors ['boost2', 'cancel'] |
| 19 | `max-consecutive-ones` | Array | 13 | 410 | — |
| 20 | `max-product-subarray` | Array | 10 | 410 | dead anchors ['resetCheck'] |
| 21 | `merge-intervals` | Array | 6 | 410 | **duplicate id** |
| 22 | `merge-two-sorted-arrays` | Array | 38 | 410 | — |
| 23 | `move-zeros-end` | Array | 10 | 410 | — |
| 24 | `next-permutation` | Array | 4 | 410 | dead anchors ['checkSwap', 'foundBreak', 'foundSwap', 'reverseSuffix'] |
| 25 | `pascals-triangle` | Matrix | 27 | 410 | — |
| 26 | `print-max-subarray` | Array | 15 | 410 | — |
| 27 | `rearrange-by-sign` | Array | 8 | 410 | — |
| 28 | `remove-duplicates-sorted` | Array | 15 | 410 | — |
| 29 | `repeating-missing-number` | Array | 7 | 410 | — |
| 30 | `reverse-pairs` | Array | 44 | 410 | — |
| 31 | `rotate-matrix-90` | Matrix | 8 | 410 | — |
| 32 | `second-largest-element` | Array | 9 | 410 | — |
| 33 | `set-matrix-zeroes` | Matrix | 5 | 410 | dead anchors ['markCol0', 'zeroCol0'] |
| 34 | `single-number` | Array | 7 | 410 | — |
| 35 | `sort-0-1-2` | Array | 8 | 410 | — |
| 36 | `spiral-matrix` | Matrix | 9 | 410 | — |
| 37 | `stock-buy-sell` | Array | 8 | 410 | — |
| 38 | `three-sum` | Array | 10 | 410 | dead anchors ['innerSkipDupLeft', 'innerSkipDupRight'] |
| 39 | `two-sum` | Array | 5 | 410 | dead anchors ['none'] |
| 40 | `union-sorted-arrays` | Array | 10 | 410 | dead anchors ['drainB'] |

### BST — 16 problems, 6 clean, 10 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `bst-delete` | Tree | 7 | 410 | dead anchors ['goRight', 'replaceWithLeft']; ends mid-recursion (call stack 1 deep on the last step) |
| 2 | `bst-floor` | Tree | 4 | 410 | dead anchors ['exactHit'] |
| 3 | `bst-floor-ceil` | Tree | 4 | 410 | dead anchors ['exactMatch'] |
| 4 | `bst-inorder-successor` | Tree | 4 | 410 | dead anchors ['passUnderneath', 'rememberAncestor', 'successorAbove'] |
| 5 | `bst-insert` | Tree | 4 | 410 | dead anchors ['attachRight'] |
| 6 | `bst-intro` | Tree | 8 | 410 | dead anchors ['notLarger'] |
| 7 | `bst-kth-smallest` | Tree | 9 | 410 | — |
| 8 | `bst-lca` | Tree | 3 | 410 | dead anchors ['bothLargerGoRight'] |
| 9 | `bst-min-max` | Tree | 7 | 410 | — |
| 10 | `bst-search` | Tree | 7 | 410 | — |
| 11 | `bst-validate` | Tree | 8 | 410 | — |
| 12 | `construct-bst-preorder` | Tree | 26 | 410 | — |
| 13 | `correct-bst-swap` | Tree | 11 | 410 | dead anchors ['swapAdjacent'] |
| 14 | `largest-bst-in-bt` | Tree | 16 | 410 | — |
| 15 | `merge-two-bsts` | Tree | 17 | 410 | dead anchors ['drainSecond'] |
| 16 | `two-sum-bst` | Tree | 7 | 410 | dead anchors ['advanceLow'] |

### Binary Search — 32 problems, 19 clean, 13 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `aggressive-cows` | Array | 20 | 410 | — |
| 2 | `binary-search-1d` | Array | 7 | 200 | dead anchors ['left', 'miss']; **legacy still serves** (/api/binarysearch) |
| 3 | `book-allocation` | Array | 44 | 410 | — |
| 4 | `count-occurrences` | Array | 7 | 410 | dead anchors ['absent'] |
| 5 | `count-rotations` | Array | 8 | 410 | — |
| 6 | `find-min-rotated-sorted` | Array | 8 | 410 | — |
| 7 | `find-peak-element` | Array | 6 | 410 | — |
| 8 | `find-peak-element-2d` | Matrix | 5 | 410 | dead anchors ['moveRight'] |
| 9 | `first-last-occurrence` | Array | 7 | 410 | dead anchors ['absent'] |
| 10 | `floor-ceil-sorted-array` | Array | 8 | 410 | — |
| 11 | `koko-eating-bananas` | Array | 20 | 410 | — |
| 12 | `kth-element-2-sorted-arrays` | Array | 3 | 410 | dead anchors ['shrinkHigh', 'shrinkLow', 'swapToSmaller'] |
| 13 | `kth-missing-positive` | Array | 8 | 410 | — |
| 14 | `lower-bound` | Array | 8 | 410 | — |
| 15 | `matrix-median` | Matrix | 8 | 410 | — |
| 16 | `median-2-sorted-arrays` | Array | 4 | 410 | dead anchors ['shrinkHigh', 'shrinkLow'] |
| 17 | `min-days-bouquets` | Array | 23 | 410 | dead anchors ['impossible'] |
| 18 | `minimize-max-distance-gas-station` | Array | 77 | 410 | — |
| 19 | `nth-root-number` | Array | 8 | 410 | dead anchors ['tooSmall'] |
| 20 | `painters-partition` | Array | 38 | 410 | — |
| 21 | `row-max-ones` | Matrix | 6 | 410 | — |
| 22 | `search-2d-matrix` | Matrix | 5 | 410 | dead anchors ['less', 'miss'] |
| 23 | `search-2d-matrix-2` | Matrix | 10 | 410 | dead anchors ['hit', 'moveDown'] |
| 24 | `search-insert-position` | Array | 6 | 410 | — |
| 25 | `search-rotated-sorted` | Array | 7 | 410 | dead anchors ['miss', 'rightSorted'] |
| 26 | `search-rotated-sorted-2` | Array | 7 | 410 | dead anchors ['miss', 'shrink'] |
| 27 | `ship-packages-d-days` | Array | 62 | 410 | — |
| 28 | `single-element-sorted` | Array | 5 | 410 | dead anchors ['goRight'] |
| 29 | `smallest-divisor` | Array | 26 | 410 | — |
| 30 | `split-array-largest-sum` | Array | 37 | 410 | — |
| 31 | `square-root-number` | Array | 12 | 410 | — |
| 32 | `upper-bound` | Array | 8 | 410 | — |

### Binary Trees — 38 problems, 35 clean, 3 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `bottom-view-bt` | Tree | 19 | 410 | — |
| 2 | `boundary-traversal` | Tree | 14 | 410 | — |
| 3 | `children-sum-property` | Tree | 11 | 410 | — |
| 4 | `construct-bt-post-in` | Tree | 22 | 410 | — |
| 5 | `construct-bt-pre-in` | Tree | 17 | 410 | — |
| 6 | `count-complete-tree-nodes` | Tree | 20 | 410 | — |
| 7 | `flatten-bt-to-ll` | Tree | 19 | 410 | — |
| 8 | `identical-trees` | Tree | 15 | 410 | dead anchors ['valuesDiffer'] |
| 9 | `iterative-inorder` | Tree | 22 | 410 | — |
| 10 | `iterative-preorder` | Tree | 19 | 410 | — |
| 11 | `max-width-bt` | Tree | 19 | 410 | — |
| 12 | `morris-inorder` | Tree | 17 | 410 | — |
| 13 | `morris-preorder` | Tree | 13 | 410 | — |
| 14 | `nodes-distance-k` | Tree | 14 | 410 | — |
| 15 | `postorder-1-stack` | Tree | 18 | 410 | — |
| 16 | `postorder-2-stacks` | Tree | 19 | 410 | — |
| 17 | `pre-post-in-one-traversal` | Tree | 16 | 410 | — |
| 18 | `right-left-view-bt` | Tree | 13 | 410 | — |
| 19 | `root-to-leaf-path` | Tree | 11 | 410 | — |
| 20 | `serialize-deserialize-bt` | Tree | 16 | 410 | — |
| 21 | `symmetric-tree` | Tree | 14 | 410 | dead anchors ['levelBreaksSymmetry'] |
| 22 | `top-view-bt` | Tree | 22 | 410 | — |
| 23 | `traversals-in-one-pass` | Tree | 23 | 410 | — |
| 24 | `tree-balanced` | Tree | 13 | 410 | — |
| 25 | `tree-burn-time` | Tree | 13 | 410 | — |
| 26 | `tree-diameter` | Tree | 11 | 410 | — |
| 27 | `tree-height` | Tree | 16 | 410 | — |
| 28 | `tree-inorder` | Tree | 12 | 410 | — |
| 29 | `tree-intro` | Tree | 16 | 410 | — |
| 30 | `tree-lca` | Tree | 16 | 410 | — |
| 31 | `tree-level-order` | Tree | 17 | 410 | — |
| 32 | `tree-max-path-sum` | Tree | 14 | 410 | — |
| 33 | `tree-postorder` | Tree | 12 | 410 | — |
| 34 | `tree-preorder` | Tree | 12 | 410 | — |
| 35 | `tree-rep-java` | Tree | 8 | 410 | dead anchors ['orphan'] |
| 36 | `unique-bt-requirements` | Tree | 7 | 410 | — |
| 37 | `vertical-order-traversal` | Tree | 16 | 410 | — |
| 38 | `zigzag-traversal` | Tree | 16 | 410 | — |

### Bit Manipulation — 18 problems, 16 clean, 2 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `check-ith-bit-set` | Bits | 3 | 410 | — |
| 2 | `check-number-odd` | Bits | 2 | 410 | — |
| 3 | `check-power-of-2` | Bits | 3 | 410 | — |
| 4 | `count-primes-range-sieve` | Array | 9 | 410 | — |
| 5 | `count-set-bits` | Bits | 10 | 410 | — |
| 6 | `divide-two-numbers-bitwise` | Bits | 10 | 410 | — |
| 7 | `divisors-of-number` | Array | 16 | 410 | — |
| 8 | `intro-bits-tricks` | Bits | 7 | 410 | — |
| 9 | `min-bit-flips` | Bits | 8 | 410 | — |
| 10 | `pow-x-n-math` | Bits | 14 | 410 | — |
| 11 | `power-set-bitwise` | Array | 9 | 410 | — |
| 12 | `prime-factorisation-queries` | Array | 5 | 410 | — |
| 13 | `print-prime-factors` | Array | 7 | 410 | — |
| 14 | `set-unset-rightmost-bit` | Bits | 1 | 410 | dead anchors ['unsetRightmostSet'] |
| 15 | `single-number-1` | Array | 7 | 410 | — |
| 16 | `single-number-3` | Array | 16 | 410 | — |
| 17 | `swap-two-numbers` | Array | 4 | 410 | — |
| 18 | `xor-numbers-in-range` | Bits | 6 | 410 | dead anchors ['mod0', 'mod3'] |

### Dynamic Programming — 55 problems, 53 clean, 2 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `assign-cookies-dp` | DpTable | 8 | 410 | — |
| 2 | `best-time-stock-1` | DpTable | 14 | 410 | — |
| 3 | `best-time-stock-2` | DpTable | 14 | 410 | — |
| 4 | `best-time-stock-3` | DpTable | 34 | 410 | — |
| 5 | `best-time-stock-4` | DpTable | 26 | 410 | — |
| 6 | `burst-balloons` | DpTable | 36 | 410 | — |
| 7 | `climbing-stairs` | DpTable | 7 | 410 | — |
| 8 | `coin-change-2` | DpTable | 19 | 410 | — |
| 9 | `count-partitions-given-diff` | DpTable | 26 | 410 | dead anchors ['zero'] |
| 10 | `count-square-submatrices` | Matrix | 18 | 410 | — |
| 11 | `count-subsets-with-sum-k` | DpTable | 19 | 410 | — |
| 12 | `distinct-subsequences` | DpTable | 44 | 410 | — |
| 13 | `edit-distance` | DpTable | 17 | 410 | — |
| 14 | `evaluate-boolean-expression` | DpTable | 12 | 410 | — |
| 15 | `frog-jump` | DpTable | 6 | 410 | — |
| 16 | `frog-jump-k-distance` | DpTable | 16 | 410 | — |
| 17 | `grid-unique-paths` | DpTable | 11 | 410 | — |
| 18 | `house-robber-2` | DpTable | 12 | 410 | — |
| 19 | `knapsack-01` | DpTable | 25 | 410 | — |
| 20 | `largest-divisible-subset` | DpTable | 9 | 410 | — |
| 21 | `lis-binary-search` | DpTable | 20 | 410 | — |
| 22 | `longest-bitonic-subsequence` | DpTable | 58 | 410 | — |
| 23 | `longest-common-subsequence` | DpTable | 17 | 410 | — |
| 24 | `longest-common-substring` | DpTable | 18 | 410 | — |
| 25 | `longest-increasing-subsequence` | DpTable | 38 | 410 | — |
| 26 | `longest-palindromic-subsequence` | DpTable | 27 | 410 | — |
| 27 | `longest-string-chain` | DpTable | 18 | 410 | — |
| 28 | `matrix-chain-multiplication` | DpTable | 32 | 410 | — |
| 29 | `matrix-chain-multiplication-theory` | DpTable | 6 | 410 | — |
| 30 | `max-rectangle-area-all-ones` | Matrix | 62 | 410 | — |
| 31 | `max-sum-non-adjacent` | DpTable | 9 | 410 | — |
| 32 | `mcm-cost-eval` | DpTable | 12 | 410 | — |
| 33 | `min-insertions-deletions-a-b` | DpTable | 14 | 410 | — |
| 34 | `min-insertions-palindrome` | DpTable | 27 | 410 | — |
| 35 | `minimum-coins-dp` | DpTable | 37 | 410 | — |
| 36 | `minimum-falling-path-sum` | DpTable | 13 | 410 | — |
| 37 | `ninja-and-his-friends` | DpTable | 43 | 410 | — |
| 38 | `ninjas-training` | DpTable | 14 | 410 | — |
| 39 | `number-of-lis` | DpTable | 12 | 410 | — |
| 40 | `palindrome-partitioning-2` | DpTable | 11 | 410 | — |
| 41 | `partition-array-max-sum` | DpTable | 20 | 410 | — |
| 42 | `partition-equal-subset-sum` | DpTable | 51 | 410 | dead anchors ['odd'] |
| 43 | `partition-set-min-abs-diff` | DpTable | 8 | 410 | — |
| 44 | `print-lis` | DpTable | 47 | 410 | — |
| 45 | `print-longest-common-subsequence` | DpTable | 33 | 410 | — |
| 46 | `rod-cutting-problem` | DpTable | 27 | 410 | — |
| 47 | `shortest-common-supersequence` | DpTable | 18 | 410 | — |
| 48 | `stock-cooldown` | DpTable | 12 | 410 | — |
| 49 | `stock-transaction-fee` | DpTable | 14 | 410 | — |
| 50 | `subset-sum-equal-target` | DpTable | 31 | 410 | — |
| 51 | `target-sum-dp` | DpTable | 12 | 410 | — |
| 52 | `triangle-min-path-sum` | DpTable | 12 | 410 | — |
| 53 | `unbounded-knapsack` | DpTable | 34 | 410 | — |
| 54 | `unique-paths-2` | DpTable | 11 | 410 | — |
| 55 | `wildcard-matching` | DpTable | 22 | 410 | — |

### Graph BFS/DFS — 7 problems, 3 clean, 4 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `bfs-traversal` | Graph | 21 | 410 | — |
| 2 | `directed-cycle-dfs` | Graph | 8 | 410 | dead anchors ['backtrack', 'noCycle'] |
| 3 | `distance-nearest-1` | Matrix | 48 | 410 | — |
| 4 | `number-of-provinces` | Graph | 14 | 410 | — |
| 5 | `rotting-oranges` | Matrix | 37 | 410 | dead anchors ['impossible'] |
| 6 | `undirected-cycle-bfs` | Graph | 10 | 410 | dead anchors ['noCycle'] |
| 7 | `undirected-cycle-dfs` | Graph | 12 | 410 | dead anchors ['cycleDetected'] |

### Greedy Algorithms — 14 problems, 9 clean, 5 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `assign-cookies` | Array | 7 | 410 | — |
| 2 | `candy` | Array | 12 | 410 | — |
| 3 | `fractional-knapsack` | Array | 8 | 410 | dead anchors ['skip'] |
| 4 | `insert-interval` | Array | 6 | 410 | — |
| 5 | `job-sequencing` | Array | 18 | 410 | — |
| 6 | `jump-game-1` | Array | 12 | 410 | dead anchors ['stuck'] |
| 7 | `jump-game-2` | Array | 8 | 410 | dead anchors ['stuck'] |
| 8 | `lemonade-change` | Array | 6 | 410 | dead anchors ['already'] |
| 9 | `lru-page-replacement` | Array | 16 | 410 | — |
| 10 | `minimum-platforms` | Array | 30 | 410 | — |
| 11 | `n-meetings-in-one-room` | Interval | 16 | 410 | — |
| 12 | `non-overlapping-intervals` | Interval | 10 | 410 | — |
| 13 | `shortest-job-first` | Array | 6 | 410 | — |
| 14 | `valid-parentheses-checker` | String | 10 | 410 | dead anchors ['invalid'] |

### Heaps & PriorityQueue — 17 problems, 16 clean, 1 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `check-min-heap` | Tree | 14 | 410 | dead anchors ['violation'] |
| 2 | `design-twitter` | PriorityQueue | 13 | 410 | — |
| 3 | `hand-of-straights` | Array | 13 | 410 | — |
| 4 | `heaps-theory` | Tree | 12 | 410 | — |
| 5 | `implement-min-heap` | Tree | 17 | 410 | — |
| 6 | `kth-largest-element` | Tree | 11 | 410 | — |
| 7 | `kth-largest-stream` | PriorityQueue | 21 | 410 | — |
| 8 | `kth-smallest-element` | Array | 10 | 410 | — |
| 9 | `maximum-sum-combination` | PriorityQueue | 21 | 410 | — |
| 10 | `median-data-stream` | Array | 9 | 410 | — |
| 11 | `merge-k-sorted-lists` | LinkedList | 18 | 410 | — |
| 12 | `min-cost-connect-sticks` | Array | 6 | 410 | — |
| 13 | `min-to-max-heap` | Tree | 18 | 410 | — |
| 14 | `replace-rank-array` | Array | 15 | 410 | — |
| 15 | `sort-k-sorted-array` | PriorityQueue | 15 | 410 | — |
| 16 | `task-scheduler` | Array | 15 | 410 | — |
| 17 | `top-k-frequent-elements` | Array | 6 | 410 | — |

### Learn the Basics — 14 problems, 7 clean, 7 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `armstrong-check` | Array | 5 | 410 | — |
| 2 | `check-prime` | Array | 3 | 410 | — |
| 3 | `count-digits` | Array | 7 | 410 | — |
| 4 | `factorial-number` | Stack | 9 | 410 | ends mid-recursion (call stack 1 deep on the last step) |
| 5 | `fibonacci-recursion` | RecursionTree | 22 | 410 | ends mid-recursion (call stack 1 deep on the last step) |
| 6 | `gcd-two-numbers` | Array | 4 | 410 | — |
| 7 | `palindrome-number` | Array | 7 | 410 | — |
| 8 | `palindrome-string-recursion` | String | 3 | 410 | dead anchors ['mismatch']; ends mid-recursion (call stack 3 deep on the last step) |
| 9 | `print-1-to-n` | RecursionTree | 11 | 410 | ends mid-recursion (call stack 1 deep on the last step) |
| 10 | `print-divisors` | Array | 7 | 410 | — |
| 11 | `print-n-to-1` | RecursionTree | 11 | 410 | ends mid-recursion (call stack 1 deep on the last step) |
| 12 | `reverse-array-recursion` | RecursionTree | 5 | 410 | ends mid-recursion (call stack 1 deep on the last step) |
| 13 | `reverse-number` | Array | 7 | 410 | — |
| 14 | `sum-first-n` | Stack | 11 | 410 | ends mid-recursion (call stack 1 deep on the last step) |

### Linked List — 31 problems, 29 clean, 2 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `add-one-to-number-ll` | LinkedList | 12 | 410 | dead anchors ['grow'] |
| 2 | `add-two-numbers-ll` | LinkedList | 10 | 410 | — |
| 3 | `clone-ll-random-pointer` | LinkedList | 20 | 410 | — |
| 4 | `delete-head-dll` | LinkedList | 6 | 410 | — |
| 5 | `delete-head-ll` | LinkedList | 6 | 410 | — |
| 6 | `delete-middle-node-ll` | LinkedList | 9 | 410 | — |
| 7 | `delete-occurrences-key-dll` | LinkedList | 10 | 410 | — |
| 8 | `detect-loop-linked-list` | LinkedList | 10 | 410 | — |
| 9 | `find-starting-point-loop` | LinkedList | 8 | 410 | dead anchors ['noLoop'] |
| 10 | `flattening-ll` | LinkedList | 37 | 410 | — |
| 11 | `insert-head-dll` | LinkedList | 5 | 410 | — |
| 12 | `insert-head-ll` | LinkedList | 5 | 410 | — |
| 13 | `intersection-point-y-ll` | LinkedList | 18 | 410 | — |
| 14 | `intro-doubly-ll` | LinkedList | 4 | 410 | — |
| 15 | `intro-singly-ll` | LinkedList | 5 | 410 | — |
| 16 | `length-ll` | LinkedList | 9 | 410 | — |
| 17 | `length-of-loop-ll` | LinkedList | 14 | 410 | — |
| 18 | `middle-linked-list` | LinkedList | 9 | 410 | — |
| 19 | `pairs-given-sum-dll` | LinkedList | 10 | 410 | — |
| 20 | `palindrome-ll` | LinkedList | 13 | 410 | — |
| 21 | `remove-duplicates-sorted-dll` | LinkedList | 14 | 410 | — |
| 22 | `remove-nth-from-back` | LinkedList | 12 | 410 | — |
| 23 | `reverse-dll` | LinkedList | 9 | 410 | — |
| 24 | `reverse-linked-list` | LinkedList | 14 | 410 | — |
| 25 | `reverse-ll-group-k` | LinkedList | 9 | 410 | — |
| 26 | `reverse-ll-recursive` | LinkedList | 12 | 410 | — |
| 27 | `rotate-ll` | LinkedList | 12 | 410 | — |
| 28 | `search-ll` | LinkedList | 8 | 410 | — |
| 29 | `segregate-odd-even-ll` | LinkedList | 10 | 410 | — |
| 30 | `sort-012-ll` | LinkedList | 19 | 410 | — |
| 31 | `sort-ll` | LinkedList | 16 | 410 | — |

### Recursion & Backtracking — 25 problems, 20 clean, 5 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `atoi-recursive` | String | 7 | 410 | — |
| 2 | `check-subsequence-sum-k` | Stack | 12 | 410 | — |
| 3 | `combination-sum-2` | Stack | 44 | 410 | — |
| 4 | `combination-sum-3` | Stack | 56 | 410 | — |
| 5 | `combination-sum-i` | Stack | 52 | 410 | — |
| 6 | `count-good-numbers` | Bits | 8 | 410 | — |
| 7 | `count-subsequences-sum-k` | Stack | 23 | 410 | — |
| 8 | `generate-binary-strings` | String | 16 | 410 | — |
| 9 | `generate-parentheses` | String | 27 | 410 | — |
| 10 | `letter-combinations-phone` | String | 22 | 410 | — |
| 11 | `m-coloring` | Graph | 10 | 410 | dead anchors ['undo'] |
| 12 | `n-queens` | Matrix | 79 | 410 | — |
| 13 | `palindrome-partitioning` | Stack | 15 | 410 | — |
| 14 | `permutations` | Array | 37 | 410 | — |
| 15 | `pow-x-n-recursive` | Bits | 6 | 410 | — |
| 16 | `power-set` | Stack | 23 | 410 | — |
| 17 | `rat-in-a-maze` | Matrix | 64 | 410 | — |
| 18 | `reverse-stack-recursion` | Stack | 22 | 410 | — |
| 19 | `sort-stack-recursion` | Stack | 18 | 410 | — |
| 20 | `subsequences-patterns-theory` | Stack | 19 | 410 | ends mid-recursion (call stack 4 deep on the last step) |
| 21 | `subsets-2` | Stack | 19 | 410 | — |
| 22 | `subsets-i` | Stack | 23 | 410 | — |
| 23 | `sudoku-solver` | Matrix | 18 | 410 | dead anchors ['backtrack', 'deadEnd'] |
| 24 | `word-break` | String | 10 | 410 | dead anchors ['memoHit', 'noSegmentation'] |
| 25 | `word-search` | Matrix | 21 | 410 | dead anchors ['exhausted'] |

### Sliding Window — 12 problems, 10 clean, 2 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `binary-subarrays-with-sum` | Array | 25 | 410 | — |
| 2 | `count-nice-subarrays` | Array | 24 | 410 | — |
| 3 | `fruit-into-baskets` | Array | 12 | 410 | — |
| 4 | `longest-repeating-character-replacement` | String | 9 | 410 | dead anchors ['shrink'] |
| 5 | `longest-substring-k-distinct` | String | 14 | 410 | — |
| 6 | `longest-substring-without-repeating` | String | 14 | 410 | **duplicate id** |
| 7 | `max-consecutive-ones-3` | Array | 28 | 410 | — |
| 8 | `maximum-points-cards` | Array | 9 | 410 | — |
| 9 | `minimum-window-subsequence` | String | 20 | 410 | — |
| 10 | `minimum-window-substring` | String | 27 | 410 | — |
| 11 | `number-substrings-all-three-chars` | String | 11 | 410 | — |
| 12 | `subarrays-k-different-integers` | Array | 28 | 410 | — |

### Sorting Algorithms — 5 problems, 5 clean, 0 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `bubble-sort` | Array | 20 | 410 | — |
| 2 | `insertion-sort` | Array | 13 | 410 | — |
| 3 | `merge-sort` | Array | 34 | 410 | — |
| 4 | `quick-sort` | Array | 22 | 410 | — |
| 5 | `selection-sort` | Array | 22 | 410 | — |

### Stack & Queue — 30 problems, 15 clean, 15 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `asteroid-collision` | Stack | 6 | 410 | dead anchors ['bothExplode'] |
| 2 | `balanced-parentheses` | Stack | 10 | 410 | dead anchors ['mismatch'] |
| 3 | `celebrity-problem` | Matrix | 5 | 410 | dead anchors ['noCeleb'] |
| 4 | `infix-to-postfix` | Stack | 12 | 410 | dead anchors ['closeParen', 'openParen'] |
| 5 | `infix-to-prefix` | Stack | 9 | 410 | dead anchors ['closeParen', 'openParen'] |
| 6 | `largest-rectangle-histogram` | Stack | 17 | 410 | — |
| 7 | `lfu-cache` | LinkedList | 10 | 410 | dead anchors ['update'] |
| 8 | `lru-cache` | LinkedList | 12 | 410 | dead anchors ['put.update'] |
| 9 | `maximum-rectangles-binary-matrix` | Stack | 43 | 410 | — |
| 10 | `min-stack` | Stack | 13 | 410 | — |
| 11 | `next-greater-element-1` | Stack | 14 | 410 | — |
| 12 | `next-greater-element-2` | Stack | 15 | 410 | — |
| 13 | `next-smaller-element` | Stack | 15 | 410 | — |
| 14 | `number-greater-elements-right` | Array | 32 | 410 | — |
| 15 | `postfix-to-infix` | Stack | 7 | 410 | — |
| 16 | `postfix-to-prefix` | Stack | 7 | 410 | — |
| 17 | `prefix-to-infix` | Stack | 7 | 410 | — |
| 18 | `prefix-to-postfix` | Stack | 7 | 410 | — |
| 19 | `queue-array-impl` | Queue | 12 | 410 | dead anchors ['frontEmpty', 'underflow'] |
| 20 | `queue-ll-impl` | LinkedList | 12 | 410 | dead anchors ['frontEmpty', 'underflow'] |
| 21 | `queue-stack-impl` | Queue | 16 | 410 | dead anchors ['peekEmpty'] |
| 22 | `remove-k-digits` | Stack | 13 | 410 | dead anchors ['trimTail'] |
| 23 | `sliding-window-maximum` | Stack | 22 | 410 | dead anchors ['popFrontOutOfWindow'] |
| 24 | `stack-array-impl` | Stack | 12 | 410 | dead anchors ['peekEmpty', 'underflow'] |
| 25 | `stack-ll-impl` | LinkedList | 11 | 410 | dead anchors ['peekEmpty', 'popEmpty'] |
| 26 | `stack-queue-impl` | Queue | 15 | 410 | dead anchors ['popEmpty', 'topEmpty'] |
| 27 | `stock-span-problem` | Stack | 21 | 410 | — |
| 28 | `sum-subarray-minimums` | Stack | 17 | 410 | — |
| 29 | `sum-subarray-ranges` | Stack | 15 | 410 | — |
| 30 | `trapping-rainwater` | Stack | 30 | 410 | — |

### Strings — 24 problems, 23 clean, 1 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `bracket-reversals` | String | 9 | 410 | — |
| 2 | `count-and-say` | String | 12 | 410 | — |
| 3 | `count-palindromic-subsequences` | DpTable | 11 | 410 | — |
| 4 | `count-substrings-k-distinct` | String | 11 | 410 | — |
| 5 | `isomorphic-strings` | String | 4 | 410 | — |
| 6 | `kmp-lps-algo` | String | 11 | 410 | — |
| 7 | `largest-odd-number-string` | String | 2 | 410 | — |
| 8 | `longest-common-prefix` | String | 5 | 410 | **duplicate id** |
| 9 | `longest-happy-prefix` | String | 11 | 410 | — |
| 10 | `longest-palindromic-substring` | String | 8 | 410 | — |
| 11 | `max-nesting-depth-parentheses` | String | 20 | 410 | — |
| 12 | `rabin-karp-algo` | String | 9 | 410 | — |
| 13 | `remove-outermost-parentheses` | String | 11 | 410 | — |
| 14 | `reverse-every-word` | String | 5 | 410 | — |
| 15 | `reverse-words-string` | String | 5 | 410 | — |
| 16 | `roman-to-integer` | String | 8 | 410 | — |
| 17 | `rotate-string` | String | 4 | 410 | — |
| 18 | `shortest-palindrome` | String | 24 | 410 | — |
| 19 | `sort-characters-frequency` | String | 8 | 410 | — |
| 20 | `string-hashing-theory` | String | 5 | 410 | — |
| 21 | `string-to-integer-atoi` | String | 7 | 410 | — |
| 22 | `sum-beauty-all-substrings` | String | 16 | 410 | — |
| 23 | `valid-anagram` | String | 8 | 410 | — |
| 24 | `z-function-algo` | String | 32 | 410 | — |

### Tries & Prefixes — 2 problems, 2 clean, 0 flagged

| # | id | dsType | steps | legacy | findings |
|---|---|---|---|---|---|
| 1 | `implement-trie` | Trie | 12 | 410 | — |
| 2 | `word-break-trie` | Trie | 18 | 410 | — |
