import React from 'react';
import { render, screen, fireEvent, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import CommandPalette, { PALETTE_RESULT_LIMIT } from './CommandPalette';

const problems = Array.from({ length: 12 }, (_, i) => ({
  id: `sort-${i}`, title: `Sort Variant ${i}`, category: 'Sorting', traced: true
})).concat([{ id: 'two-sum', title: 'Two Sum', category: 'Arrays', traced: true }]);

function open(props = {}) {
  const handlers = { onClose: vi.fn(), onSelectProblem: vi.fn(), onCycleTheme: vi.fn(), onViewAll: vi.fn() };
  render(<CommandPalette isOpen problems={problems} {...handlers} {...props} />);
  return { ...handlers, input: screen.getByRole('combobox') };
}

beforeEach(() => { localStorage.clear(); Element.prototype.scrollIntoView = vi.fn(); });
afterEach(() => { delete Element.prototype.scrollIntoView; });

describe('CommandPalette (the switcher)', () => {
  it('shows a bounded list and carries the query to the library with View all', () => {
    const { input, onViewAll } = open();
    fireEvent.change(input, { target: { value: 'sort' } });
    const options = screen.getAllByRole('option');
    // 8 problems and the View all row - never the whole catalogue in a dialog.
    expect(options).toHaveLength(PALETTE_RESULT_LIMIT + 1);
    const viewAll = screen.getByRole('option', { name: 'View all 12 results in the library' });
    fireEvent.click(viewAll);
    expect(onViewAll).toHaveBeenCalledWith('sort');
  });

  it('moves the active option with the arrows, announces it and keeps it on screen', () => {
    const { input } = open();
    fireEvent.change(input, { target: { value: 'sort' } });
    const first = screen.getAllByRole('option')[0];
    expect(input).toHaveAttribute('aria-activedescendant', first.id);
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    const second = screen.getAllByRole('option')[1];
    expect(second).toHaveAttribute('aria-selected', 'true');
    expect(input).toHaveAttribute('aria-activedescendant', second.id);
    expect(Element.prototype.scrollIntoView).toHaveBeenLastCalledWith({ block: 'nearest' });
    fireEvent.keyDown(input, { key: 'ArrowUp' });
    fireEvent.keyDown(input, { key: 'ArrowUp' });
    // Wraps to the last option, View all.
    expect(screen.getByRole('option', { name: /View all/ })).toHaveAttribute('aria-selected', 'true');
  });

  it('marks the problem already open, and Enter on it still goes through onSelectProblem', () => {
    const { input, onSelectProblem } = open({ currentProblemId: 'two-sum' });
    fireEvent.change(input, { target: { value: 'two sum' } });
    const option = screen.getAllByRole('option')[0];
    expect(within(option).getByText('Current')).toBeInTheDocument();
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(onSelectProblem).toHaveBeenCalledWith('two-sum');
  });

  it('highlights the matched words, as the library does', () => {
    const { input } = open();
    fireEvent.change(input, { target: { value: 'two' } });
    expect(screen.getAllByRole('option')[0].querySelector('mark')).toHaveTextContent('Two');
  });

  it('offers recent searches on an empty query - the library\'s own list - and remembers a choice', () => {
    localStorage.setItem('dsa:recentSearches', JSON.stringify(['graph']));
    const { input, onSelectProblem } = open();
    const recent = screen.getByRole('option', { name: /graph/ });
    fireEvent.click(recent);
    expect(input).toHaveValue('graph');
    fireEvent.change(input, { target: { value: 'two sum' } });
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(onSelectProblem).toHaveBeenCalledWith('two-sum');
    expect(JSON.parse(localStorage.getItem('dsa:recentSearches'))[0]).toBe('two sum');
  });

  it('handles its own Escape, even while typing, and marks it handled', () => {
    const { input, onClose } = open();
    fireEvent.change(input, { target: { value: 'so' } });
    const notPrevented = fireEvent.keyDown(input, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(notPrevented).toBe(false);
  });

  it('says the catalogue is loading rather than that nothing matches', () => {
    const { input } = open({ problems: [], catalogLoading: true });
    fireEvent.change(input, { target: { value: 'two' } });
    expect(screen.getByRole('status')).toHaveTextContent('Loading the catalogue…');
  });

  it('names the query when nothing matches', () => {
    const { input } = open();
    fireEvent.change(input, { target: { value: 'zzzz' } });
    expect(screen.queryAllByRole('option')).toHaveLength(0);
    expect(screen.getByRole('status')).toHaveTextContent('No problem matches “zzzz”.');
  });
});
