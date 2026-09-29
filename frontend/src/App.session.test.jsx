import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import App from './App';
import { encodeInput, decodeInput } from './hooks/useShareableView';

/**
 * Session truth in the running app (IMPLEMENTATION_HANDOFF.md §6, P1): what is on screen,
 * what the "Running on" line claims, what the editor holds and what the URL shares must all
 * describe the same run. Each case here reproduces a defect observed at 82f2808 against the
 * real backend (REVAMP_TRACKER.md B1–B7).
 */

let latestLocation;
function LocationProbe() {
  latestLocation = useLocation();
  return null;
}

function renderApp(path = '/problem/two-sum') {
  return render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route path="/problem/:id" element={<><App /><LocationProbe /></>} />
        <Route path="/" element={<p>Library home</p>} />
      </Routes>
    </MemoryRouter>
  );
}

const sharedParams = () => new URLSearchParams(latestLocation.search);

function ok(body) {
  return { ok: true, status: 200, json: () => Promise.resolve(body) };
}

const TWO_SUM = {
  id: 'two-sum', title: 'Two Sum', category: 'Arrays', difficulty: 'Easy', dsType: 'Array',
  traced: true, striverSheetSection: 'Arrays - Easy', javaCode: 'int solve() {\n  return 0;\n}',
  complexity: { timeComplexity: 'O(N)', spaceComplexity: 'O(1)' },
  inputSpec: {
    fields: [
      { name: 'nums', label: 'Array', type: 'INT_ARRAY', defaultValue: [2, 7, 11, 15],
        constraints: { minLength: 2, maxLength: 10 } },
      { name: 'target', label: 'Target sum', type: 'INT', defaultValue: 9, constraints: { min: -100, max: 100 } }
    ]
  }
};

const WORD = {
  id: 'reverse-word', title: 'Reverse Word', category: 'Arrays', difficulty: 'Easy', dsType: 'Array',
  traced: true, striverSheetSection: 'Arrays - Easy', javaCode: 'void run() {}',
  inputSpec: { fields: [{ name: 'word', label: 'Word', type: 'STRING', defaultValue: 'abc' }] }
};

function trace(input, stepCount = 3) {
  return {
    encoding: 'delta', truncated: false, resolvedInput: input, anchors: {},
    steps: Array.from({ length: stepCount }, (_, i) => ({
      stepNumber: i + 1, activeLine: 1, keyframe: i === 0, dsType: 'Array', variables: {},
      description: `ran ${JSON.stringify(input)} step ${i + 1}`,
      arrayState: [{ index: 0, value: i, state: 'default' }]
    }))
  };
}

let calls;
let executeOverride;
beforeEach(() => {
  calls = [];
  executeOverride = null;
  vi.stubGlobal('fetch', vi.fn((url, opts = {}) => {
    calls.push({ url, method: opts.method || 'GET', body: opts.body });
    if (url === '/api/problems') return Promise.resolve(ok([TWO_SUM, WORD]));
    const detail = url.match(/^\/api\/problems\/([^/]+)$/);
    if (detail) {
      const found = [TWO_SUM, WORD].find((p) => p.id === detail[1]);
      return Promise.resolve(found ? ok(found) : { ok: false, status: 404, json: () => Promise.resolve(null) });
    }
    const exec = url.match(/^\/api\/problems\/([^/]+)\/execute$/);
    if (exec) {
      const found = [TWO_SUM, WORD].find((p) => p.id === exec[1]);
      if (!found) return Promise.resolve({ ok: false, status: 404, json: () => Promise.resolve(null) });
      if (opts.method === 'POST') {
        if (executeOverride) return executeOverride(JSON.parse(opts.body));
        const body = JSON.parse(opts.body);
        if (found === TWO_SUM && (typeof body.target !== 'number' || body.target > 100)) {
          return Promise.resolve({ ok: false, status: 400, json: () => Promise.resolve({ fieldErrors: { target: 'Must be at most 100.' } }) });
        }
        return Promise.resolve(ok(trace(body, 12)));
      }
      return Promise.resolve(ok(trace(found === TWO_SUM ? { nums: [2, 7, 11, 15], target: 9 } : { word: 'abc' })));
    }
    return Promise.resolve({ ok: false, status: 404, json: () => Promise.resolve(null) });
  }));
});

afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

const DEFAULT_STEP = 'ran {"nums":[2,7,11,15],"target":9} step 1';

async function openEditor() {
  renderApp();
  await screen.findByText(DEFAULT_STEP);
  fireEvent.click(screen.getByRole('button', { name: 'Edit input' }));
  return screen.findByLabelText('Target sum');
}

async function runTarget(value) {
  fireEvent.change(screen.getByLabelText('Target sum'), { target: { value: String(value) } });
  fireEvent.click(screen.getByRole('button', { name: 'Run with this input' }));
}

describe('App session: unknown ids (B1)', () => {
  it('says the problem does not exist instead of opening a different one', async () => {
    renderApp('/problem/does-not-exist');
    expect(await screen.findByRole('heading', { name: /no algorithm called “does-not-exist”/i })).toBeInTheDocument();
    expect(latestLocation.pathname).toBe('/problem/does-not-exist');
    expect(screen.getByRole('link', { name: /browse all algorithms/i })).toHaveAttribute('href', '/');
    // Nothing else was executed in its place.
    expect(calls.some((c) => c.url === '/api/problems/two-sum/execute')).toBe(false);
    expect(screen.queryByText(/ran \{/)).not.toBeInTheDocument();
  });
});

describe('App session: what ran is what is shown (B2, B3)', () => {
  it('states the custom input after a custom run, not the default', async () => {
    await openEditor();
    await runTarget(13);
    await screen.findByText('ran {"nums":[2,7,11,15],"target":13} step 1');

    fireEvent.click(screen.getByRole('button', { name: 'Done editing' }));
    expect(screen.getByTestId('input-summary')).toHaveTextContent(/target\s*13/);
  });

  it('keeps an edited draft when the editor closes and reopens', async () => {
    await openEditor();
    fireEvent.change(screen.getByLabelText('Target sum'), { target: { value: '42' } });

    fireEvent.click(screen.getByRole('button', { name: 'Done editing' }));
    expect(screen.queryByLabelText('Target sum')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Edit input' }));

    expect(screen.getByLabelText('Target sum')).toHaveValue(42);
  });

  it('flags a draft that differs from the run on screen', async () => {
    await openEditor();
    expect(screen.queryByText('Changes not run')).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Target sum'), { target: { value: '42' } });
    expect(screen.getByText('Changes not run')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Run with this input' }));
    await screen.findByText('ran {"nums":[2,7,11,15],"target":42} step 1');
    expect(screen.queryByText('Changes not run')).not.toBeInTheDocument();
  });

  it('initialises a new problem\'s draft from that problem\'s own spec', async () => {
    await openEditor();
    fireEvent.change(screen.getByLabelText('Target sum'), { target: { value: '42' } });

    fireEvent.click(screen.getByRole('button', { name: /Next: Reverse Word/i }));
    await screen.findByText('ran {"word":"abc"} step 1');

    expect(screen.getByLabelText('Word')).toHaveValue('abc');
    expect(screen.queryByLabelText('Target sum')).not.toBeInTheDocument();
  });
});

describe('App session: sharing only what succeeded (B4)', () => {
  it('writes a successful custom input to the link', async () => {
    await openEditor();
    await runTarget(13);
    await screen.findByText('ran {"nums":[2,7,11,15],"target":13} step 1');
    // The URL write is a router transition that lands just after the run renders.
    await waitFor(() => expect(decodeInput(sharedParams().get('input'))).toEqual({ nums: [2, 7, 11, 15], target: 13 }));
  });

  it('never writes a rejected input into the link', async () => {
    await openEditor();
    await runTarget(13);
    await screen.findByText('ran {"nums":[2,7,11,15],"target":13} step 1');
    await waitFor(() => expect(sharedParams().get('input')).not.toBeNull());

    await runTarget(999);
    await screen.findByText('Must be at most 100.');
    await new Promise((resolve) => setTimeout(resolve, 20));
    // The URL still identifies the run on screen.
    expect(decodeInput(sharedParams().get('input'))).toEqual({ nums: [2, 7, 11, 15], target: 13 });
    expect(screen.getByText('ran {"nums":[2,7,11,15],"target":13} step 1')).toBeInTheDocument();
  });

  it('does not share an input whose run failed on the network', async () => {
    await openEditor();
    executeOverride = () => Promise.reject(new Error('offline'));
    await runTarget(5);
    await screen.findByText(/new run failed/i);
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(sharedParams().get('input')).toBeNull();
  });
});

describe('App session: failed reruns keep the previous result (B7)', () => {
  it('labels the old result and retries the failed submission', async () => {
    await openEditor();
    executeOverride = () => Promise.reject(new Error('offline'));
    await runTarget(5);

    const notice = await screen.findByRole('alert', { name: /new run failed/i });
    expect(notice).toHaveTextContent(/previous result/i);
    expect(screen.getByText(DEFAULT_STEP)).toBeInTheDocument();

    executeOverride = (body) => Promise.resolve(ok(trace(body, 4)));
    fireEvent.click(screen.getByRole('button', { name: /retry this input/i }));
    await screen.findByText('ran {"nums":[2,7,11,15],"target":5} step 1');
    expect(screen.queryByRole('alert', { name: /new run failed/i })).not.toBeInTheDocument();
  });
});

describe('App session: restoring a shared link', () => {
  it('runs the shared input before restoring its step', async () => {
    const input = encodeInput({ nums: [1, 2], target: 3 });
    renderApp(`/problem/two-sum?input=${input}&step=5`);
    await screen.findByText('ran {"nums":[1,2],"target":3} step 5');
    expect(sharedParams().get('step')).toBe('5');
    expect(sharedParams().get('input')).toBe(input);
    fireEvent.click(screen.getByRole('button', { name: 'Edit input' }));
    expect(screen.getByLabelText('Target sum')).toHaveValue(3);
  });

  it('explains a rejected shared input instead of pretending it restored', async () => {
    renderApp(`/problem/two-sum?input=${encodeInput({ nums: [1, 2], target: 999 })}&step=2`);
    expect(await screen.findByRole('status', { name: /shared link/i }))
      .toHaveTextContent(/input in this link could not be run.*step 2 belonged to that run/i);
    expect(screen.getByText(DEFAULT_STEP)).toBeInTheDocument();
    await waitFor(() => expect(sharedParams().get('input')).toBeNull());
  });

  it('explains a step beyond the end of the run and starts at step 1', async () => {
    renderApp('/problem/two-sum?step=40');
    expect(await screen.findByRole('status', { name: /shared link/i }))
      .toHaveTextContent(/step 40.*3 steps/i);
    expect(screen.getByText(DEFAULT_STEP)).toBeInTheDocument();
    await waitFor(() => expect(sharedParams().get('step')).toBeNull());
  });

  it('keeps an unrelated view parameter while the step moves', async () => {
    renderApp('/problem/two-sum?view=code');
    await screen.findByText(DEFAULT_STEP);
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(sharedParams().get('step')).toBe('2'));
    expect(sharedParams().get('view')).toBe('code');
  });
});

describe('App session: progress', () => {
  it('does not mark a truncated run as watched', async () => {
    executeOverride = null;
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/problems') return Promise.resolve(ok([TWO_SUM]));
      if (url === '/api/problems/two-sum') return Promise.resolve(ok(TWO_SUM));
      return Promise.resolve(ok({ ...trace({ nums: [1], target: 1 }, 2), truncated: true }));
    }));
    renderApp();
    await screen.findByText(/step 1$/);
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    await screen.findByText(/step 2$/);
    const stored = JSON.parse(window.localStorage.getItem('dsa-ui:progress') || '{}');
    expect(stored['two-sum']?.watched).not.toBe(true);
  });
});
