import { useEffect, useLayoutEffect, useState, useCallback, useRef } from 'react';
import { decodeTrace } from '../trace/decodeTrace';
import { validComparisonSteps } from '../trace/validComparisonSteps';
import { sameInput } from './useInputDraft';

async function fetchRun(problemId, body, signal, approachId) {
  const query = approachId ? `?approach=${encodeURIComponent(approachId)}` : '';
  const res = await fetch(`/api/problems/${problemId}/execute${query}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body), signal
  });
  if (!res.ok) throw new Error(`execute failed: ${res.status}`);
  const data = await res.json();
  if (data?.problemId !== problemId || data?.approachId !== (approachId ?? 'canonical')
      || !['full', 'delta'].includes(data.encoding)
      || typeof data.truncated !== 'boolean' || !data.resolvedInput
      || typeof data.resolvedInput !== 'object' || Array.isArray(data.resolvedInput)
      // The server fills omitted defaults. Every explicitly submitted value must match.
      || !Object.entries(body).every(([key, value]) => Object.hasOwn(data.resolvedInput, key)
        && sameInput(data.resolvedInput[key], value))) {
    throw new Error('Comparison returned a different executable or input');
  }
  const steps = decodeTrace(data);
  if (!validComparisonSteps(steps)) {
    throw new Error('Comparison trace is empty or malformed');
  }
  return { steps, resolvedInput: data.resolvedInput, truncated: data.truncated };
}

const EMPTY = { loading: false, error: null, defaultRun: null, alternateRun: null };

/** Isolated default/other-case runs, requested only while the panel is open. */
export default function useComparisonTrace(problemId, alternateInput, isActive, approachId = null) {
  const [state, setState] = useState(EMPTY);
  // A fresh input literal on render must not trigger an execution loop. Snapshot at load.
  const alternateInputRef = useRef(alternateInput);
  const requestRef = useRef(0);
  const abortRef = useRef(null);
  const mounted = useRef(false);
  const owner = useRef(null);
  useLayoutEffect(() => { alternateInputRef.current = alternateInput; }, [alternateInput]);

  const load = useCallback(async () => {
    if (!mounted.current || owner.current.problemId !== problemId
      || owner.current.approachId !== approachId || owner.current.isActive !== isActive) return;
    const request = ++requestRef.current;
    abortRef.current?.abort();
    abortRef.current = null;
    if (!problemId || !isActive) return;
    const controller = new AbortController();
    abortRef.current = controller;
    const identity = { problemId, approachId };
    setState({ ...EMPTY, ...identity, loading: true });
    try {
      const alternate = JSON.parse(JSON.stringify(alternateInputRef.current ?? {}));
      const [defaultRun, alternateRun] = await Promise.all([
        fetchRun(problemId, {}, controller.signal, approachId),
        fetchRun(problemId, alternate, controller.signal, approachId)
      ]);
      // Abort does not necessarily cancel body decoding. Retire after BOTH decodes.
      if (!mounted.current || request !== requestRef.current) return;
      setState({ ...identity, loading: false, error: null, defaultRun, alternateRun });
    } catch (error) {
      if (!mounted.current || request !== requestRef.current) return;
      controller.abort();
      console.warn('Comparison trace fetch failed:', error);
      setState({ ...EMPTY, ...identity, error: 'Could not load both valid runs to compare.' });
    } finally {
      if (request === requestRef.current) abortRef.current = null;
    }
  }, [problemId, isActive, approachId]);

  useLayoutEffect(() => {
    owner.current = { problemId, approachId, isActive };
    mounted.current = true;
    return () => {
      mounted.current = false;
      requestRef.current += 1;
      abortRef.current?.abort();
      abortRef.current = null;
    };
  }, [problemId, approachId, isActive]);

  useEffect(() => { load(); }, [load]);

  const own = isActive && state.problemId === problemId && state.approachId === approachId;
  return { ...(own ? state : EMPTY), retry: load };
}
