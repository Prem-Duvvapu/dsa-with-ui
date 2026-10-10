import { useState, useEffect, useRef, useCallback } from 'react';
import { decodeTrace } from '../trace/decodeTrace';

/**
 * Owns every piece of state between "the user clicked a problem" and "the canvas
 * has something to draw": fetch, abort, stale-response guard, delta decoding,
 * and the playback clock.
 *
 * App.jsx used to hold all of this inline — ~120 lines of useState/useEffect/useRef
 * threaded through five effects and two async functions. Pulling it out makes the
 * data layer testable in isolation and lets App focus on layout.
 *
 * The hook does NOT own the catalogue fetch (GET /api/problems). That is a one-time
 * load with different error handling; mixing it in would muddy the interface.
 *
 * A RUN is one committed unit: its steps, the input it ran on, its anchors and whether it
 * hit the step budget. They used to be five independent setters, and a custom run replaced
 * the steps while leaving `resolvedInput` describing the default - the "Running on" line
 * then stated an input the animation was not running. Now a run is replaced whole, and
 * only by a response that is still the newest request after its body has been decoded.
 *
 * A failed RERUN keeps the last valid run and reports `rerunFailure`, so the screen can
 * say "previous result; the new run failed" instead of blanking the one thing the learner
 * was looking at. The very first load has no previous run to keep, so its failure is
 * `error` as before.
 */

const EMPTY_RUN = Object.freeze({
  id: 0,
  problemId: null,
  steps: [],
  resolvedInput: null,
  anchors: null,
  truncated: false,
  submittedInput: null,
  offline: false,
  approachId: null,
  approachLabel: null,
  code: null,
  dsType: null,
  complexity: null,
  hasApproachMetadata: false
});

/** A deep copy of an input map, so later edits to the draft cannot rewrite what ran. */
function snapshot(values) {
  try {
    return JSON.parse(JSON.stringify(values ?? {}));
  } catch {
    return {};
  }
}

/** Classifies a successful execute body without turning broken data into a fake trace. */
function classifyExecValue(execValue) {
  let decoded;

  try {
    if (Array.isArray(execValue)) {
      decoded = execValue;
    } else if (execValue && Array.isArray(execValue.steps)) {
      decoded = decodeTrace(execValue);
    } else {
      return { kind: 'malformed', steps: [] };
    }
  } catch {
    return { kind: 'malformed', steps: [] };
  }

  if (!decoded.every(step => step && typeof step === 'object' && !Array.isArray(step)
      && Number.isInteger(step.stepNumber) && typeof step.description === 'string')) {
    return { kind: 'malformed', steps: [] };
  }
  if (decoded.length === 0) return { kind: 'empty', steps: [] };
  return { kind: 'ok', steps: decoded };
}

/** The run a decoded body describes. A legacy bare-array body carries no metadata. */
function runFrom(id, problemId, body, steps, submittedInput) {
  const envelope = Array.isArray(body) ? null : body;
  return {
    id,
    problemId,
    steps,
    resolvedInput: envelope?.resolvedInput ?? submittedInput ?? null,
    anchors: envelope?.anchors ?? null,
    truncated: envelope?.truncated === true,
    submittedInput,
    offline: false,
    approachId: envelope?.approachId ?? null,
    approachLabel: envelope?.approachLabel ?? null,
    code: envelope?.code ?? null,
    dsType: envelope?.dsType ?? null,
    complexity: envelope?.complexity ?? null,
    hasApproachMetadata: typeof envelope?.approachId === 'string'
  };
}

/** A selected approach cannot silently receive another executable or partial metadata. */
function matchingExecution(body, problemId, approachId = null) {
  if (body?.problemId != null && body.problemId !== problemId) return false;
  if (approachId !== null && body?.approachId !== approachId) return false;
  if (approachId !== null || body?.approachId != null) {
    return body?.problemId === problemId && typeof body.code === 'string' && body.code.trim().length > 0
      && typeof body.dsType === 'string'
      && typeof body.truncated === 'boolean'
      && body.resolvedInput && typeof body.resolvedInput === 'object' && !Array.isArray(body.resolvedInput)
      && body.anchors && typeof body.anchors === 'object' && !Array.isArray(body.anchors);
  }
  return true; // Older canonical/bare-array contracts remain supported, not selected alternatives.
}

