import React, { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom';
import ApproachSelector from './ApproachSelector';
import InputPanel from '../components/InputPanel';
import { defaultInput } from '../input/randomizeInput';

// Component contract only; no fabricated LCS algorithm/API result is served here.
const approaches = [['recursion', 4, 'abcd', 'acd'], ['memoization', 8, 'abcde', 'ace'],
  ['canonical', 12, 'abcde', 'ace']].map(([id, maxLength, first, second]) => ({
  id, label: id === 'canonical' ? 'Tabulation' : id[0].toUpperCase() + id.slice(1), summary: 'Unit approach.',
  inputSpec: { fields: [
    { name: 'first', label: 'First string', type: 'STRING', defaultValue: first, constraints: { minLength: 1, maxLength } },
    { name: 'second', label: 'Second string', type: 'STRING', defaultValue: second, constraints: { minLength: 1, maxLength } }
  ] }
}));
function Setup({ onRun = vi.fn(), onEditInput = vi.fn() }) {
  const [selected, selectApproach] = useState('canonical');
  const [values, replace] = useState(defaultInput(approaches[2].inputSpec));
  const selectedApproach = approaches.find(option => option.id === selected);
  return <>
    <ApproachSelector session={{ approaches, selectedApproach, shownApproach: approaches[2], selectApproach,
      run: { id: 1, approachId: 'canonical', approachLabel: 'Tabulation' }, draft: { values, replace } }}
      onRun={onRun} onEditInput={onEditInput} />
    <InputPanel problemId="string-draft-contract" inputSpec={selectedApproach.inputSpec}
      values={values} onChange={replace} onRun={onRun} />
  </>;
}
const choose = id => fireEvent.change(screen.getByRole('combobox', { name: 'Solution approach' }), { target: { value: id } });

describe('two-string approach editor guidance', () => {
  it('keeps a longer controlled value while native maxlength adopts the selected limit', () => {
    const onRun = vi.fn();
    render(<Setup onRun={onRun} />);
    choose('recursion');
    expect(screen.getByLabelText('First string')).toHaveValue('abcde');
    expect(screen.getByLabelText('Second string')).toHaveValue('ace');
    expect(screen.getByLabelText('First string')).toHaveAttribute('maxlength', '4');
    expect(screen.getByLabelText('Second string')).toHaveAttribute('maxlength', '4');
    expect(screen.getByRole('status', { name: 'Input limits' })).toHaveTextContent('First string has 5 characters; maximum 4.');
    expect(screen.getByRole('button', { name: 'Run recursion' })).toHaveAccessibleDescription(/maximum 4/);
    choose('memoization');
    expect(screen.getByLabelText('First string')).toHaveValue('abcde');
    expect(screen.getByLabelText('First string')).toHaveAttribute('maxlength', '8');
    expect(screen.queryByRole('status', { name: 'Input limits' })).not.toBeInTheDocument();
    expect(onRun).not.toHaveBeenCalled();
  });

  it('reports both preserved long strings and submits their exact values only on explicit Run', () => {
    const onRun = vi.fn();
    render(<Setup onRun={onRun} />);
    fireEvent.change(screen.getByLabelText('First string'), { target: { value: 'abcdefghijkl' } });
    fireEvent.change(screen.getByLabelText('Second string'), { target: { value: 'abcdefghi' } });
    choose('memoization');
    const warning = screen.getByRole('status', { name: 'Input limits' });
    expect(warning).toHaveTextContent('First string has 12 characters; maximum 8.');
    expect(warning).toHaveTextContent('Second string has 9 characters; maximum 8.');
    choose('recursion');
    expect(screen.getByLabelText('First string')).toHaveValue('abcdefghijkl');
    expect(screen.getByLabelText('Second string')).toHaveValue('abcdefghi');
    expect(onRun).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Run recursion' }));
    expect(onRun).toHaveBeenCalledWith({ first: 'abcdefghijkl', second: 'abcdefghi' });
  });

  it('replaces both fields with the explicit recursive example without changing the displayed run', () => {
    const onRun = vi.fn(), onEditInput = vi.fn();
    render(<Setup onRun={onRun} onEditInput={onEditInput} />);
    choose('recursion');
    fireEvent.click(screen.getByRole('button', { name: 'Replace input with recursion example' }));
    expect(screen.getByLabelText('First string')).toHaveValue('abcd');
    expect(screen.getByLabelText('Second string')).toHaveValue('acd');
    expect(screen.queryByRole('status', { name: 'Input limits' })).not.toBeInTheDocument();
    expect(screen.getByRole('status', { name: 'Solution approach' })).toHaveTextContent('showing: Tabulation');
    expect(onEditInput).toHaveBeenCalledOnce();
    expect(onRun).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Run input' }));
    expect(onRun).toHaveBeenCalledWith({ first: 'abcd', second: 'acd' });
  });

  it('lets Restore defaults prepare the fitting selected approach example without executing', () => {
    const onRun = vi.fn();
    render(<Setup onRun={onRun} />);
    fireEvent.change(screen.getByLabelText('First string'), { target: { value: 'xy' } });
    fireEvent.change(screen.getByLabelText('Second string'), { target: { value: 'zz' } });
    choose('recursion');
    expect(screen.queryByRole('button', { name: 'Replace input with recursion example' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Restore defaults' }));
    expect(screen.getByLabelText('First string')).toHaveValue('abcd');
    expect(screen.getByLabelText('Second string')).toHaveValue('acd');
    expect(onRun).not.toHaveBeenCalled();
  });
});
