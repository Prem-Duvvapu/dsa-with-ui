# Complete source helpers, batch 2

Base: merged #220 / `7a5120e`. Branch: `fix/complete-source-helpers-batch2`.
Pre-publication evidence snapshot; the owner approved publishing this batch after CI passes.

Four new owners: Book Allocation, Painter's Partition, Shipping Within D Days and
Minimum Days for Bouquets. Existing five core owners remain covered. Exact-source
Java17 compilation/execution, independent partition/interval oracles and HTTP delivery
pass29 contract cases. Full backend:8,052 tests,0 failures/errors,500 existing skips;
frontend:79 files/788 tests/build. `test-results.txt` contains labelled selected log
summaries/RED excerpts, not byte-identical full logs. Two wrong-answer mutations and
one non-source golden mutation were deliberately rejected, then restored.

From repository root:

```sh
cd backend && mvn -B test
cd ../frontend && npm ci && npx vitest run && npx vite build
```

```sh
node docs/ui-revamp/evidence/source-code/compare-goldens.cjs 7a5120e core /tmp/core-batch2-goldens.json
SOURCE_RESOURCE_FAMILY=core SOURCE_CAPTURE_ID=book-allocation PLAYWRIGHT_MODULE=/path/to/playwright node docs/ui-revamp/evidence/source-code/complete-source-journey.cjs /tmp/core-batch2-browser
SOURCE_RESOURCE_FAMILY=core SOURCE_CUSTOM_CASE=partition PLAYWRIGHT_MODULE=/path/to/playwright node docs/ui-revamp/evidence/source-code/custom-source-journey.cjs /tmp/core-batch2-custom
```

Browser commands require the real backend8923 and production preview5180. Do not run
Maven tests while that worktree's backend serves the journey. The installed external
Playwright module is not a project dependency. The core browser probe derives every
owner from resource files:36 rows here,320/1366px,both themes, exact first/middle/final
highlights, source keys, view continuity, no page overflow/errors, identified build
assets and source hashes equal to current goldens. Screenshot selection is explicit.

`custom-results.json` verifies Book Allocation [1,2,3,4],m=2 returns6; rejected m=13
returns400, focuses the error summary and retains source/step/shared input. The two
compatibility directories retain the strengthened existing Balanced Parentheses and
default DP/LCS custom probes. Input comparisons include decoded base64url content and
structural arrays, not only the existence of an input parameter.

Limits and existing defects are in `verification.json` and the source plan. Source
gates are not all-canvas mathematical certification: Book Allocation's final diagram
still says probing112 despite answer113. Smallest Divisor and Matrix Median have
separately reproduced input-contract defects. Initial ad-hoc browser diagnostics were
excluded after a local server termination/body-read failure; a fresh live diagnostic
confirmed the stale diagram. None is included as a passing matrix row.

Forty-four other historical non-platform omissions,77 platform contexts,C++ labelling,
semantic review of compiler successes,54 DP candidate rows, device/screen-reader/human
and performance/release gates remain. The fresh locked install still reports nine
dependency findings; no upgrades or new dependencies were introduced.
