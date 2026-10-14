# Infix grammar and complete conversion sources

Base: merged #225 / `bec487c`. Branch: `fix/infix-source-validation`.
Local verification completed on 2026-10-14; publication is a separate owner decision.

Scope: Infix to Postfix and Infix to Prefix. Their original valid-input algorithms,
defaults, alternates, Stack renderer, 40-character ceiling and execution budgets stay
unchanged. Invalid binary expressions now receive an `expression` field error before
trace execution. The shared validator enforces a declared `requireInfixExpression`
constraint, not a problem-id switch. Served help explains balanced groups, single A-Z
operands, binary operators and the absence of unary operators or implicit multiplication.

Both owners load exact standalone Java17 solutions through the existing strict core
resource family. Each includes actual precedence/associativity helpers, complete
parenthesis handling and its own grammar guard. Prefix includes reverse-and-swap.
The standalone solvers do not impose the visualizer's UI size cap; the API still does.
All eight existing operation anchors are retained. No fallback or dependency is added.
One additional teaching correction affects custom Prefix runs only: an equal-`^` pop
now explains reversed-scan associativity instead of the false inequality `3 < 3`.
Answers, operation order and state remain unchanged; default golden narration is unchanged.

Randomize now honors this declared grammar only for flagged string fields, producing
short, varied uppercase expressions within their bounds. Randomize edits the draft;
Run remains explicit. Other string-generation behavior is unchanged and is not certified
by this package. No generic regex generator or broad randomizer rewrite is included.

## Regression evidence

- All39 initial selected backend checks fail on the original implementation: two exact
  source compilations and37 grammar/API/spec checks, with each converter checked independently.
- Even adding a Solution wrapper and JDK imports to each old listing still fails:
  Postfix omits `precedence`; Prefix also omits `reverseAndSwapParens`.
- Each final source test compares233 valid expressions with the tracer and an independent
  recursive-descent syntax tree, and rejects4,673 unique invalid expressions in both
  standalone Java and InputValidator. These sets include exhaustive strings over
  `A+-*/^()` through length4, fixed associativity/group/boundary cases, and250 seeded
  expression constructions (duplicates collapsed). Explicit alphabet/null failures are
  additional; these are not4,906 traced executions per converter.
- Disabling shared grammar enforcement causes36/38 selected checks to fail; the two
  served-declaration checks correctly still pass. Removing the declaration alone is
  caught by the independent served-spec guards.
- Reversing associativity in both compiling sources causes2/2 selected tests to fail
  on `A^B^C`: `AB^C^`/`^^ABC` instead of `ABC^^`/`^A^BC`.
- Removing the standalone grammar guards causes2/2 selected tests to fail on `+`.
- A separate narration regression and live browser guard fail on the old `3 < 3` message.
  The final Java test identifies the incoming operator from the actual highlighted
  character, so a genuinely higher-precedence pop is not mislabeled as an equal one.
- Initial frontend grammar tests fail2/16 before implementation. Disabling the finished
  generator branch makes all four new guards fail (4/36 across two selected files).
- Every mutation is restored. Proof runs overlap and must not be summed as distinct tests.
  One initial input-panel test used a matcher unavailable in Vitest1; it was corrected
  to the equivalent call-count and exact-argument assertions, not weakened.
  An intermediate full run had one false failure because the first narration test
  classified every popped `^` as equal precedence, including an arriving `*`. That
  assertion was refined to the actual incoming character and given a strict-greater
  negative guard, re-proved RED on the original message, then verified in the final suite.
  The superseded run and pre-narration browser manifests are not acceptance evidence.

Final local backend suite:8,183 reported cases,0 failures/errors,500 unchanged skips.
The registry-derived source contract passes48 cases across15 owners and35 published
examples; the infix-specific class passes38 cases. Frontend:83 files/857 tests, production
build passed; JS389.24kB/gzip124.01, CSS98.74kB/gzip17.92. Startup-cleanup smoke and
packaging pass. Two default goldens change source/anchors/highlights only. No dependency
or lockfile change. The existing nine vulnerable package entries remain a release issue.

## Reproduce

```sh
cd backend && mvn -B test
cd ../frontend && npm ci && npx vitest run && npx vite build
```

Real packaged backend on8923 and production preview on5180 are required for browser
probes. Do not run Maven while the worktree serves a journey. Use separate output
directories. External Playwright is not a project dependency. Normal execution limits
remain enabled; probes are paced, not rate-limit-bypassed.

