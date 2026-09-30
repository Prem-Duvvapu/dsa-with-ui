import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import '@testing-library/jest-dom';
import AlgorithmLibrary from './AlgorithmLibrary';
import { CatalogProvider, useCatalog } from '../catalog/CatalogProvider';

const catalog = Array.from({ length: 63 }, (_, index) => ({ id: `problem-${index}`, title: `Algorithm ${String(index).padStart(2, '0')}`, category: index % 2 ? 'Graphs' : 'Arrays', difficulty: index % 2 ? 'Hard' : 'Easy', dsType: 'Array', traced: index !== 62 }));
function Destination() { const { problems } = useCatalog(); return <p>Selected from {problems.length}</p>; }
function Location() { const location = useLocation(); return <output data-testid="location">{location.pathname}{location.search}</output>; }
function mount(path = '/') {
  return render(<MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}><CatalogProvider><Routes><Route path="/" element={<AlgorithmLibrary />} /><Route path="/problem/:id" element={<Destination />} /></Routes><Location /></CatalogProvider></MemoryRouter>);
}
beforeEach(() => { localStorage.clear(); sessionStorage.clear(); vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => catalog })); });
afterEach(() => vi.unstubAllGlobals());

describe('Algorithm library', () => {
  it('makes results beyond the old 50-result cap reachable and keeps the batch in the URL', async () => {
    mount();
    const library = screen.getByRole('region', { name: 'Algorithm library' });
    await waitFor(() => expect(within(library).getAllByRole('link')).toHaveLength(50));
    fireEvent.click(screen.getByRole('button', { name: /Load more algorithms/ }));
    expect(within(library).getAllByRole('link')).toHaveLength(63);
    expect(screen.getByTestId('location')).toHaveTextContent('limit=100');
    expect(screen.getByRole('link', { name: /Algorithm 62/ })).toHaveAttribute('href', '/problem/problem-62');
  });
  it('combines URL filters and lets the learner clear a zero-match search', async () => {
    mount('/?category=Graphs&difficulty=Hard&runnable=true');
    await screen.findByRole('link', { name: /Algorithm 01/ });
    const library = screen.getByRole('region', { name: 'Algorithm library' });
    expect(within(library).getAllByRole('link')).toHaveLength(31);
    fireEvent.change(screen.getByRole('textbox', { name: 'Search algorithms' }), { target: { value: 'no-such-problem' } });
    expect(screen.getByText('No algorithms match these filters.')).toBeInTheDocument();
    // The empty state names its scope: it clears the search AND the filters.
    fireEvent.click(screen.getByRole('button', { name: 'Clear search and filters' }));
    expect(within(library).getAllByRole('link')).toHaveLength(50);
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/);
  });
  it('shares one catalogue across route changes and never executes a problem from the library', async () => {
    mount();
    fireEvent.click(await screen.findByRole('link', { name: /Algorithm 00/ }));
    expect(screen.getByText('Selected from 63')).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch.mock.calls[0][0]).toBe('/api/problems');
  });
  it('preserves star filtering and recent queries on result selection', async () => {
    mount();
    fireEvent.click(await screen.findByRole('button', { name: 'Star Algorithm 00' }));
    fireEvent.change(screen.getByRole('combobox', { name: 'Progress' }), { target: { value: 'starred' } });
    expect(within(screen.getByRole('region', { name: 'Algorithm library' })).getAllByRole('link')).toHaveLength(1);
    fireEvent.change(screen.getByRole('textbox', { name: 'Search algorithms' }), { target: { value: 'Algorithm 00' } });
    fireEvent.click(screen.getByRole('link', { name: /Algorithm 00/ }));
    expect(JSON.parse(localStorage.getItem('dsa:recentSearches'))).toContain('Algorithm 00');
  });
  it('presents an empty catalogue honestly', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
    mount();
    expect(await screen.findByText('The catalogue is empty.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Start with/ })).not.toBeInTheDocument();
  });

  it('clears filters without discarding the search, and each chip removes only its own filter', async () => {
    mount('/?q=Algorithm&category=Graphs&difficulty=Hard');
    await screen.findByRole('link', { name: /Algorithm 01/ });
    const chips = screen.getByRole('list', { name: 'Active filters' });
    expect(within(chips).getAllByRole('button')).toHaveLength(2);

    fireEvent.click(screen.getByRole('button', { name: 'Remove filter Difficulty: Hard' }));
    expect(screen.getByTestId('location').textContent).toMatch(/category=Graphs/);
    expect(screen.getByTestId('location').textContent).not.toMatch(/difficulty/);

    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }));
    expect(screen.getByTestId('location')).toHaveTextContent('/?q=Algorithm');
    expect(screen.queryByRole('list', { name: 'Active filters' })).not.toBeInTheDocument();
  });

  it('folds the same filter controls into a Filters (N) disclosure', async () => {
    mount('/?category=Graphs');
    await screen.findByRole('link', { name: /Algorithm 01/ });
    const toggle = screen.getByRole('button', { name: /Filters \(1\)/ });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    const panel = document.getElementById(toggle.getAttribute('aria-controls'));
    expect(within(panel).getByRole('combobox', { name: 'Category' })).toHaveValue('Graphs');
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(panel).toHaveAttribute('data-open', 'true');
  });

  it('normalises invalid URL values to "no filter" instead of an empty list', async () => {
    mount('/?category=Nope&difficulty=Impossible&status=weird&limit=-4');
    await screen.findByRole('link', { name: /Algorithm 00/ });
    expect(within(screen.getByRole('region', { name: 'Algorithm library' })).getAllByRole('link')).toHaveLength(50);
    expect(screen.queryByRole('list', { name: 'Active filters' })).not.toBeInTheDocument();
  });

  it('announces loaded rows and keeps focus somewhere sensible after the last batch', async () => {
    mount();
    await screen.findByRole('link', { name: /Algorithm 00/ });
    expect(screen.getByText('Showing 50 of 63')).toHaveAttribute('role', 'status');
    const more = screen.getByRole('button', { name: /Load more algorithms/ });
    more.focus();
    fireEvent.click(more);
    expect(screen.getByText('Showing 63 of 63')).toBeInTheDocument();
    // The button is gone once everything is loaded; focus lands on the first new row.
    expect(document.activeElement).toBe(screen.getByRole('link', { name: /Algorithm 50/ }));
  });

  it('offers Continue beside the progress summary, outside the closed disclosure', async () => {
    localStorage.setItem('dsa-ui:lastVisitedProblem', JSON.stringify('problem-7'));
    mount();
    const link = await screen.findByRole('link', { name: /Continue: Algorithm 07/ });
    expect(link).toHaveAttribute('href', '/problem/problem-7');
    expect(link.closest('details')).toBeNull();
  });

  it('restores the scroll position saved for this history entry once the rows exist', async () => {
    sessionStorage.setItem('dsa:library-scroll:default', '900');
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
    const raf = vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb) => { cb(); return 0; });
    mount();
    await screen.findByRole('link', { name: /Algorithm 00/ });
    expect(scrollTo).toHaveBeenCalledWith(0, 900);
    scrollTo.mockRestore();
    raf.mockRestore();
  });

  it('does not restore any scroll position on a history entry that saved none', async () => {
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
    mount();
    await screen.findByRole('link', { name: /Algorithm 00/ });
    expect(scrollTo).not.toHaveBeenCalled();
    scrollTo.mockRestore();
  });

  it('keeps working when storage is denied', async () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('denied'); });
    const getItem = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('denied'); });
    mount();
    fireEvent.click(await screen.findByRole('button', { name: 'Star Algorithm 00' }));
    expect(screen.getByRole('button', { name: 'Unstar Algorithm 00' })).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(screen.getByRole('link', { name: /Algorithm 00/ }));
    expect(screen.getByText('Selected from 63')).toBeInTheDocument();
    setItem.mockRestore();
    getItem.mockRestore();
  });

  it('labels an offline sample as a sample and retries the catalogue itself', async () => {
    fetch.mockRejectedValueOnce(new Error('offline'));
    mount();
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/small offline sample/i);
    expect(alert).toHaveTextContent(/browsing only/i);
    fireEvent.click(screen.getByRole('button', { name: 'Retry loading the catalogue' }));
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(fetch.mock.calls.every(([url]) => url === '/api/problems')).toBe(true);
  });
});

