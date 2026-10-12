import React, { useState } from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi } from 'vitest';
import ApproachSelector from './ApproachSelector';

const array = [10, 9, 2, 5, 3, 7, 101, 18];
const approaches = ['recursion', 'memoization', 'canonical'].map((id, index) => ({
  id, label: ['Recursion', 'Memoization', 'Tabulation'][index], summary: 'Actual declared approach.',
  inputSpec: { fields: [{ name: 'nums', label: 'Array', type: 'INT_ARRAY',
    constraints: { minLength: 1, maxLength: [6, 9, 16][index] },
    defaultValue: index === 0 ? array.slice(0, 6) : array }] }
}));

function Setup({ onRun = vi.fn(), onEditInput = vi.fn() }) {
  const [selected, select] = useState('canonical');
  const [values, replace] = useState({ nums: array });
  return <>
    <ApproachSelector session={{ approaches, selectedApproach: approaches.find(a => a.id === selected),
      shownApproach: approaches[2], selectApproach: select,
      run: { id: 1, approachId: 'canonical', approachLabel: 'Tabulation' },
      draft: { values, replace } }} onRun={onRun} onEditInput={onEditInput} />
    <output aria-label="Draft">{JSON.stringify(values)}</output>
  </>;
}

function choose(id) {
  fireEvent.change(screen.getByRole('combobox', { name: 'Solution approach' }), { target: { value: id } });
}

describe('approach-specific input guidance', () => {
  it('uses Ninja’s actual INT_GRID schema for preserved memoization-width limits', () => {
    const selectedApproach = { id: 'memoization', label: 'Memoization',
      inputSpec: { fields: [{ name: 'grid', label: 'Chocolate Grid', type: 'INT_GRID',
        constraints: { maxRows: 5, maxCols: 4, minValue: 0, maxValue: 100 },
        defaultValue: [[3, 1, 1], [2, 5, 1], [1, 5, 5], [2, 1, 1]] }] } };
    render(<ApproachSelector session={{ approaches: [selectedApproach, { id: 'canonical', label: 'Rolling tabulation' }],
      selectedApproach, selectApproach: vi.fn(), run: { id: 1, approachId: 'canonical' },
      draft: { values: { grid: [[1, 2, 3, 4, 5]] }, replace: vi.fn() } }} onRun={vi.fn()} onEditInput={vi.fn()} />);
    expect(screen.getByRole('status', { name: 'Input limits' }))
      .toHaveTextContent('Chocolate Grid has a row with 5 columns; maximum 4.');
  });

  it('explains the preserved eight-value LIS input before a rejected recursive run', () => {
    const onRun = vi.fn();
    render(<Setup onRun={onRun} />);
    choose('recursion');
    const warning = screen.getByRole('status', { name: 'Input limits' });
    expect(warning).toHaveTextContent('Recursion visualizer limits');
    expect(warning).toHaveTextContent('Array has 8 values; maximum 6.');
    expect(screen.getByRole('button', { name: 'Run recursion' })).toHaveAccessibleDescription(/maximum 6/);
    expect(screen.getByLabelText('Draft')).toHaveTextContent(JSON.stringify({ nums: array }));
    expect(onRun).not.toHaveBeenCalled();
    // Guidance does not impersonate the server boundary or silently repair the input.
    fireEvent.click(screen.getByRole('button', { name: 'Run recursion' }));
    expect(onRun).toHaveBeenCalledWith({ nums: array });
  });

  it('offers an explicit example replacement without running or changing the shown approach', () => {
    const onRun = vi.fn();
    const onEditInput = vi.fn();
    render(<Setup onRun={onRun} onEditInput={onEditInput} />);
    choose('recursion');
    fireEvent.click(screen.getByRole('button', { name: 'Replace input with recursion example' }));
    expect(screen.getByLabelText('Draft')).toHaveTextContent(JSON.stringify({ nums: array.slice(0, 6) }));
    expect(screen.queryByRole('status', { name: 'Input limits' })).not.toBeInTheDocument();
    expect(screen.getByRole('status', { name: 'Solution approach' })).toHaveTextContent('showing: Tabulation');
    expect(onEditInput).toHaveBeenCalledOnce();
    expect(onRun).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Run recursion' }));
    expect(onRun).toHaveBeenCalledWith({ nums: array.slice(0, 6) });
  });

  it('offers a named native editor action that preserves the entire draft', () => {
    const onEditInput = vi.fn();
    render(<Setup onEditInput={onEditInput} />);
    choose('recursion');
    fireEvent.click(screen.getByRole('button', { name: 'Adjust input' }));
    expect(onEditInput).toHaveBeenCalledOnce();
    expect(screen.getByLabelText('Draft')).toHaveTextContent(JSON.stringify({ nums: array }));
  });

  it.each(['loading', 'pending', 'restoring'])('disables both recovery actions during %s', state => {
    const onEditInput = vi.fn();
    const replace = vi.fn();
    const onRun = vi.fn();
    render(<ApproachSelector session={{ approaches, selectedApproach: approaches[0],
      selectApproach: vi.fn(), run: { id: 1, approachId: 'canonical' }, [state]: true,
      draft: { values: { nums: array }, replace } }} onRun={onRun} onEditInput={onEditInput} />);
    for (const name of ['Adjust input', 'Replace input with recursion example', 'Run recursion']) {
      const button = screen.getByRole('button', { name });
      expect(button).toBeDisabled();
      fireEvent.click(button);
    }
    expect(replace).not.toHaveBeenCalled();
    expect(onEditInput).not.toHaveBeenCalled();
    expect(onRun).not.toHaveBeenCalled();
  });

  it('rechecks the selected spec instead of carrying a recursion warning into memoization', () => {
    render(<Setup />);
    choose('recursion');
    expect(screen.getByRole('status', { name: 'Input limits' })).toBeInTheDocument();
    choose('memoization');
    expect(screen.queryByRole('status', { name: 'Input limits' })).not.toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Solution approach' })).toHaveValue('memoization');
    expect(screen.getByLabelText('Draft')).toHaveTextContent(JSON.stringify({ nums: array }));
  });
});
