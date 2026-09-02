import QueueCanvas from '../components/QueueCanvas';

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

  if (heroDsType === 'Graph' && runHasQueue) {
    companions.push({ key: 'queue', Component: QueueCanvas, props: { step, title: 'Queue' } });
  }

  return companions;
}