describe('Catalogue provider lifecycle', () => {
  function Probe() {
    const { problems, error, source, loading, retry } = useCatalog();
    return <div><output data-testid="state">{loading ? 'loading' : `${source}:${problems.length}:${error ? 'error' : 'ok'}`}</output><button onClick={retry}>retry</button></div>;
  }
  const mountProbe = () => render(<CatalogProvider><Probe /></CatalogProvider>);

  it('keeps a live catalogue, labelled as such, when a later refresh fails', async () => {
    mountProbe();
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('live:63:ok'));
    fetch.mockRejectedValueOnce(new Error('down'));
    fireEvent.click(screen.getByText('retry'));
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('live:63:error'));
  });

  it('treats a non-array body as a failure rather than an empty catalogue', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => ({ problems: [] }) });
    mountProbe();
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent(/sample:\d+:error/));
  });

  it('aborts the request when the provider unmounts', async () => {
    let signal;
    fetch.mockImplementationOnce((url, opts) => { signal = opts.signal; return new Promise(() => {}); });
    const { unmount } = mountProbe();
    await waitFor(() => expect(signal).toBeDefined());
    unmount();
    expect(signal.aborted).toBe(true);
  });
});

describe('Algorithm library responsive rules', () => {
  // jsdom applies no CSS, so the disclosure test above proves the wiring but not that a
  // phone actually hides the controls. These read the stylesheet itself: removing the rule
  // that hides a closed disclosure, or shrinking a touch target, fails here.
  const css = readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'AlgorithmLibrary.module.css'), 'utf8');
  const phone = css.slice(css.indexOf('@media (max-width: 640px)'));

  it('hides a closed filter disclosure on phones, and only there', () => {
    expect(phone).toMatch(/\.filters\[data-open="false"\]\s*\{\s*display:\s*none;/);
    expect(css.slice(0, css.indexOf('@media'))).not.toMatch(/\.filters\[data-open="false"\]/);
    expect(phone).toMatch(/\.filtersToggle\s*\{\s*display:\s*inline-flex;/);
  });

  it('gives chips and recent searches the 44px touch target', () => {
    for (const selector of ['.chips button', '.recents button']) {
      const rule = css.slice(css.indexOf(`${selector} {`), css.indexOf('}', css.indexOf(`${selector} {`)));
      expect(rule, selector).toMatch(/min-height:\s*var\(--target-min\)/);
    }
  });
});
