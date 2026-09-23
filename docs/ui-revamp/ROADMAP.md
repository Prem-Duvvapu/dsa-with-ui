# Delivery roadmap

Planning baseline: 2026-09-23. The user requested a thorough plan before further implementation. **Only the planning package is complete; redesign implementation is planned.**

Statuses: `planned` → `in-progress` → `review` → `done`. Use `blocked` only with a concrete unresolved dependency. Completion requires the listed evidence; an attractive component or passing build alone is insufficient.

## 1. Phase overview and effort

| Phase | Outcome | Initial effort |
| --- | --- | ---: |
| R0 | Baseline, inventory, reference layouts, risk decisions | 1–1.5 days |
| R1 | Visual foundations and complete algorithm library | 1.5–2 days |
| R2 | Reference problem workspace and shared execution state | 3–4 days |
| R3 | All renderer families and narrow-screen adaptation | 2–3 days |
| R4 | Accessibility, regression, usability iteration, release readiness | 2.5–4.5 days |
| Total | First fully reviewed redesign release | **10–15 days** |

Focused engineering effort, not a delivery-date commitment. No parallel agent work is assumed. Re-estimate after R2 when renderer and state-transition costs are known. Changes to algorithms, new learning content, or dedicated replacement renderers require separate estimates.

## 2. R0 — Baseline and design validation

| ID | Work | Depends on | Acceptance and evidence | Status |
| --- | --- | --- | --- | --- |
| R0-01 | Browser and repository baseline | Planning package | Current screenshots at specified widths/themes; actual stats/representative IDs; existing test/build baseline; known failures recorded separately | planned |
| R0-02 | Freeze preservation matrix | R0-01 | F01–F25 each mapped to destination and test/manual check; renderer/input coverage confirmed | planned |
| R0-03 | Reference layout prototype | R0-02 | Library, Array, Graph+Queue, and 2D DP layouts at laptop/phone widths; long title, input editor, code split, and Analysis shown | planned |
| R0-04 | Design review and decisions | R0-03 | Review stage visibility, input discovery, code readability, and view transitions; record chosen dimensions/spacing and any adjustments | planned |

**Exit:** A concrete reference design supports both compact and space-demanding algorithms without deleting a feature. Prototype artifacts are labeled as prototypes; no placeholder simulation is presented as Java execution.

The review is a deliberate iteration point, not a claim that every routine implementation choice needs approval. If the user provides further direction, incorporate it before applying the design broadly.

## 3. R1 — Foundations and library

| ID | Work | Depends on | Acceptance and evidence | Status |
| --- | --- | --- | --- | --- |
| R1-01 | Theme, tokens, shared controls, page shell | R0-04 | System/light/dark; readable execution states; storage failure safe; focus and reduced motion; compact responsive header | planned |
| R1-02 | Shared catalogue and search extraction | R0-02 | No per-problem catalogue refetch; deduplication/retry/fallback retained; search ranking and recents tests pass | planned |
| R1-03 | Algorithm library | R1-01, R1-02 | Search/category/difficulty/runnable filters; all IDs reachable beyond first 50; URL state and return scroll; loading/empty/error states | planned |
| R1-04 | Problem switcher | R1-02, R1-01 | Ctrl/Cmd+K, modal focus, Escape, background inertness, current-problem handling, View all results | planned |

**Exit:** A user can find and open any catalogue entry with mouse or keyboard, including entries outside the first result batch. No execution requests occur merely from viewing the library.

## 4. R2 — Reference workspace

| ID | Work | Depends on | Acceptance and evidence | Status |
| --- | --- | --- | --- | --- |
| R2-01 | Problem-session state boundary | R1-02 | Draft/run separation, correct async defaults, stale-response safety, one timer, pause rules, preserved step/draft across views | planned |
| R2-02 | Playground and input layout | R2-01, R1-01, R0-04 | Natural scroll; generous stage; accessible input action; all seven input contract types; randomize/reset/run/validation preserved | planned |
| R2-03 | Code walkthrough and optional split | R2-02 | Same run/step; active-line follow control; readable split with keyboard resize; narrow-screen diagram/source selection | planned |
| R2-04 | Analysis and history | R2-01, R1-01 | Complete variables/frames/containers/complexity; bounded textual step list; capture where appropriate; accurate seek | planned |
| R2-05 | Focus mode, routes, switcher integration | R1-03, R1-04, R2-03, R2-04 | Existing deep links, view links, Back/refresh, unknown IDs, focus restore, no hidden keyboard handler | planned |
| R2-06 | Reference slice verification | R2-05 | Array, Graph+Queue, 2D DP real runs; edited inputs; error states; themes; 390px and 1366×768 browser journeys; user-visible design review artifact | planned |