```sh
node docs/ui-revamp/evidence/source-code/compare-goldens.cjs bec487c core /tmp/infix-goldens.json
SOURCE_RESOURCE_FAMILY=core SOURCE_CAPTURE_ID=infix-to-prefix PLAYWRIGHT_MODULE=/path/to/playwright node docs/ui-revamp/evidence/source-code/complete-source-journey.cjs /tmp/infix-chromium
SOURCE_RESOURCE_FAMILY=core SOURCE_CUSTOM_CASE=infix-postfix AUDIT_WIDTH=320 AUDIT_THEME=dark PLAYWRIGHT_MODULE=/path/to/playwright node docs/ui-revamp/evidence/source-code/custom-source-journey.cjs /tmp/infix-custom
PLAYWRIGHT_MODULE=/path/to/playwright node docs/ui-revamp/evidence/source-code/infix-session-journey.cjs /tmp/infix-session
```

`AUDIT_ENGINE=firefox` selects Firefox for each journey. Custom probes can select
`infix-prefix` too; `SOURCE_REJECTED_EXPRESSION` chooses another malformed case.
The `infix-postfix-cap` and `infix-prefix-cap` profiles use39 characters and genuinely
render all19 peak-stack items, checking page containment in addition to API payload size.
Final completed manifests pass170 journeys:120 default-source rows (15 owners ×2 widths
×2 themes ×2 engines),16 shared-link/Randomize/Focus/reload sessions,32 successful-run/
rejected-rerun cases, and2 older-family compatibility journeys. The16 cap cases are a
subset of those32 custom cases, not additional journeys. Each custom case makes exactly
3 executions; each session makes5 (including its shared reload). View/Focus changes and
Randomize alone execute nothing. Prefix's equal-exponent message is checked too.

`manifest-audit.json` confirms completed axes, source hashes against current goldens,
exact final served assets and that every local browser manifest postdates the final
16:08:00Z backend package. Both engines pass with no page errors or page-wide horizontal
overflow. Code first/middle/final highlights, source-owned End and view/step continuity
are checked. The full final backend includes the corrected narration test and guard.

Four named screenshots in `verification.json` were visually inspected. Long lines stay
inside the source pane. Existing vertical-space limitations remain visible: a19-item
stack needs page scrolling, and the phone source pane gives limited useful reading height.
No claim is made that all19 items fit simultaneously in the viewport or that P8 is done.

`public-merged-string-225.json` records a separately scoped deployment check of the
already merged previous package, not these unpublished infix changes: both exact detail
sources match #225, and8 public full/delta executions pass4 independently known cases
(collision, longer pattern, mixed spacing,60 spaces), with431 traced/0 duplicate ids.
The first Node-fetch detail attempt timed out; curl succeeded. This does not establish
all catalogue results, complexity metadata, human acceptance, release or rollback readiness.

Remaining outside this package:38 other historical non-platform omissions after landing,
77 platform contexts,41 statements,49 DP candidates, canonical complexity defects, broader
source semantics and Randomize contracts, C++ language honesty, D6/D7 and P7/P8/P9.
Device, screen-reader, learner, performance and release/rollback gates remain open.

## Separate read-only continuation findings

The following reports are not additional passing infix tests and none of their defects
has been fixed in this package:

- `next-candidate-preflight.json`:89 runtime cases. Count Good Numbers passes all60
  allowed lengths against iterative BigInteger arithmetic;12 SPF cases pass independent
  trial division. Phone `777777` truncates before its full4,096 results. Four of9 sampled
  Gas Stations inputs violate the authored two-decimal claim. All four candidates also
  expose inaccurate generic complexity metadata; Phone lacks an authored statement.
- `numeric-boundary-preflight.json`:9 of10 targeted requests accept narrowed integers
  instead of refusing them. The Graph payload control rejects correctly, but a graph's
  separate scalar `start` still wraps. `numeric-boundary-browser.json`/PNG reproduces
  draft/share4294967297 versus actual input1 with HTTP200 and no field error.
- `numeric-field-inventory.json`:478 numeric field declarations across340 canonical
  problems use the unchecked conversion paths. This is structural scope, not340 individually
  reproduced endpoint failures or an inventory of every alternative approach.
- `dependency-preflight.json`:fresh read-only audit,9 vulnerable package entries from
  11 unique advisories;2 production-only Router entries. Suggested major versions and
  engine/peer metadata are recorded, not installed or claimed compatible by testing.

RCA-065 and RCA-066 are open; RCA-056 has the additional Phone budget finding. Exact
numeric representability is the next correctness priority after this package lands.
Do not fix it only in the browser or mix broad toolchain changes into this source PR.
