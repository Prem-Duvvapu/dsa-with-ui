import React from 'react';
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import '@testing-library/jest-dom';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';

function problem(id, title) {
  return { id, title, category: 'Arrays', dsType: 'Array', traced: true };
}

const CATALOG = [problem('two-sum', 'Two Sum'), problem('kadane-algo', 'Kadane Algo')];
const CATALOG_TITLES = CATALOG.map(p => p.title);

async function renderDashboard() {
  const result = render(
    <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Dashboard />
    </MemoryRouter>
  );
  // Returning-learning actions now live in a disclosure so library search stays prominent.
  const summary = await screen.findByText('Your learning');
  fireEvent.click(summary);
  return result;
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
    await renderDashboard();
    await waitFor(() => expect(screen.getByRole('button', { name: /start/i })).toBeInTheDocument());

    expect(screen.queryByRole('button', { name: /^continue/i })).not.toBeInTheDocument();
  });

  it('offers a returning visitor a continue card naming their last problem', async () => {
    window.localStorage.setItem('dsa-ui:lastVisitedProblem', JSON.stringify('kadane-algo'));
    await renderDashboard();

    await waitFor(() => expect(screen.getByText(/continue where you left off/i)).toBeInTheDocument());
    const continueBlock = screen.getByText(/continue where you left off/i).closest('div');
    expect(within(continueBlock).getByText('Kadane Algo')).toBeInTheDocument();
    expect(within(continueBlock).getByRole('button', { name: /continue/i })).toBeInTheDocument();
  });

  it('reports how many problems have been watched so far', async () => {
    window.localStorage.setItem('dsa-ui:progress', JSON.stringify({ 'two-sum': { watched: true } }));
    await renderDashboard();

    await waitFor(() => expect(screen.getByText(/1 of 2/)).toBeInTheDocument());
  });

  it('always offers a way into the full problem browser', async () => {
    await renderDashboard();
    await waitFor(() => expect(screen.getByRole('button', { name: /browse/i })).toBeInTheDocument());
  });

  it('shows an error with a retry when the catalogue cannot be reached', async () => {
    fetch.mockImplementationOnce(() => Promise.reject(new Error('network down')));
    await renderDashboard();

    await waitFor(() => expect(screen.getByText(/could not reach/i)).toBeInTheDocument());
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  describe('today\'s problem', () => {
    it('picks one catalogue problem for today and offers a way to try it', async () => {
      await renderDashboard();
      await waitFor(() => expect(screen.getByRole('button', { name: /today/i })).toBeInTheDocument());

      const section = screen.getByRole('button', { name: /today/i }).closest('section, div');
      expect(CATALOG_TITLES.some(title => section.textContent.includes(title))).toBe(true);
    });

    it('picks the same problem across a reload, for the same day', async () => {
      const { unmount } = await renderDashboard();
      await waitFor(() => expect(screen.getByRole('button', { name: /today/i })).toBeInTheDocument());
      const first = screen.getByRole('button', { name: /today/i }).closest('section, div').textContent;
      unmount();

      await renderDashboard();
      await waitFor(() => expect(screen.getByRole('button', { name: /today/i })).toBeInTheDocument());
      const second = screen.getByRole('button', { name: /today/i }).closest('section, div').textContent;

      expect(second).toBe(first);
    });
  });

  describe('streak', () => {
    it('has nothing to show with no streak recorded yet', async () => {
      await renderDashboard();
      await waitFor(() => expect(screen.getByRole('button', { name: /start/i })).toBeInTheDocument());
      expect(screen.queryByText(/day streak/i)).not.toBeInTheDocument();
    });

    it('reports the current streak once one has been recorded', async () => {
      window.localStorage.setItem('dsa-ui:streak', JSON.stringify({ lastDate: '2026-09-14', current: 3, longest: 5 }));
      await renderDashboard();
      await waitFor(() => expect(screen.getByText(/3.*day streak/i)).toBeInTheDocument());
    });
  });

  describe('review queue', () => {
    it('is absent when nothing is starred', async () => {
      await renderDashboard();
      await waitFor(() => expect(screen.getByRole('button', { name: /start/i })).toBeInTheDocument());
      expect(screen.queryByText(/review/i)).not.toBeInTheDocument();
    });

    it('offers a way back into a starred problem', async () => {
      window.localStorage.setItem('dsa-ui:progress', JSON.stringify({ 'kadane-algo': { starred: true } }));
      await renderDashboard();

      await waitFor(() => expect(screen.getByText(/review queue/i)).toBeInTheDocument());
      expect(screen.getByRole('button', { name: /review/i })).toBeInTheDocument();
    });
  });
});
