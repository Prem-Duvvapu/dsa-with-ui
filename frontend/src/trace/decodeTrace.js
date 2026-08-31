/**
 * Rebuilds full steps from the backend's delta encoding.
 *
 * A trace grows as steps x n because every step used to carry a complete snapshot of
 * the data structure. The backend now sends only what changed. Two rules, and each
 * step's `keyframe` flag says which one applies:
 *
 *   keyframe: true   discard what you were holding. A field present here is its value;
 *                    a field ABSENT here is genuinely empty.
 *   otherwise        carry the previous step forward. A field present here replaces
 *                    what you held; a field absent here is unchanged.
 *
 * The distinction is load-bearing rather than tidy. bfs-traversal empties `activeEdges`
 * on 6 of its 21 steps and the tree traversals empty their call stack on the last one.
 * If "absent" always meant "unchanged", the canvas would keep highlighting edges the
 * algorithm had already left, and the call stack would never unwind. So a field that
 * changes to empty arrives explicitly as [], and one that changes to null arrives as a
 * keyframe.
 *
 * This mirrors TraceEncoderTest.decode() in the backend, which is written deliberately
 * literally so it can be read as the specification for this function. If you change one,
 * change both — the round-trip test there is what proves the pair agree.
 */

/** Fields that carry forward. Scalars (stepNumber, activeLine, description) always ship. */
const CARRIED = [
  'queueOrStackState',
  'callStack',
  'nodeStates',
  'activeEdges',
  'variables',
  'dsType',
  'gridState',
  'arrayState',
  'listState',
  'trieState',
  'treeNodes',
  'graphNodes',
  'graphEdges',
  'dpTable'
];

/**
 * @param {{encoding?: string, steps?: Array}} response a trace response from the API
 * @returns {Array} steps with every field populated, whatever encoding arrived
 */
export function decodeTrace(response) {
  const steps = response?.steps;
  if (!Array.isArray(steps)) return [];

  // `full` needs no work, and neither does a legacy response with no encoding field —
  // the eighteen per-topic endpoints still send complete steps.
  if (response.encoding !== 'delta') return steps;

  const decoded = [];
  let carried = null;

  for (const delta of steps) {
    const previous = delta.keyframe === true ? null : carried;
    const step = {
      stepNumber: delta.stepNumber,
      activeLine: delta.activeLine,
      description: delta.description
    };

    for (const field of CARRIED) {
      // Present in the delta wins; otherwise carry forward. `undefined` means absent —
      // an explicit null or [] is a real value and must not be treated as missing.
      step[field] = delta[field] !== undefined ? delta[field] : previous?.[field] ?? null;
    }

    decoded.push(step);
    carried = step;
  }
  return decoded;
}

/**
 * The nearest keyframe at or before `index`, for seeking without replaying from zero.
 * Returns 0 when the stream carries no keyframes, which is the safe answer.
 */
export function keyframeBefore(steps, index) {
  for (let i = Math.min(index, steps.length - 1); i >= 0; i -= 1) {
    if (steps[i]?.keyframe === true) return i;
  }
  return 0;
}
