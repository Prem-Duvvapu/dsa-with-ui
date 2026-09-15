# Review gates

Every change goes through six reviews before it lands. They are roles, not people — one
person wears all six, and the point is that each pass asks a question the others do not.

This file exists because of a specific failure mode in this repo: **the test suite is
excellent at catching fake work inside a unit and weak at catching wrong wiring between
units.** That is how 106 of 433 problems carried defects under a fully green suite, how a
mobile layout lost three panels with 245 tests passing, and how a feature populated the
catalogue correctly and served nothing. Each gate below is aimed at a failure this codebase
has actually had.

> **How to use this.** Work through the gates in order on your own change. Where a gate
> lists a command, run it — do not reason about what it would print. Record the answer to
> anything a gate flags in the PR description, including the ones you decided not to fix
> and why.

---

## 1. Senior Backend Engineer

**Asks: does this hold up under a caller who is not the UI?**

- [ ] Every new field on a model is actually **served**. `/api/problems/{id}` hand-builds
      its response map and silently drops anything it does not name — this has already
      shipped once. Assert the JSON a client receives, not the object in memory.
- [ ] Input from a caller crosses `InputValidator` and nothing else. Unknown fields are
      rejected rather than ignored; every bound in the `InputSpec` is enforced.
- [ ] Both caps still apply: the per-field size ceiling and the global step budget. A new
      endpoint that runs an algorithm without them is a free CPU burner.
- [ ] Response weight considered. `/api/problems` is the largest response in the API; see
      `HttpCachingTest` for why its ETag must stay **weak** (a strong one silently disables
      compression) and why the projection is cached and unmodifiable.
- [ ] Anything shared across requests is immutable, or documented as to why not.

```bash
cd backend && mvn test
```

---

## 2. Senior Frontend Engineer

**Asks: what happens when the data is missing, huge, or late?**

- [ ] No new npm dependency without a stated reason. This app ships one bundle and the
      dependency list is deliberately short.
- [ ] Every `var()` and `className` resolves — `designTokens.test.js` fails the build
      otherwise, because CSS drops an unresolvable declaration silently.
- [ ] No hardcoded colour. Use a token, or add one to **all three** declarations (`:root`,
      the `prefers-color-scheme` block, and `[data-theme="light"]`). A colour defined in
      only one renders one theme's ink on the other's ground.
- [ ] Loading, empty, and error states exist for anything that fetches.
- [ ] A component that can render without its data returns `null` rather than inventing a
      plausible default. `DsuCanvas` used to draw a fabricated DSU when its state was
      missing — nothing failed, and the user was shown a lie.
- [ ] The change survives a reload: view preferences that should persist do, and ones that
      should not (the mobile drawer, the selected problem) do not.

```bash
cd frontend && npx vitest run && npx vite build
```

---

## 3. Senior UI/UX Designer

**Asks: does this help someone who has never seen the app?**

- [ ] The canvas keeps the room. Panels that support *setting up* a run — the input editor,
      the complexity card, the problem statement — stay out of the way *during* one.
- [ ] State never rides on colour alone. Every legend entry pairs a colour with a glyph and
      a word; keep it that way.
- [ ] Reachable by keyboard, and the binding is discoverable. Shortcuts that exist only in a
      `title` attribute are shortcuts nobody uses — add them to `ShortcutHelp`.
- [ ] Screen-reader parity for anything new: `LiveTraceTicker` announces the narration and
      `StepStateSummary` describes the data. A new canvas needs the latter to say something
      true about it.
- [ ] Both themes checked, not just the one you develop in.
- [ ] If the change moves a region the tour points at, the `data-tour` anchor moves with it.

---

## 4. Senior Product Manager

**Asks: is this worth the space it takes?**

- [ ] The change serves a learner watching an algorithm, not a hypothetical user.
- [ ] Nothing is presented as fact that the system does not know. Absent data renders as
      absent — `ProblemConstraints` records a bound only when it is real, because an
      invented constraint is indistinguishable from a true one on screen.
- [ ] Source-of-truth confusion avoided. The problem's own constraints and the visualiser's
      input caps differ by orders of magnitude and are labelled apart on purpose.
- [ ] Coverage claims in `README.md` / `CLAUDE.md` / `HANDOFF.md` match
      `GET /api/problems/stats`. Never quote a number from a document.
- [ ] Scope stated: what this does *not* do is written down rather than left implied.

```bash
curl -s localhost:8923/api/problems/stats | python3 -m json.tool
```

---

## 5. Senior Code Architect

**Asks: what does this make harder to change later?**

- [ ] No fallback, anywhere. An unknown id is 404, an untraced one is 501, an unknown
      `dsType` renders an explicit unsupported state. A `default:` that returns data is the
      defect this whole architecture exists to prevent.
- [ ] No hand-maintained list of ids in a test or a switch. Derive from the registry or the
      catalogue — drift in exactly those lists is what hid the last live fallbacks through
      four rounds of cleanup.
- [ ] Contracts live at the seam. A tracer declares its `dsType` and a test proves the
      payload backs it; a canvas reads a documented field rather than parsing a string.
- [ ] Dead code goes. A retired branch means deleting the generator it called, not just
      refusing to call it — check reachability transitively.
- [ ] Content stays out of constructors where it can. 433 `ProblemDetail`s in Java means
      every copy edit is a recompile.
- [ ] Anchors, not positions. Anything that points at the UI from outside it — the tour,
      a test — targets a stable attribute, never a CSS class or a coordinate.

---

## 6. Senior QA

**Asks: would this test have caught the bug?**

- [ ] **Prove the new test fails against the unfixed code.** Not optional here. This repo
      shipped 303 broken problems under 90 green tests; a test that passes on the first run
      has proven nothing. See `prove-the-test-fails`.
- [ ] The assertion could distinguish right from wrong. `!steps.isEmpty()` was true for
      every one of those 303 problems.
- [ ] Skips are counted, not just tolerated. `stepCountGrowsWithInput` currently skips
      **152 of 433** problems; a growing skip count is coverage quietly leaving.
- [ ] Structural changes get a smoke test per breakpoint. A refactor removed the code
      panel, input editor and complexity card from mobile at once, and every test passed.
- [ ] Goldens regenerated **and read**. `git status` shows only the fixtures you expected,
      and you checked the values are right — `factorial(5) = 120`, not merely "it changed".
- [ ] Storage isolated. Persisted preferences make a suite order-dependent;
      `setupTests.js` clears them around every test.

```bash
cd backend && mvn test -Dtest=GoldenTraceTest -Dgolden.regenerate=true
git status --porcelain backend/src/test/resources/golden/
git diff backend/src/test/resources/golden/
```

---

## Before it lands

- [ ] Branch cut from `main`; nothing committed on `main` itself.
- [ ] Full suite green on both tiers.
- [ ] Pinned numbers unmoved (433 unique ids, 7 duplicates), or moved deliberately with the
      `README.md` coverage table updated in the same commit.
- [ ] Recurring classes of defect recorded in `RCA.md`.
- [ ] The commit message says what was wrong, not only what changed.
