import React, { StrictMode } from 'react';
import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import useProblemSession from './useProblemSession';
import { encodeInput } from './useShareableView';
import { defaultInput } from '../input/randomizeInput';
import { inputLimitWarnings } from '../input/inputLimitWarnings';

// Deliberately fictional metadata/trace envelope for a shared-session unit contract.
// These are not LCS algorithm responses or evidence that a real LCS tracer exists.
const problemId = 'string-draft-contract';
const options = [
  ['recursion', 4, 'abcd', 'acd'], ['memoization', 8, 'abcde', 'ace'], ['canonical', 12, 'abcde', 'ace']
].map(([id, maxLength, first, second]) => ({ id, label: id === 'canonical' ? 'Tabulation' : id[0].toUpperCase() + id.slice(1),
  summary: 'Unit session definition.', dsType: id === 'canonical' ? 'DpTable' : 'RecursionTree',
  inputSpec: { fields: [
    { name: 'first', label: 'First string', type: 'STRING', defaultValue: first, constraints: { minLength: 1, maxLength } },
    { name: 'second', label: 'Second string', type: 'STRING', defaultValue: second, constraints: { minLength: 1, maxLength } }
  ] }, complexity: { timeComplexity: 'unit complexity' }, alternateInput: { first: 'a', second: 'b' } }));
const definition = id => options.find(option => option.id === id);
const metadata = { title: 'Two-string session unit', ...definition('canonical'),
  id: problemId, approachId: 'canonical', defaultApproachId: 'canonical', approaches: options,
  javaCode: 'unit canonical source' };
const ok = body => ({ ok: true, status: 200, json: async () => structuredClone(body) });
function run(id, input) {
  const option = definition(id);
  return { problemId, approachId: id, approachLabel: option.label, dsType: option.dsType,
    complexity: option.complexity, code: `unit ${id} source`, anchors: { start: 1 }, encoding: 'full',
    resolvedInput: { ...defaultInput(option.inputSpec), ...input }, truncated: false,
    steps: Array.from({ length: 3 }, (_, index) => ({ stepNumber: index + 1, activeLine: 1,
      dsType: option.dsType, variables: {}, callStack: [], description: `unit ${id} step ${index + 1}` })) };
}

