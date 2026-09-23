import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
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
    fireEvent.click(screen.getAllByRole('button', { name: 'Clear filters' })[0]);
    expect(within(library).getAllByRole('link')).toHaveLength(50);
    expect(screen.getByTestId('location')).toHaveTextContent('/');
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
});
