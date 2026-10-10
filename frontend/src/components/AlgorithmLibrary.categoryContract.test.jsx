import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import contract from '../../../contracts/categories.json';
import AlgorithmLibrary from './AlgorithmLibrary';
import { CatalogProvider } from '../catalog/CatalogProvider';

// The old Sidebar contract compared a retired hardcoded grid to the shared contract.
// Exercise the actual library filters now, including exact backend category strings.
const catalog = contract.map(({ category }, index) => ({
  id: `category-example-${index}`, title: `Example for ${category}`, category,
  difficulty: 'Easy', dsType: 'Array', traced: true
}));
beforeEach(() => vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => catalog })));
afterEach(() => vi.unstubAllGlobals());
function mount() {
  render(<MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <CatalogProvider><AlgorithmLibrary /></CatalogProvider>
  </MemoryRouter>);
}

describe('live library category contract', () => {
  it('offers every backend category with a readable label, without aliases or duplicates', async () => {
    mount();
    await screen.findByRole('link', { name: /Example for Arrays/ });
    const options = within(screen.getByRole('combobox', { name: 'Category' }))
      .getAllByRole('option').filter(option => option.value);
    expect(options.map(option => option.value).sort()).toEqual(contract.map(({ category }) => category).sort());
    for (const option of options) expect(option.textContent.trim().length).toBeGreaterThan(0);
  });

  it.each(catalog)('keeps $category browsable through its real filter', async problem => {
    mount();
    await screen.findByRole('link', { name: /Example for Arrays/ });
    fireEvent.change(screen.getByRole('combobox', { name: 'Category' }), { target: { value: problem.category } });
    const library = screen.getByRole('region', { name: 'Algorithm library' });
    await waitFor(() => expect(within(library).getAllByRole('link')).toHaveLength(1));
    expect(within(library).getByRole('link')).toHaveAttribute('href', `/problem/${problem.id}`);
    expect(within(library).getByRole('link')).toHaveTextContent(problem.title);
    expect(fetch).toHaveBeenCalledTimes(1);
  });
});