let posts, holdBodies, releaseBodies, holdDetail, releaseDetail, reject, navigate, location;
beforeEach(() => {
  posts = []; holdBodies = false; releaseBodies = []; holdDetail = false; releaseDetail = null; reject = false;
  vi.stubGlobal('fetch', vi.fn((url, init = {}) => {
    const parsed = new URL(url, 'https://unit.invalid');
    if (!parsed.pathname.endsWith('/execute')) {
      return holdDetail ? new Promise(resolve => { releaseDetail = () => resolve(ok(metadata)); }) : Promise.resolve(ok(metadata));
    }
    if (init.method !== 'POST') return Promise.resolve(ok(run('canonical', {})));
    const approach = parsed.searchParams.get('approach') ?? 'canonical', input = JSON.parse(init.body);
    posts.push({ approach, input });
    const option = definition(approach);
    if (!option) return Promise.resolve({ ok: false, status: 400, json: async () => ({ error: 'unavailable_approach' }) });
    const resolved = { ...defaultInput(option.inputSpec), ...input };
    const names = new Set(option.inputSpec.fields.map(field => field.name));
    const errors = { ...Object.fromEntries(Object.keys(input).filter(name => !names.has(name))
      .map(name => [name, 'Unknown unit input field.'])), ...Object.fromEntries(option.inputSpec.fields.filter(field =>
      typeof resolved[field.name] === 'string' && resolved[field.name].length > field.constraints.maxLength
    ).map(field => [field.name, 'Too long for selected unit approach.'])) };
    if (reject || Object.keys(errors).length) return Promise.resolve({ ok: false, status: 400,
      json: async () => ({ fieldErrors: Object.keys(errors).length ? errors : { first: 'Unit rejection.' } }) });
    const body = run(approach, input);
    return Promise.resolve({ ok: true, status: 200, json: () => holdBodies
      ? new Promise(resolve => releaseBodies.push(() => resolve(body))) : Promise.resolve(body) });
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });
const address = (approach, input) => `/problem/${problemId}?${new URLSearchParams({ approach, step: '3',
  ...(input === undefined ? {} : { input: encodeInput(input) }) })}`;
function mount(path = `/problem/${problemId}`, { summaryOnly = false, strict = false } = {}) {
  function Probe() { location = useLocation(); navigate = useNavigate(); return null; }
  // Real catalogue summaries include the canonical spec but no approach definitions.
  const catalogEntry = summaryOnly ? { id: problemId, title: metadata.title,
    dsType: metadata.dsType, traced: true, inputSpec: metadata.inputSpec } : metadata;
  const wrapper = ({ children }) => {
    const content = <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes><Route path="/problem/:id" element={<>{children}<Probe /></>} /></Routes>
    </MemoryRouter>;
    return strict ? <StrictMode>{content}</StrictMode> : content;
  };
  return renderHook(() => useProblemSession({ problemId: useParams().id, catalogEntry }), { wrapper });
}
async function loaded() {
  const hook = mount();
  await waitFor(() => expect(hook.result.current.settled).toBe(true));
  return hook;
}
async function restored(result, approach) {
  await waitFor(() => expect(result.current.run.approachId).toBe(approach));
  await waitFor(() => expect(result.current.restoring).toBe(false));
}

describe('two-string approach defaults belong to the restored executable', () => {
  it('preserves pristine and dirty drafts on ordinary candidate selection without running or sharing', async () => {
    const { result } = await loaded();
    const before = location.search;
    act(() => result.current.selectApproach('recursion'));
    expect(result.current.draft.values).toEqual({ first: 'abcde', second: 'ace' });
    expect(inputLimitWarnings(result.current.inputSpec, result.current.draft.values))
      .toEqual([{ field: 'first', message: 'First string has 5 characters; maximum 4.' }]);
    act(() => { result.current.draft.replace({ first: 'abcdefghijkl', second: 'abcdefghi' });
      result.current.selectApproach('memoization'); });
    expect(result.current.draft.values).toEqual({ first: 'abcdefghijkl', second: 'abcdefghi' });
    expect(inputLimitWarnings(result.current.inputSpec, result.current.draft.values)).toHaveLength(2);
    expect(result.current.problem.javaCode).toBe('unit canonical source');
    expect(posts).toEqual([]);
    expect(location.search).toBe(before);
  });

  it('uses requested defaults for a same-page no-input approach link, not the old candidate defaults', async () => {
    const { result } = await loaded();
    await act(async () => result.current.submit({ first: 'xyz', second: 'xz' }));
    act(() => navigate(address('recursion')));
    await restored(result, 'recursion');
    expect(result.current.run.resolvedInput).toEqual({ first: 'abcd', second: 'acd' });
    expect(result.current.draft.values).toEqual(result.current.run.resolvedInput);
    expect(result.current.currentStepIndex).toBe(2);
    expect(result.current.draftChanged).toBe(false);
  });

  it('fills an omitted field from the requested approach on same-page partial-input links in both directions and Back', async () => {
    const { result } = await loaded();
    act(() => navigate(address('recursion', { first: 'xy' })));
    await restored(result, 'recursion');
    expect(result.current.draft.values).toEqual({ first: 'xy', second: 'acd' });
    act(() => navigate(address('canonical', { first: 'uv' })));
    await restored(result, 'canonical');
    expect(result.current.draft.values).toEqual({ first: 'uv', second: 'ace' });
    act(() => navigate(-1));
    await restored(result, 'recursion');
    expect(result.current.draft.values).toEqual({ first: 'xy', second: 'acd' });
  });

  it.each([false, true])('seeds a cold partial-input link from requested defaults (StrictMode=%s)', async strict => {
    const { result } = mount(address('recursion', { first: 'xy' }), { strict });
    await restored(result, 'recursion');
    expect(result.current.draft.values).toEqual({ first: 'xy', second: 'acd' });
  });

  it('allows authoritative late detail to supply only omitted fields without guessing a spec', async () => {
    holdDetail = true;
    const { result } = mount(address('recursion', { first: 'xy' }), { summaryOnly: true });
    await waitFor(() => expect(releaseDetail).toBeTypeOf('function'));
    expect(result.current.draft.values).toEqual({ first: 'xy' });
    act(() => result.current.draft.replace({ first: 'xy', second: 'zz' }));
    await act(async () => releaseDetail());
    await restored(result, 'recursion');
    expect(result.current.run.resolvedInput).toEqual({ first: 'xy', second: 'acd' });
    expect(result.current.draft.values).toEqual({ first: 'xy', second: 'zz' });
  });

  it('does not freeze canonical summary defaults into an edit of only the first field', async () => {
    holdDetail = true;
    const { result } = mount(address('recursion', { first: 'xy' }), { summaryOnly: true });
    await waitFor(() => expect(releaseDetail).toBeTypeOf('function'));
    expect(result.current.inputSpec).toBeNull();
    expect(result.current.draft.values).toEqual({ first: 'xy' });
    act(() => result.current.draft.replace({ ...result.current.draft.values, first: 'uv' }));
    await act(async () => releaseDetail());
    await restored(result, 'recursion');
    expect(result.current.run.resolvedInput).toEqual({ first: 'xy', second: 'acd' });
    expect(result.current.draft.values).toEqual({ first: 'uv', second: 'acd' });
  });

  it('retains a known canonical summary spec on a plain legacy/default route', async () => {
    holdDetail = true;
    const { result } = mount(`/problem/${problemId}`, { summaryOnly: true });
    await waitFor(() => expect(result.current.settled).toBe(true));
    expect(result.current.inputSpec).toEqual(metadata.inputSpec);
    expect(result.current.draft.values).toEqual({ first: 'abcde', second: 'ace' });
  });

  it.each([false, true])('seeds an unresolved same-page no-input reset before later edits (edit=%s)', async edit => {
    holdDetail = true;
    const { result } = mount(`/problem/${problemId}`, { summaryOnly: true });
    await waitFor(() => expect(result.current.settled).toBe(true));
    await act(async () => result.current.submit({ first: 'xy', second: 'xz' }));
    holdBodies = true;
    act(() => navigate(address('recursion')));
    await waitFor(() => expect(releaseBodies).toHaveLength(1));
    expect(result.current.inputSpec).toBeNull();
    expect(result.current.draft.values).toEqual({});
    if (edit) act(() => result.current.draft.replace({ first: 'uv' }));
    await act(async () => { releaseDetail(); releaseBodies[0](); });
    await restored(result, 'recursion');
    expect(result.current.run.resolvedInput).toEqual({ first: 'abcd', second: 'acd' });
    expect(result.current.draft.values).toEqual(edit ? { first: 'uv', second: 'acd' } : { first: 'abcd', second: 'acd' });
  });

  it('never writes a restored response over a later user edit while its body decodes', async () => {
    const { result } = await loaded();
    holdBodies = true;
    act(() => navigate(address('recursion', { first: 'xy' })));
    await waitFor(() => expect(releaseBodies).toHaveLength(1));
    expect(result.current.draft.values).toEqual({ first: 'xy', second: 'acd' });
    act(() => result.current.draft.replace({ first: 'xy', second: 'zz' }));
    await act(async () => releaseBodies[0]());
    await restored(result, 'recursion');
    expect(result.current.draft.values).toEqual({ first: 'xy', second: 'zz' });
    expect(result.current.run.resolvedInput).toEqual({ first: 'xy', second: 'acd' });
    expect(result.current.draftChanged).toBe(true);
  });

  it('keeps a rejected link draft, prior source/run, and truthful prior-run share identity', async () => {
    const { result } = await loaded();
    const priorRun = result.current.run;
    act(() => navigate(address('recursion', { first: 'waytoolong' })));
    await waitFor(() => expect(result.current.linkNotices.length).toBeGreaterThan(0));
    await waitFor(() => expect(result.current.restoring).toBe(false));
    expect(result.current.draft.values).toEqual({ first: 'waytoolong', second: 'acd' });
    expect(result.current.run).toBe(priorRun);
    expect(result.current.problem.javaCode).toBe('unit canonical source');
    expect(new URLSearchParams(location.search).get('approach')).toBeNull();
    expect(new URLSearchParams(location.search).get('input')).toBeNull();
  });

  it('does not borrow defaults from an unknown requested approach', async () => {
    const { result } = await loaded();
    act(() => navigate(address('unknown', { first: 'xy' })));
    await waitFor(() => expect(result.current.linkNotices.some(notice => notice.kind === 'approach')).toBe(true));
    expect(posts.at(-1)).toEqual({ approach: 'unknown', input: { first: 'xy' } });
    expect(result.current.run.approachId).toBe('canonical');
    expect(result.current.problem.javaCode).toBe('unit canonical source');
    // No invented unknown-approach default is asserted or supplied to the server.
  });

  it('filters unknown fields only from the editor while the server receives and rejects the exact link map', async () => {
    const { result } = await loaded();
    act(() => navigate(address('recursion', { first: 'xy', extra: 'unrecognized' })));
    await waitFor(() => expect(result.current.linkNotices.length).toBeGreaterThan(0));
    expect(posts.at(-1)).toEqual({ approach: 'recursion', input: { first: 'xy', extra: 'unrecognized' } });
    expect(result.current.draft.values).toEqual({ first: 'xy', second: 'acd' });
    expect(result.current.run.approachId).toBe('canonical');
    expect(result.current.problem.javaCode).toBe('unit canonical source');
  });
});
