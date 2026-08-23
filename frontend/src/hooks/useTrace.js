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

/** Decodes a v2 execute response body into a plain steps array, or [] if unusable. */
function decodeExecValue(execValue) {
  if (Array.isArray(execValue)) return execValue;
  if (execValue?.steps) return decodeTrace(execValue);
  return [];
}

/**
 * @param {string|null} problemId  the currently selected problem id
 * @param {object|null} problem    the catalogue entry (for fallback steps/dsType)
 * @returns playback state + controls
 */
export default function useTrace(problemId, problem) {
  const [steps, setSteps] = useState([]);
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(800);
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

    (async () => {
      try {
        // Try the v2 unified endpoint first.
        const [detailRes, execRes] = await Promise.allSettled([
          fetch(`/api/problems/${problemId}`, { signal: controller.signal })
            .then(r => r.ok ? r.json() : null),
          fetch(`/api/problems/${problemId}/execute`, { signal: controller.signal })
            .then(r => {
              if (r.status === 501) return { untraced: true };
              return r.ok ? r.json() : null;
            })
        ]);

        // A newer selection landed while these were in flight — discard.
        if (requestId !== requestIdRef.current) return;

        const detailValue = detailRes.status === 'fulfilled' ? detailRes.value : null;
        setDetail(detailValue);

        // Execution response: could be a trace response (with steps array or top-level
        // array), or an untraced marker, or null on error.
        const execValue = execRes.status === 'fulfilled' ? execRes.value : null;

        if (execValue?.untraced) {
          setSteps([]);
          setTruncated(false);
          setError('untraced');
          setLoading(false);
          return;
        }

        const decoded = decodeExecValue(execValue);

        if (decoded.length > 0) {
          setSteps(decoded);
        } else if (detailValue?.executionSteps?.length > 0) {
          // Fallback to steps embedded in the detailValue response (legacy shape).
          setSteps(detailValue.executionSteps);
        } else if (problem?.executionSteps?.length > 0) {
          // Fallback to catalogue-embedded steps (cold-start data).
          setSteps(problem.executionSteps);
        } else {
          setSteps([{
            stepNumber: 1,
            activeLine: 1,
            description: `Interactive execution visualizer for ${detailValue?.title || problem?.title || problemId}.`,
            variables: { Status: 'Loaded', Algorithm: detailValue?.title || problem?.title || problemId },
            dsType: detailValue?.dsType || problem?.dsType || 'Array'
          }]);
        }

        setTruncated(!Array.isArray(execValue) && execValue?.truncated === true);
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
        return;
      }
      if (res.status === 501) {
        setFieldErrors({});
        setSteps([]);
        setTruncated(false);
        setError('untraced');
        return;
      }
      if (!res.ok) {
        setFieldErrors({});
        setError('fetch');
        return;
      }

      const body = await res.json();
      const decoded = decodeExecValue(body);
      setFieldErrors({});
      setSteps(decoded);
      setTruncated(body?.truncated === true);
      setCurrentStepIndex(0);
      setIsPlaying(false);
      setError(null);
    } catch (err) {
      if (err.name === 'AbortError') return;
      if (requestId !== requestIdRef.current) return;
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

  const play = useCallback(() => setIsPlaying(true), []);
  const pause = useCallback(() => setIsPlaying(false), []);
  const togglePlay = useCallback(() => setIsPlaying(p => !p), []);

  const stepNext = useCallback(() => {
    setIsPlaying(false);
    setCurrentStepIndex(p => Math.min(p + 1, steps.length - 1));
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
    setCurrentStepIndex(idx);
  }, []);

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
