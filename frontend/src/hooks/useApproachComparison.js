import { useCallback, useLayoutEffect, useRef, useState } from 'react';
import { decodeTrace } from '../trace/decodeTrace';
import { validComparisonSteps } from '../trace/validComparisonSteps';
import { sameInput } from './useInputDraft';

async function execute(problemId, approachId, input, signal) {
  const response = await fetch(`/api/problems/${problemId}/execute?approach=${encodeURIComponent(approachId)}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input), signal
  });
  let body;
  let responseJsonBytes = null;
  try {
    if (typeof response.text === 'function') {
      const raw = await response.text();
      responseJsonBytes = new TextEncoder().encode(raw).byteLength;
      body = JSON.parse(raw);
    } else body = await response.json();
  } catch { throw Error('The comparison response was unreadable.'); }
  if (!response.ok) {
    const fields = Object.entries(body?.fieldErrors ?? {}).map(([name, message]) => `${name}: ${message}`).join(' ');
    throw Error(fields || (response.status === 400 ? 'This approach cannot run the recorded input.' : `Request failed (${response.status}).`));
  }
  if (body?.problemId !== problemId || body?.approachId !== approachId
      || !['full', 'delta'].includes(body.encoding)
      || typeof body.truncated !== 'boolean' || !body.resolvedInput
      || typeof body.resolvedInput !== 'object' || Array.isArray(body.resolvedInput)
      || !sameInput(body.resolvedInput, input)) {
    throw Error('The response does not describe the requested approach and input.');
  }
  const steps = decodeTrace(body);
  if (!validComparisonSteps(steps)) {
    throw Error('The comparison trace is empty or malformed.');
  }
  return { ...body, steps, responseJsonBytes };
}

/** Explicit, isolated requests: never executes from an effect or commits to the main session. */
export default function useApproachComparison(problemId) {
  const [state, setState] = useState({ problemId: null, pending: false, results: null });
  const generation = useRef(0);
  const controller = useRef(null);
  const mounted = useRef(false);
  const owner = useRef(problemId);
  const retire = useCallback(() => {
    generation.current += 1;
    controller.current?.abort();
    controller.current = null;
  }, []);
  useLayoutEffect(() => {
    owner.current = problemId;
    mounted.current = true;
    return () => { mounted.current = false; retire(); };
  }, [problemId, retire]);
  const clear = useCallback(() => {
    if (!mounted.current || owner.current !== problemId) return;
    retire();
    setState({ problemId, pending: false, results: null });
  }, [problemId, retire]);
  const compare = useCallback(async (ids, values) => {
    if (!mounted.current || owner.current !== problemId) return;
    retire();
    if (ids.length !== 2 || ids[0] === ids[1]) {
      setState({ problemId, pending: false, results: null });
      return;
    }
    const request = generation.current;
    const input = JSON.parse(JSON.stringify(values));
    const abort = new AbortController();
    controller.current = abort;
    setState({ problemId, pending: true, results: null });
    const outcomes = await Promise.allSettled(ids.map(id => execute(problemId, id, input, abort.signal)));
    if (!mounted.current || request !== generation.current) return;
    setState({ problemId, pending: false, results: outcomes.map((result, i) => ({ id: ids[i],
      run: result.status === 'fulfilled' ? result.value : null,
      error: result.status === 'rejected' ? result.reason.message : null })) });
  }, [problemId, retire]);
  return { ...(state.problemId === problemId ? state : { pending: false, results: null }), compare, clear };
}
