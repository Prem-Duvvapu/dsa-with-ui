import { useCallback, useEffect, useRef } from 'react';
import useLatestSearchParams from './useLatestSearchParams';

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
 *
 * This is the ONE writer of the problem route's query string (`step`, `input`, `view`).
 * Every write is a merge onto the latest parameters this hook has produced, not onto the
 * ones captured by the render that scheduled it - two writes in the same tick (a view
 * change and a step change, say) used to race, and the second silently dropped the first.
 */

const STEP = 'step';
const INPUT = 'input';
const VIEW = 'view';

/**
 * Past this an input link stops being safely shareable: some servers and chat clients cut
 * URLs near 8 KB, and an input that large is better saved as a preset than mailed around.
 */
export const MAX_SHARED_INPUT_LENGTH = 4000;

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
 * @param totalSteps   steps in the run that belongs to `problemId`; 0 while none is loaded
 * @param mirror       false while a link is being restored, so nothing overwrites it
 * @param onRestore    called once per problem with {step, input} recovered from the URL
 */
export default function useShareableView({ problemId, stepIndex, totalSteps, mirror = true, onRestore }) {
  // Every write merges onto the latest URL this hook produced, even while the router's
  // transition has not delivered it yet (see useLatestSearchParams).
  const [params, update] = useLatestSearchParams();
  const latest = useRef(params);
  latest.current = params;
  const onRestoreRef = useRef(onRestore);
  onRestoreRef.current = onRestore;

  const restoredFor = useRef(null);

  // Restore once per problem, before any mirroring can overwrite what the link carried.
  useEffect(() => {
    if (!problemId || restoredFor.current === problemId) return;
    restoredFor.current = problemId;

    const rawStep = Number(latest.current.get(STEP));
    onRestoreRef.current?.({
      step: Number.isInteger(rawStep) && rawStep > 0 ? rawStep - 1 : null,
      input: decodeInput(latest.current.get(INPUT))
    });
  }, [problemId]);

  // Mirror the step outward. 1-based in the URL: step=1 is the first step, which is what
  // the controls show and what anyone reading the link expects.
  useEffect(() => {
    if (!mirror || restoredFor.current !== problemId) return;
    if (!Number.isInteger(stepIndex) || totalSteps <= 0) return;
    update((next) => {
      if (stepIndex <= 0) next.delete(STEP);
      else next.set(STEP, String(stepIndex + 1));
    });
  }, [problemId, stepIndex, totalSteps, mirror, update]);

  /**
   * Call after a run on caller-supplied input has SUCCEEDED; pass null to drop it from the
   * URL. Returns false when the input cannot be represented in a link (encoding failed or
   * the result is too long), in which case the link carries no input rather than a wrong one.
   */
  const shareInput = useCallback((values) => {
    const encoded = values ? encodeInput(values) : null;
    const fits = Boolean(encoded) && encoded.length <= MAX_SHARED_INPUT_LENGTH;
    update((next) => {
      if (fits) next.set(INPUT, encoded);
      else next.delete(INPUT);
    });
    return values ? fits : true;
  }, [update]);

  /** Presentation only: the default view is written as no parameter at all. */
  const setView = useCallback((view, defaultView = null) => {
    update((next) => {
      if (!view || view === defaultView) next.delete(VIEW);
      else next.set(VIEW, view);
    });
  }, [update]);

  return { shareInput, setView, view: params.get(VIEW) };
}
