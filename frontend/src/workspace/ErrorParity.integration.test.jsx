import React from 'react';
import { render, screen, fireEvent, waitFor, within, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import ProblemWorkspace from './ProblemWorkspace';

/**
 * Error parity across views and the not-found page (INDEPENDENT_REVIEW_DB8683B.md S7, S8):
 * why a run is unavailable must not depend on which view is open, and every control the
 * not-found page offers must work.
 */

let location;
function Probe() { location = useLocation(); return null; }
const renderAt = (path) => render(
  <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <Routes><Route path="/problem/:id" element={<ProblemWorkspace />} /></Routes>
    <Probe />
  </MemoryRouter>
);
const ok = (body) => ({ ok: true, status: 200, json: () => Promise.resolve(body) });
const ALPHA = {
  id: 'alpha', title: 'Alpha', category: 'Test', difficulty: 'Easy', dsType: 'Array', traced: true, javaCode: 'void a() {}',
  inputSpec: { fields: [{ name: 'n', label: 'Count', type: 'INT', defaultValue: 1, constraints: { min: 0, max: 50 } }] }
};
const BETA = { ...ALPHA, id: 'beta', title: 'Beta' };
const trace = (n) => ({ encoding: 'delta', resolvedInput: { n }, anchors: {}, steps: [
  { stepNumber: 1, activeLine: 1, keyframe: true, dsType: 'Array', variables: {}, description: `n=${n} step 1`, arrayState: [{ index: 0, value: 1, state: 'default' }] }
] });

let executeFails;
let held;
beforeEach(() => {
  executeFails = false;
  held = null;
  Element.prototype.scrollIntoView = vi.fn();
  vi.stubGlobal('fetch', vi.fn((url, opts = {}) => {
    if (url === '/api/problems') return Promise.resolve(ok([ALPHA, BETA]));
    const detail = url.match(/^\/api\/problems\/([^/]+)$/);
    if (detail) return Promise.resolve([ALPHA, BETA].some((p) => p.id === detail[1]) ? ok(detail[1] === 'beta' ? BETA : ALPHA) : { ok: false, status: 404, json: () => Promise.resolve(null) });
    if (!url.startsWith('/api/problems/alpha') && !url.startsWith('/api/problems/beta')) return Promise.resolve({ ok: false, status: 404, json: () => Promise.resolve(null) });
    if (opts.method === 'POST') {
      const { n } = JSON.parse(opts.body);
      const respond = () => (n > 50
        ? { ok: false, status: 400, json: () => Promise.resolve({ fieldErrors: { n: 'Must be at most 50.' } }) }
        : ok(trace(n)));
      return new Promise((resolve) => { held = () => resolve(respond()); });
    }
    if (executeFails) return Promise.resolve({ ok: false, status: 500, json: () => Promise.resolve(null) });
    return Promise.resolve(ok(trace(1)));
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe('A failed first load is explained in every view (S7)', () => {
  it.each(['playground', 'code', 'analysis'])('names the backend failure in the %s view', async (view) => {
    executeFails = true;
    renderAt(`/problem/alpha${view === 'playground' ? '' : `?view=${view}`}`);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Could not load this trace from the backend.');
    expect(screen.getAllByRole('alert')).toHaveLength(1);
  });
});

describe('A rejected run stays fixable after leaving the editor (S7)', () => {
  it('offers a route back to the field errors from another view', async () => {
    renderAt('/problem/alpha');
    await screen.findByText('n=1 step 1', { selector: 'p' });
    fireEvent.change(screen.getByLabelText('Count'), { target: { value: '99' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run input' }));
    await waitFor(() => expect(held).toBeTypeOf('function'));
    fireEvent.click(screen.getByRole('tab', { name: 'Analysis' }));
    await act(async () => { held(); await new Promise((r) => setTimeout(r, 20)); });

    const notice = await screen.findByRole('alert', { name: /input could not run/i });
    fireEvent.click(within(notice).getByRole('button', { name: 'Fix it in the editor' }));
    expect(screen.getByRole('tab', { name: 'Playground' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText('Count: Must be at most 50.')).toBeInTheDocument();
  });
});

describe('The not-found page keeps every control working (S8)', () => {
  it('opens Help', async () => {
    renderAt('/problem/nope');
    await screen.findByRole('heading', { name: /No algorithm called/ });
    fireEvent.click(screen.getAllByRole('button', { name: 'Help' })[0]);
    expect(screen.getByRole('dialog', { name: 'Keyboard shortcuts' })).toBeInTheDocument();
  });

  it('opens Switch problem and navigates from it', async () => {
    renderAt('/problem/nope');
    await screen.findByRole('heading', { name: /No algorithm called/ });
    fireEvent.click(screen.getAllByRole('button', { name: /Switch problem/ })[0]);
    const dialog = screen.getByRole('dialog', { name: /command palette/i });
    fireEvent.change(within(dialog).getByRole('textbox'), { target: { value: 'Beta' } });
    fireEvent.click(within(dialog).getByText('Beta'));
    await waitFor(() => expect(location.pathname).toBe('/problem/beta'));
  });
});
