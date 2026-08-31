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
 */

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

/**
 * @param {string|null} problemId  the currently selected problem id
 * @param {object|null} problem    the catalogue entry (for checked-in offline steps)
 * @returns playback state + controls
 */
export default function useTrace(problemId, problem) {
  const [steps, setSteps] = useState([]);
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  // 1000ms is the "1.0x" preset in Controls — the only default that lands on a real
  // button. 800ms matched none of the 2000/1000/500/250 presets, so nothing was ever
  // highlighted at startup.
  const [speed, setSpeed] = useState(1000);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  /** true when the last successful run hit the server's step budget. */
  const [truncated, setTruncated] = useState(false);
  /** Per-field messages from the last rejected POST /execute. Cleared on any success. */
  const [fieldErrors, setFieldErrors] = useState({});
  /** The full per-problem detail (javaCode, complexity, defaultGraphNodes, ...) —
   *  the catalogue list endpoint only carries summary fields, so canvases and the
   *  code/complexity panels need this merged in by the caller. */
  const [detail, setDetail] = useState(null);

  const timerRef = useRef(null);
  /** Monotonic counter: a slower earlier response must never overwrite a newer one. */
  const requestIdRef = useRef(0);
  const abortRef = useRef(null);

  // ── Fetch trace on problem change ──────────────────────────────────────────
  useEffect(() => {
    if (!problemId) return;

    const requestId = ++requestIdRef.current;

    // Abort any in-flight request from a previous selection.
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    setIsPlaying(false);
    setCurrentStepIndex(0);
    setError(null);
    setFieldErrors({});
    setLoading(true);
    setDetail(null);
    setSteps([]);
    setTruncated(false);

    (async () => {
      try {
        // Try the v2 unified endpoint first.
        const [detailRes, execRes] = await Promise.allSettled([
          fetch(`/api/problems/${problemId}`, { signal: controller.signal })
            .then(r => r.ok ? r.json() : null),
          fetch(`/api/problems/${problemId}/execute`, { signal: controller.signal })
            .then(async r => {
              if (r.status === 501) return { kind: 'untraced' };
              if (!r.ok) return { kind: 'fetch' };
              try {
                return { kind: 'body', value: await r.json() };
              } catch {
                return { kind: 'malformed' };
              }
            })
        ]);

        // A newer selection landed while these were in flight — discard.
        if (requestId !== requestIdRef.current) return;

        const detailValue = detailRes.status === 'fulfilled' ? detailRes.value : null;
        setDetail(detailValue);

        const execOutcome = execRes.status === 'fulfilled'
          ? execRes.value
          : { kind: 'fetch' };

        if (execOutcome?.kind === 'untraced') {
          setSteps([]);
          setTruncated(false);
          setError('untraced');
          setLoading(false);
          return;
        }

        if (execOutcome?.kind === 'fetch') {
          const offlineSteps = Array.isArray(problem?.executionSteps)
            ? problem.executionSteps
            : [];
          setSteps(offlineSteps);
          setTruncated(false);
          setCurrentStepIndex(0);
          setError('fetch');
          return;
        }

        if (execOutcome?.kind === 'malformed') {
          setSteps([]);
          setTruncated(false);
          setCurrentStepIndex(0);
          setError('malformed');
          return;
        }

        const classified = classifyExecValue(execOutcome?.value);
        if (classified.kind !== 'ok') {
          setSteps([]);
          setTruncated(false);
          setCurrentStepIndex(0);
          setError(classified.kind);
          return;
        }

        setSteps(classified.steps);
        setTruncated(!Array.isArray(execOutcome.value)
          && execOutcome.value?.truncated === true);
        setCurrentStepIndex(0);
        setError(null);
      } catch (err) {
        if (err.name === 'AbortError') return;
        if (requestId !== requestIdRef.current) return;
        console.error('useTrace fetch error:', err);
        setError('fetch');
      } finally {
        if (requestId === requestIdRef.current) {
          setLoading(false);
        }
      }
    })();

    return () => controller.abort();
  }, [problemId]); // eslint-disable-line react-hooks/exhaustive-deps
  // `problem` is intentionally excluded: changing it without changing the id should
  // not refetch. The id alone drives the request lifecycle.

  // ── Run against caller-supplied input ───────────────────────────────────────
  /**
   * POSTs to /api/problems/{id}/execute with the given input. A 400 attaches
   * fieldErrors and leaves the current animation on screen — the point of inline
   * field errors is that the learner sees what to fix without losing their place.
   */
  const runInput = useCallback(async (inputValues) => {
    if (!problemId) return;

    const requestId = ++requestIdRef.current;
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    setLoading(true);
    try {
      const res = await fetch(`/api/problems/${problemId}/execute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(inputValues || {}),
        signal: controller.signal
      });
      if (requestId !== requestIdRef.current) return;

      if (res.status === 400) {
        const body = await res.json().catch(() => null);
        setFieldErrors(body?.fieldErrors || {});
        setError(null);
        return;
      }
      if (res.status === 501) {
        setFieldErrors({});
        setSteps([]);
        setTruncated(false);
        setCurrentStepIndex(0);
        setIsPlaying(false);
        setError('untraced');
        return;
      }
      if (!res.ok) {
        setFieldErrors({});
        setSteps([]);
        setTruncated(false);
        setCurrentStepIndex(0);
        setIsPlaying(false);
        setError('fetch');
        return;
      }

      let body;
      try {
        body = await res.json();
      } catch {
        setFieldErrors({});
        setSteps([]);
        setTruncated(false);
        setCurrentStepIndex(0);
        setIsPlaying(false);
        setError('malformed');
        return;
      }

      const classified = classifyExecValue(body);
      if (classified.kind !== 'ok') {
        setFieldErrors({});
        setSteps([]);
        setTruncated(false);
        setCurrentStepIndex(0);
        setIsPlaying(false);
        setError(classified.kind);
        return;
      }

      setFieldErrors({});
      setSteps(classified.steps);
      setTruncated(body?.truncated === true);
      setCurrentStepIndex(0);
      setIsPlaying(false);
      setError(null);
    } catch (err) {
      if (err.name === 'AbortError') return;
      if (requestId !== requestIdRef.current) return;
      setFieldErrors({});
      setSteps([]);
      setTruncated(false);
      setCurrentStepIndex(0);
      setIsPlaying(false);
      setError('fetch');
    } finally {
      if (requestId === requestIdRef.current) {
        setLoading(false);
      }
    }
  }, [problemId]);

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

  const seek = useCallback((idx) => {
    setIsPlaying(false);
    setCurrentStepIndex(steps.length > 0
      ? Math.min(Math.max(idx, 0), steps.length - 1)
      : 0);
  }, [steps.length]);

  return {
    steps,
    currentStep,
    currentStepIndex,
    isPlaying,
    speed,
    loading,
    error,
    truncated,
    fieldErrors,
    detail,
    play,
    pause,
    togglePlay,
    stepNext,
    stepPrev,
    reset,
    seek,
    setSpeed,
    runInput
  };
}
