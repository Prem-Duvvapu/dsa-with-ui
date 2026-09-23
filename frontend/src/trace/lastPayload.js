/**
 * The most recent value a trace stated for a field, for canvases whose structure persists
 * between steps.
 *
 * Tracers restate a structure only on the steps that change it, and narrate in between. A
 * canvas reading `step.queueOrStackState || []` cannot tell "the stack is empty" from "this
 * step did not mention the stack", and renders both as empty — so the stack blinks empty
 * between every push. Measured across Stack & Queue: 60 steps in 21 of 24 problems showed
 * an empty stack while it held items, `stock-span-problem` on every other step.
 *
 * An explicit empty array is a real value and is returned as-is. Only absence looks back.
 */
export function lastPayload(steps, index, field, current) {
  const here = current?.[field];
  if (here !== undefined && here !== null) return here;

  if (Array.isArray(steps)) {
    for (let i = Math.min(index ?? steps.length - 1, steps.length - 1); i >= 0; i -= 1) {
      const value = steps[i]?.[field];
      if (value !== undefined && value !== null) return value;
    }
  }
  return null;
}
