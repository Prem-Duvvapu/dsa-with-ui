import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import '@testing-library/jest-dom';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';

function problem(id, title) {
  return { id, title, category: 'Arrays', dsType: 'Array', traced: true };
}

const CATALOG = [problem('two-sum', 'Two Sum'), problem('kadane-algo', 'Kadane Algo')];

function renderDashboard() {
  return render(
    <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Dashboard />
    </MemoryRouter>
  );
}

describe('Dashboard', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({
      ok: true,
      json: () => Promise.resolve(CATALOG)
    })));
  });

  it('greets a first-time visitor with a way to start, not a continue card', async () => {
    renderDashboard();
    await waitFor(() => expect(screen.getByRole('button', { name: /start/i })).toBeInTheDocument());

    expect(screen.queryByText(/continue/i)).not.toBeInTheDocument();
  });

  it('offers a returning visitor a continue card naming their last problem', async () => {
    window.localStorage.setItem('dsa-ui:lastVisitedProblem', JSON.stringify('kadane-algo'));
    renderDashboard();

    await waitFor(() => expect(screen.getByText('Kadane Algo')).toBeInTheDocument());
    expect(screen.getByRole('button', { name: /continue/i })).toBeInTheDocument();
  });

  it('reports how many problems have been watched so far', async () => {
    window.localStorage.setItem('dsa-ui:progress', JSON.stringify({ 'two-sum': { watched: true } }));
    renderDashboard();

    await waitFor(() => expect(screen.getByText(/1 of 2/)).toBeInTheDocument());
  });

  it('always offers a way into the full problem browser', async () => {
    renderDashboard();
    await waitFor(() => expect(screen.getByRole('button', { name: /browse/i })).toBeInTheDocument());
  });

  it('shows an error with a retry when the catalogue cannot be reached', async () => {
    fetch.mockImplementationOnce(() => Promise.reject(new Error('network down')));
    renderDashboard();

    await waitFor(() => expect(screen.getByText(/could not reach/i)).toBeInTheDocument());
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });
});
