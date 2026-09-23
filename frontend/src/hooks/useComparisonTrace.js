import { useEffect, useState, useCallback, useRef } from 'react';
import { decodeTrace } from '../trace/decodeTrace';

async function fetchRun(problemId, body) {
  const res = await fetch(`/api/problems/${problemId}/execute`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body || {})
  });
  if (!res.ok) throw new Error(`execute failed: ${res.status}`);
  const data = await res.json();
  return {
    steps: decodeTrace(data),
    resolvedInput: data?.resolvedInput ?? null
  };
}

/**
 * Fetches the same problem's default run and its tracer-declared alternate run side by
 * side, for the compare panel. `isActive` gates the fetch so opening the panel is what
 * triggers it, not merely having a problem with an alternateInput selected — the compare
 * panel is off by default, and most sessions never open it.
 */
export default function useComparisonTrace(problemId, alternateInput, isActive) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [defaultRun, setDefaultRun] = useState(null);
  const [alternateRun, setAlternateRun] = useState(null);

  // A caller re-rendering with a fresh `{ ...alternateInput }` literal each time (App.jsx's
  // activeProblem is itself rebuilt every render) must not turn into a fetch loop - the
  // value only needs to be current AT FETCH TIME, not part of what decides to refetch.
  const alternateInputRef = useRef(alternateInput);
  alternateInputRef.current = alternateInput;

  const load = useCallback(async () => {
    if (!problemId || !isActive) return;
    setLoading(true);
    setError(null);
    setDefaultRun(null);
    setAlternateRun(null);
    try {
      const [defaultResult, alternateResult] = await Promise.all([
        fetchRun(problemId, {}),
        fetchRun(problemId, alternateInputRef.current)
      ]);
      setDefaultRun(defaultResult);
      setAlternateRun(alternateResult);
    } catch (err) {
      console.warn('Comparison trace fetch failed:', err);
      setError('Could not load both runs to compare.');
    } finally {
      setLoading(false);
    }
  }, [problemId, isActive]);

  useEffect(() => { load(); }, [load]);

  return { loading, error, defaultRun, alternateRun, retry: load };
}