**Exit:** One complete learning flow works across three structurally different examples. Current input, run, selected step, and source line stay consistent while changing views. This is the earliest point to route normal users through the replacement shell.

If a layout works only for Two Sum, this phase is not complete. If prototype inputs disappear on tab changes, this phase is not complete.

## 5. R3 — Renderer and responsive coverage

| ID | Work | Depends on | Acceptance and evidence | Status |
| --- | --- | --- | --- | --- |
| R3-01 | Sequence and linear structures | R2-06 | Array and shared mappings, LinkedList, Stack, Queue, Interval checked; long values/indices and empty state readable | planned |
| R3-02 | Spatial and branching structures | R2-06 | Tree, Graph, Trie, RecursionTree, Dsu checked; edges/labels remain meaningful; bounded navigation and reset available where needed | planned |
| R3-03 | Grid, DP, and companion layouts | R2-06 | Matrix, 1D/2D/slice DP, Graph/Matrix queue, Stack grid; stable pane presence and no clipped labels | planned |
| R3-04 | Complete responsive pass | R3-01, R3-02, R3-03 | 320/390/768/1366/1440 widths; short height, long title, virtual keyboard and 200% zoom; no page-wide horizontal overflow | planned |

**Exit:** Every registry key and input editor has recorded coverage. A failed case must be fixed or explicitly scoped and reviewed; hiding its navigation entry does not satisfy feature preservation.

## 6. R4 — Quality, iteration, release

| ID | Work | Depends on | Acceptance and evidence | Status |
| --- | --- | --- | --- | --- |
| R4-01 | Keyboard and accessibility review | R3-04 | Tab/dialog/resize controls; input error associations; readable text alternatives; reduced motion; screen-reader walkthrough | planned |
| R4-02 | Performance and trace-limit review | R3-04 | Baseline vs new build sizes; long-trace playback/seek and DOM counts measured; no unbounded history list; incomplete traces labeled | planned |
| R4-03 | Representative learner walkthroughs | R3-04 | Newcomer and returning-user tasks; observations, friction, and fixes recorded; design refinement verified | planned |
| R4-04 | Final regression and cleanup | R4-01, R4-02, R4-03 | Frontend tests/build; required backend CI; startup smoke if affected; remove legacy dead layout and prototype routes; docs accurate | planned |
| R4-05 | Release decision and handoff | R4-04 | F01–F25 closed with evidence; release ledger complete; remaining limits named; screenshots and rollback reference available | planned |

**Exit:** The release definition in [QUALITY_AND_RELEASE.md](QUALITY_AND_RELEASE.md) is met. Publishing/merging follows user authorization and is not implied by completing the plan.

## 7. Suggested change boundaries

1. Planning documents only.
2. Catalogue/search extraction and visual foundations, preserving the current usable interface.
3. Library and quick switcher.
4. Reference workspace/session/view integration.
5. Renderer adaptation in coherent families, with the relevant browser evidence.
6. Quality fixes, dead-code cleanup, and release documentation.

Do not force one PR per row; choose boundaries that leave each reviewed change coherent and testable. Do not combine backend algorithm expansion with UI navigation work.

## 8. Risks and response

| Risk | Signal | Response |
| --- | --- | --- |
| Features disappear behind new views | Inventory destination or test missing | Block integration until the F-ID is covered |
| Spacious shell still has tiny diagrams | Renderer measures old/fixed container | Adapt wrapper sizing and verify real structures |
| Draft/run confusion | Visible input differs from trace without a label | Separate state and capture input snapshot |
| New layout works only on large monitors | Short laptop/browser zoom clips controls | Treat 1366×768 and 320px width as required review cases |
| Hiding details hurts discoverability | Learner cannot find input/history/analysis | Improve labels, placement, and direct actions; retest tasks |
| Scope expands into a new course/platform | New content/backend systems appear | Record separately; finish preservation release first |
| Tests pass but interactions fail | Focus/scroll/resize issues in browser | Require manual/browser evidence before marking done |
| Estimates grow | Representative slice reveals editor/renderer defects | Re-estimate explicitly after R2; no silent acceptance cuts |

## 9. Work-item handoff format

For each implementation change, record: work IDs; learner outcome; changed behavior; F-IDs covered; automated checks and results; browser viewports/themes and artifacts; remaining defects; and the next unblocked dependency. Include the commit/branch and whether evidence uses real backend responses or fixtures.

**Current next step:** R0-01. No implementation work item has been marked complete by this planning pass.
