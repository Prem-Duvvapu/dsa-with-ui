import QueueCanvas from '../components/QueueCanvas';
import { GridCompanion } from '../components/GridCanvas';

/**
 * Companion panes for structures a step carries ALONGSIDE its hero dsType.
 *
 * A single ExecutionStep can hold several populated structure fields at once — Dijkstra's
 * step carries a graph AND a priority-queue snapshot in the same object. dsType picks the
 * hero canvas (see registry.js); this derives which OTHER populated fields deserve their
 * own pane, purely from what the payload carries — never from the problem id or title,
 * which is the string-sniffing this codebase has spent several PRs removing.
 *
 * Only wired for combinations a real tracer emits today (PROMPT-F-visual-fidelity.md,
 * slice F1). Registering an entry no tracer feeds would repeat the mistake it names: an
 * emitter with nothing rendering it, mirrored here as a renderer with nothing feeding it.
 *
 * Presence is decided from the WHOLE trace, not the current step alone. bfs-traversal's
 * queue is empty on its init and done steps and non-empty on almost every step between —
 * deciding per-step would make the pane pop in and out on nearly every click. Once a run
 * is known to use a queue at all, the pane stays mounted for the whole run and shows the
 * live (possibly genuinely empty) content each step — "Queue: empty" on the done step is
 * correct, not a reason to remove the pane.
 */
export function getCompanions(heroDsType, step, allSteps) {
  const companions = [];

  const runHasQueue = Array.isArray(allSteps)
    && allSteps.some((s) => s?.queueOrStackState?.length > 0);

  // Graph (bfs-traversal, dijkstra-min-heap, ...) and Matrix (rotting-oranges' multi-source
  // BFS over a grid) both narrate a real queue alongside their hero structure.
  if ((heroDsType === 'Graph' || heroDsType === 'Matrix') && runHasQueue) {
    companions.push({ key: 'queue', Component: QueueCanvas, props: { step, title: 'Queue' } });
  }

  const runHasGrid = Array.isArray(allSteps)
    && allSteps.some((s) => s?.gridState?.length > 0);

  // maximum-rectangles-binary-matrix is Stack-hero (the row's histogram is the active
  // structure) but emits `.grid(matrix)` on some steps too — the LeetCode 85 board itself,
  // otherwise never drawn. Not wired for Queue's own hero case: no Queue-dsType tracer
  // emits a grid today (see the file-level doc on why an entry needs a real emitter first).
  if (heroDsType === 'Stack' && runHasGrid) {
    companions.push({
      key: 'grid',
      Component: GridCompanion,
      props: { step: { ...step, gridState: lastKnownGrid(step, allSteps) }, title: 'Grid' }
    });
  }

  return companions;
}

/**
 * The tracer only calls `.grid(matrix)` on the steps where the matrix is what changed
 * (row start, row done) — every other step's `gridState` is simply absent, not "the
 * matrix went empty". Unlike `queueOrStackState`, which genuinely IS empty between
 * elements (a real algorithmic state this file's own doc already covers), a missing
 * `gridState` here means "unchanged since the last step that set it", so the companion
 * carries the most recent one forward instead of flashing an empty pane on every step
 * that narrates the stack instead.
 */
function lastKnownGrid(step, allSteps) {
  if (step?.gridState?.length) {
    return step.gridState;
  }
  const idx = allSteps.indexOf(step);
  for (let i = idx - 1; i >= 0; i--) {
    if (allSteps[i]?.gridState?.length) {
      return allSteps[i].gridState;
    }
  }
  return step?.gridState;
}
