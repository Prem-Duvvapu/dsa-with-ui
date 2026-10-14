# Complete string sources, batch 3

Base: merged #224 / `3a570c7`. Branch: `fix/complete-string-source-batch3`.
Verification snapshot from2026-10-13. On2026-10-14 the owner instructed continuation
after the publication request; publishing proceeds through a CI-gated PR. GitHub records
the final merge state; earlier local/unpublished wording describes the evidence snapshot.

New complete-source owners: Reverse Every Word and Rabin–Karp. Both retain their exact
runtime algorithms, visualizer input specifications, defaults, alternates and String
renderer. Siblings are unchanged. Only their source binding moves to the existing
strict `CompleteSourceTracer` resource family; no fallback or new dependency is added.

The exact displayed Java17 compiles without added import/class wrappers or application
helpers. Independent tests compare each compiled solution with its real tracer and
expected answers: exhaustive spacing-preserving reversal and naive substring matching,
plus bounds, overlaps and actual BASE256/MOD101 collisions. The compiled hash helper
is checked directly against expanded polynomial arithmetic, not merely constant text.

The initial two new tests fail against the old listings. A separate Java17 diagnostic
adds only a class wrapper and JDK imports to those old snippets: both still fail on
missing `reverse` or `hash`/`roll` helpers. Compiling wrong-result mutations and the
hash-helper mutation are checked separately and restored before final verification.
The typed results are checked too: reversal declares String, and Rabin–Karp declares
List<Integer> and returns actual Integer elements, not numeric strings with the same
printed list. Compiling declaration/unchecked-element mutations prove those guards.
See `verification.json` for actual counts, proof outcomes and excluded attempts.

Reproduce from repository root:

```sh
cd backend && mvn -B test
cd ../frontend && npm ci && npx vitest run && npx vite build
```

```sh
node docs/ui-revamp/evidence/source-code/compare-goldens.cjs 3a570c7 core /tmp/core-batch3-goldens.json
SOURCE_RESOURCE_FAMILY=core SOURCE_CAPTURE_ID=rabin-karp-algo PLAYWRIGHT_MODULE=/path/to/playwright node docs/ui-revamp/evidence/source-code/complete-source-journey.cjs /tmp/core-batch3-chromium
SOURCE_RESOURCE_FAMILY=core SOURCE_CAPTURE_ID=rabin-karp-algo AUDIT_ENGINE=firefox PLAYWRIGHT_MODULE=/path/to/playwright node docs/ui-revamp/evidence/source-code/complete-source-journey.cjs /tmp/core-batch3-firefox
SOURCE_RESOURCE_FAMILY=core SOURCE_CUSTOM_CASE=reverse-words AUDIT_WIDTH=320 PLAYWRIGHT_MODULE=/path/to/playwright node docs/ui-revamp/evidence/source-code/custom-source-journey.cjs /tmp/core-batch3-custom
```

Browser probes require a packaged real backend on8923 and production preview on5180.
Do not run Maven while that worktree serves a browser journey. Use separate output
directories for engines/cases: each probe intentionally writes fixed manifest names.
External Playwright is not a project dependency. Never raise execution rate limits.

`chromium/results.json` and `firefox/results.json` each contain52 completed rows:
13 registered core-source owners ×320/1366px ×both themes. Every source hash matches
its current golden. First/middle/final highlights, source-owned End, view/step continuity,
production asset identity, page errors and horizontal overflow are checked.

Each engine's `custom/` contains8 completed journeys: two widths for `reverse-words`,
`reverse-spaces`, `rabin-karp`, and `rabin-karp-longer`. These verify successful known
answers, exact shared input,400 focus, retained source/step/link, and exactly three
executions (initial, custom, rejected)—none for view changes. The all-space run has one
real step and intentionally omits the default `step=1` URL parameter; a longer pattern
returns a complete no-match result, not fabricated windows. Two further compatibility
manifests preserve the older Balanced Parentheses and LCS custom journeys.

Scope limits: source/math and stated browser cases only, not every catalogue answer,
all viewports, real-device/virtual-keyboard, screen-reader, learner or performance sign-off.
The broader platform statement and existing visualizer spacing extension are unchanged.
Infix grammar refusals, source omissions, node contexts, statements, DP rollout and
P7/P8/P9 remain separate work. This source-only package deliberately does not repair
the newly reproduced complexity metadata defects: Rabin–Karp has graph metadata and
Reverse Every Word incorrectly claims constant auxiliary space. Provider-owned metadata
must be corrected before canonical approach snapshots in a separate verified package.