function executeUrl(problemId, approachId = null) {
  return `/api/problems/${problemId}/execute${approachId === null ? '' : `?approach=${encodeURIComponent(approachId)}`}`;
}

/**
 * @param {string|null} problemId  the currently selected problem id
 * @param {object|null} problem    the catalogue entry (for checked-in offline steps)
 * @param {{initialSpeed?: number}} [options]  persisted preferences to start from
 * @returns playback state + controls
 */
export default function useTrace(problemId, problem, options = {}) {
  const [run, setRun] = useState(EMPTY_RUN);
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  // 1000ms is the "1.0x" playback preset — the only default that lands on a real
  // button. 800ms matched none of the 2000/1000/500/250 presets, so nothing was ever
  // highlighted at startup. App passes the persisted preference in as initialSpeed so a
  // reload does not silently reset someone who prefers 4x.
  const [speed, setSpeed] = useState(options.initialSpeed ?? 1000);
  /** The problem's first (default) trace is loading; nothing valid is drawable yet. */
  const [loading, setLoading] = useState(false);
  /** A submitted input is in flight. The current run stays drawable meanwhile. */
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);
  /** { kind, input } for the most recent failed submission, kept beside the old run. */
  const [rerunFailure, setRerunFailure] = useState(null);
  /** Per-field messages from the last rejected POST /execute. Cleared on any success. */
  const [fieldErrors, setFieldErrors] = useState({});
  /** The full per-problem detail (javaCode, complexity, defaultGraphNodes, ...) —
   *  the catalogue list endpoint only carries summary fields, so canvases and the
   *  code/complexity panels need this merged in by the caller. Stored with the id it
   *  belongs to, so the render that switches problems can never offer the old one. */
  const [detailState, setDetailState] = useState({ problemId: null, value: null });
  /** The problem whose first load has finished (either way); null while it is in flight. */
  const [settledFor, setSettledFor] = useState(null);

  const timerRef = useRef(null);
  /** Monotonic counter: a slower earlier response must never overwrite a newer one. */
  const requestIdRef = useRef(0);
  const abortRef = useRef(null);
  const runIdRef = useRef(0);
  /** The request id of the latest submission (POST), to tell it apart from a default load. */
  const submissionRef = useRef(0);
  /** The committed run, readable synchronously by seek() right after a commit. */
  const runRef = useRef(EMPTY_RUN);
  /** Read at failure time only: changing the entry without the id must not refetch. */
  const problemRef = useRef(problem);
  problemRef.current = problem;

  const commit = useCallback((next) => {
    runRef.current = next;
    setRun(next);
    setCurrentStepIndex(0);
    setIsPlaying(false);
  }, []);

  // ── Fetch trace on problem change ──────────────────────────────────────────
  useEffect(() => {
    if (!problemId) return undefined;

    const requestId = ++requestIdRef.current;

    // Abort any in-flight request from a previous selection.
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    // Detail has its own lifetime: a custom run superseding the default trace must not
    // cancel the problem's code and complexity along with it.
    const detailController = new AbortController();

    commit(EMPTY_RUN);
    setError(null);
    setRerunFailure(null);
    setFieldErrors({});
    setPending(false);
    setLoading(true);
    setSettledFor(null);
    setDetailState({ problemId, value: null });

    fetch(`/api/problems/${problemId}`, { signal: detailController.signal })
      .then(r => (r.ok ? r.json() : null))
      .catch(() => null)
      .then((value) => {
        if (detailController.signal.aborted) return;
        setDetailState({ problemId, value: value && typeof value === 'object' ? value : null });
      });

    const isCurrent = () => requestId === requestIdRef.current;

    (async () => {
      let failure = null;
      try {
        const response = await fetch(`/api/problems/${problemId}/execute`, { signal: controller.signal });
        if (!isCurrent()) return;

        if (response.status === 404) failure = 'notfound';
        else if (response.status === 501) failure = 'untraced';
        else if (!response.ok) failure = 'fetch';

        if (!failure) {
          let body;
          try {
            body = await response.json();
          } catch {
            failure = 'malformed';
          }
          // Decoding is its own await: a newer request can start while it runs.
          if (!isCurrent()) return;

          if (!failure) {
            const classified = classifyExecValue(body);
            if (classified.kind === 'ok' && matchingExecution(body, problemId)) {
              commit(runFrom(++runIdRef.current, problemId, body, classified.steps, null));
              setError(null);
              return;
            }
            failure = classified.kind === 'ok' ? 'malformed' : classified.kind;
          }
        }
      } catch (err) {
        if (err.name === 'AbortError' || !isCurrent()) return;
        console.error('useTrace fetch error:', err);
        failure = 'fetch';
      } finally {
        if (isCurrent()) {
          setLoading(false);
          setSettledFor(problemId);
        }
      }

      if (!isCurrent()) return;
      // Only a network-level failure may fall back to the checked-in sample, and only
      // to the sample for THIS problem. It is labelled offline everywhere it is drawn.
      const offlineSteps = failure === 'fetch' && Array.isArray(problemRef.current?.executionSteps)
        ? problemRef.current.executionSteps
        : null;
      if (offlineSteps?.length) {
        commit({ ...EMPTY_RUN, id: ++runIdRef.current, problemId, steps: offlineSteps, offline: true });
      }
      setError(failure);
    })();

    return () => {
      controller.abort();
      detailController.abort();
    };
  }, [problemId, commit]);

  // ── Run against caller-supplied input ───────────────────────────────────────
  /**
   * POSTs to /api/problems/{id}/execute with a snapshot of the given input and resolves to
   * an explicit outcome, so callers can act on success (share the link) without guessing:
   *
   *   { ok: true, run }                         committed; step 1, paused
   *   { ok: false, kind: 'invalid', fieldErrors }  400 - run and draft both kept
   *   { ok: false, kind: 'fetch'|'malformed'|'empty'|'untraced'|'rate-limited' }
   *                                             prior run kept, `rerunFailure` set
   *   { ok: false, kind: 'superseded' }         a newer request or problem replaced it
   */
  const runInput = useCallback(async (inputValues, approachId = null, { asDefault = false } = {}) => {
    if (!problemId) return { ok: false, kind: 'superseded' };

    const submitted = snapshot(inputValues);
    const requestId = ++requestIdRef.current;
    submissionRef.current = requestId;
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    const isCurrent = () => requestId === requestIdRef.current;

    // A submission supersedes the default load too, so that load can no longer clear it.
    setLoading(false);
    setIsPlaying(false);
    setPending(true);
    setFieldErrors({});
    setRerunFailure(null);

    const fail = (kind) => {
      if (!isCurrent()) return { ok: false, kind: 'superseded' };
      setRerunFailure({ kind, input: submitted, approachId });
      return { ok: false, kind };
    };

    try {
      const res = await fetch(executeUrl(problemId, approachId), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(submitted),
        signal: controller.signal
      });
      if (!isCurrent()) return { ok: false, kind: 'superseded' };

      if (res.status === 400) {
        const body = await res.json().catch(() => null);
        if (!isCurrent()) return { ok: false, kind: 'superseded' };
        if (body?.error === 'unavailable_approach') return fail('unavailable-approach');
        const errors = body?.fieldErrors && typeof body.fieldErrors === 'object' ? body.fieldErrors : {};
        setFieldErrors(errors);
        return { ok: false, kind: 'invalid', fieldErrors: errors, message: body?.message ?? null };
      }
      if (res.status === 501) return fail('untraced');
      if (res.status === 429) return fail('rate-limited');
      if (!res.ok) return fail('fetch');

      let body;
      try {
        body = await res.json();
      } catch {
        return fail('malformed');
      }
      if (!isCurrent()) return { ok: false, kind: 'superseded' };

      const classified = classifyExecValue(body);
      if (classified.kind !== 'ok') return fail(classified.kind);
      if (!matchingExecution(body, problemId, approachId)) return fail('malformed');

      const next = runFrom(++runIdRef.current, problemId, body, classified.steps, asDefault ? null : submitted);
      commit(next);
      setError(null);
      return { ok: true, run: next };
    } catch (err) {
      if (err.name === 'AbortError' || !isCurrent()) return { ok: false, kind: 'superseded' };
      return fail('fetch');
    } finally {
      if (isCurrent()) {
        setPending(false);
        // A submission that superseded the default load is this problem's first result.
        setSettledFor(problemId);
      }
    }
  }, [problemId, commit]);

  const dismissRerunFailure = useCallback(() => setRerunFailure(null), []);

  /**
   * Retires a submission still in flight, so its answer can neither commit nor be shared.
   * Called when the URL moves on without a problem change (a same-page link, Back/Forward):
   * the old submission belonged to the old URL. The problem's own default load is not a
   * submission and is left alone - it is the run the new URL may need.
   */
  const retireSubmission = useCallback(() => {
    if (submissionRef.current !== requestIdRef.current) return;
    requestIdRef.current += 1;
    abortRef.current?.abort();
    setPending(false);
  }, []);

  // Leaving the page ends its authority: the request in flight is aborted and its
  // generation retired, so a late answer cannot commit, share or navigate anywhere.
  useEffect(() => () => {
    requestIdRef.current += 1;
    abortRef.current?.abort();
  }, []);

  // Everything below describes the problem that was ASKED FOR. In the render that switches
  // problems - before the reset effect runs - the state still holds the previous problem's
  // run, and handing that out under the new id let consumers pair one problem with another
  // problem's steps. Until this problem has its own run or its own settled outcome, it is
  // simply loading.
  const own = Boolean(problemId) && (run.problemId === problemId || settledFor === problemId);
  const activeRun = own ? run : EMPTY_RUN;
  const steps = activeRun.steps;

  // ── Playback clock ─────────────────────────────────────────────────────────
  useEffect(() => {
    if (isPlaying) {
      timerRef.current = setInterval(() => {
        setCurrentStepIndex(prev => {
          if (prev >= steps.length - 1) {
            setIsPlaying(false);
            return prev;
          }
          return prev + 1;
        });
      }, speed);
    } else {
      clearInterval(timerRef.current);
    }
    return () => clearInterval(timerRef.current);
  }, [isPlaying, speed, steps.length]);

  // A hidden tab pauses rather than playing to the end unseen; it does not resume by itself.
  useEffect(() => {
    const onVisibility = () => { if (document.hidden) setIsPlaying(false); };
    document.addEventListener('visibilitychange', onVisibility);
    return () => document.removeEventListener('visibilitychange', onVisibility);
  }, []);

  // ── Controls ───────────────────────────────────────────────────────────────
  const currentStep = steps[currentStepIndex] || null;

  const play = useCallback(() => {
    if (steps.length > 0) setIsPlaying(true);
  }, [steps.length]);
  const pause = useCallback(() => setIsPlaying(false), []);
  const togglePlay = useCallback(() => {
    if (steps.length > 0) setIsPlaying(p => !p);
  }, [steps.length]);

  const stepNext = useCallback(() => {
    setIsPlaying(false);
    setCurrentStepIndex(p => steps.length > 0 ? Math.min(p + 1, steps.length - 1) : 0);
  }, [steps.length]);

  const stepPrev = useCallback(() => {
    setIsPlaying(false);
    setCurrentStepIndex(p => Math.max(p - 1, 0));
  }, []);

  const reset = useCallback(() => {
    setIsPlaying(false);
    setCurrentStepIndex(0);
  }, []);

  // Clamped against the committed run itself, so a seek issued right after a commit (a
  // shared link restoring its step) lands inside the new run, not the old one.
  const seek = useCallback((idx) => {
    const total = runRef.current.steps.length;
    setIsPlaying(false);
    setCurrentStepIndex(total > 0 ? Math.min(Math.max(idx, 0), total - 1) : 0);
  }, []);

  return {
    run: activeRun,
    steps,
    currentStep: own ? currentStep : null,
    currentStepIndex: own ? currentStepIndex : 0,
    isPlaying: own && isPlaying,
    speed,
    loading: loading || (Boolean(problemId) && !own),
    pending: own && pending,
    error: own ? error : null,
    rerunFailure: own ? rerunFailure : null,
    truncated: activeRun.truncated,
    resolvedInput: activeRun.resolvedInput,
    anchors: activeRun.anchors,
    fieldErrors: own ? fieldErrors : {},
    detail: detailState.problemId === problemId ? detailState.value : null,
    settled: settledFor === problemId && Boolean(problemId),
    play,
    pause,
    togglePlay,
    stepNext,
    stepPrev,
    reset,
    seek,
    setSpeed,
    runInput,
    retireSubmission,
    dismissRerunFailure
  };
}
