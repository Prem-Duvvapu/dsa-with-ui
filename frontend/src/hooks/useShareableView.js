import { useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';

/**
 * The parts of "what I am looking at" that the URL did not carry.
 *
 * `/problem/:id` already made the problem linkable. The step and the input were not, so a
 * refresh dropped you back on step 1 of the defaults, and a link to "watch what happens at
 * step 14 with THESE two words" could not be written at all.
 *
 * Two rules that matter more than they look:
 *
 * - The step is written with `replace`, never `push`. Playback moves the step several times
 *   a second; pushing would bury the previous problem under a hundred history entries and
 *   make the back button useless.
 * - The input is encoded, not spelled out. Field values are arrays, grids and graphs, and a
 *   query string is not the place to invent a syntax for them. It is also never trusted:
 *   what comes back off the URL goes through `runInput` like any other input, so the
 *   server's own per-field validation answers, and a corrupted link produces the ordinary
 *   field errors rather than a broken page.
 */

const STEP = 'step';
const INPUT = 'input';

/** JSON → URL-safe base64. Kept symmetric with decodeInput; neither ever throws. */
export function encodeInput(values) {
  try {
    const json = JSON.stringify(values);
    const bytes = new TextEncoder().encode(json);
    let binary = '';
    bytes.forEach((b) => { binary += String.fromCharCode(b); });
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  } catch {
    return null;
  }
}

export function decodeInput(encoded) {
  if (typeof encoded !== 'string' || encoded.length === 0) return null;
  try {
    const padded = encoded.replace(/-/g, '+').replace(/_/g, '/');
    const binary = atob(padded + '='.repeat((4 - (padded.length % 4)) % 4));
    const bytes = Uint8Array.from(binary, (ch) => ch.charCodeAt(0));
    const parsed = JSON.parse(new TextDecoder().decode(bytes));
    // An array or a scalar is not an input map. Anything else is the caller's problem to
    // validate, which the server does.
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

/**
 * @param problemId    the problem currently shown; changing it clears the carried input
 * @param stepIndex    the step being shown, mirrored into ?step
 * @param totalSteps   used to ignore an out-of-range ?step from an older or edited link
 * @param onRestore    called once per problem with {step, input} recovered from the URL
 */
export default function useShareableView({ problemId, stepIndex, totalSteps, onRestore }) {
  const [params, setParams] = useSearchParams();
  const restoredFor = useRef(null);
  const paramsRef = useRef(params);
  paramsRef.current = params;

  // Restore once per problem, before any mirroring can overwrite what the link carried.
  useEffect(() => {
    if (!problemId || restoredFor.current === problemId) return;
    restoredFor.current = problemId;

    const rawStep = Number(paramsRef.current.get(STEP));
    onRestore({
      step: Number.isInteger(rawStep) && rawStep > 0 ? rawStep - 1 : null,
      input: decodeInput(paramsRef.current.get(INPUT))
    });
    // onRestore is re-created every render by callers; depending on it would restore on
    // every render instead of once per problem.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [problemId]);

  // Mirror the step outward. 1-based in the URL: step=1 is the first step, which is what
  // the controls show and what anyone reading the link expects.
  useEffect(() => {
    if (restoredFor.current !== problemId) return;
    if (!Number.isInteger(stepIndex) || totalSteps <= 0) return;
    const next = new URLSearchParams(paramsRef.current);
    if (stepIndex <= 0) next.delete(STEP);
    else next.set(STEP, String(stepIndex + 1));
    if (next.toString() !== paramsRef.current.toString()) {
      setParams(next, { replace: true });
    }
  }, [problemId, stepIndex, totalSteps, setParams]);

  /** Call when a run happens on caller-supplied input; pass null to drop it from the URL. */
  const shareInput = (values) => {
    const next = new URLSearchParams(paramsRef.current);
    const encoded = values ? encodeInput(values) : null;
    if (encoded) next.set(INPUT, encoded);
    else next.delete(INPUT);
    setParams(next, { replace: true });
  };

  return { shareInput };
}
