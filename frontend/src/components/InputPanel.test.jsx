import React, { useState } from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import InputPanel from './InputPanel';
import { defaultInput } from '../input/randomizeInput';

const spec = {
  fields: [
    { name: 'nums', type: 'INT_ARRAY', label: 'Array', defaultValue: [5, 4, 3, 2, 1] }
  ]
};

/**
 * The editor is controlled: the problem session owns the draft so that it survives the
 * editor unmounting (closing the panel, switching views). This harness plays that role.
 */
function Controlled({ problemId = 'two-sum', inputSpec = spec, initial, ...props }) {
  const [values, setValues] = useState(() => initial ?? defaultInput(inputSpec));
  return (
    <InputPanel
      problemId={problemId}
      inputSpec={inputSpec}
      values={values}
      onChange={setValues}
      onRun={() => {}}
      {...props}
    />
  );
}

const chips = () => screen.getAllByLabelText(/Position \d+ value/).map((input) => Number(input.value));

describe('InputPanel', () => {
  beforeEach(() => window.localStorage.clear());

  it('offers the tracer\'s other case, and runs it', () => {
    // Every tracer declares a second, materially different input. It existed only for the
    // contract tests, so the branches only it reaches - next-permutation's swap and suffix
    // reverse, word-break's memo hit, every not-found path - were greyed out in the code
    // panel with no way for a reader to go and take them.
    const onRun = vi.fn();
    render(<Controlled problemId="next-permutation" alternateInput={{ nums: [2, 3, 1] }} onRun={onRun} />);

    fireEvent.click(screen.getByRole('button', { name: /other case/i }));

    expect(onRun).toHaveBeenCalledWith({ nums: [2, 3, 1] });
    // Load AND show: the fields now say what was run.
    expect(chips()).toEqual([2, 3, 1]);
  });

  it('hides the other-case button when the problem has no alternate', () => {
    render(<Controlled problemId="x" />);
    expect(screen.queryByRole('button', { name: /other case/i })).not.toBeInTheDocument();
  });

  it('shows the draft it is given rather than re-deriving defaults', () => {
    // A late or repeated render must never overwrite an edited draft with defaults.
    render(<Controlled initial={{ nums: [9, 8] }} />);
    expect(chips()).toEqual([9, 8]);
  });

  it('edits only the draft; running is a separate, explicit action', () => {
    const onRun = vi.fn();
    render(<Controlled onRun={onRun} />);
    fireEvent.change(screen.getByLabelText('Position 1 value'), { target: { value: '42' } });
    fireEvent.click(screen.getByRole('button', { name: 'Randomize input' }));
    fireEvent.click(screen.getByRole('button', { name: 'Reset input to default' }));
    expect(onRun).not.toHaveBeenCalled();
    expect(chips()).toEqual([5, 4, 3, 2, 1]);
  });

  it('points at the rejected fields and moves focus to the summary', () => {
    render(<Controlled fieldErrors={{ nums: 'Needs at least 2 values.' }} />);
    const summary = screen.getByRole('alert', { name: /could not run/i });
    expect(summary).toHaveTextContent('Array: Needs at least 2 values.');
    expect(document.activeElement).toBe(summary);
  });

  describe('saved inputs', () => {
    function save(name) {
      fireEvent.click(screen.getByRole('button', { name: /^save/i }));
      fireEvent.change(screen.getByLabelText(/preset name/i), { target: { value: name } });
      fireEvent.click(screen.getByRole('button', { name: /confirm save/i }));
    }

    it('saves the current form values under a name, and lists it', () => {
      render(<Controlled />);
      save('Tricky one');
      expect(screen.getByRole('button', { name: /load tricky one/i })).toBeInTheDocument();
    });

    it('loads a saved input and runs it in one click', () => {
      // Presets exist to bring someone back to a case they already built, so this follows
      // the "Other case" precedent rather than the plain field editor's: loading without
      // running would put the friction back that saving was supposed to remove.
      const onRun = vi.fn();
      render(<Controlled onRun={onRun} />);
      save('Mine');

      fireEvent.click(screen.getByRole('button', { name: /load mine/i }));

      expect(onRun).toHaveBeenCalledWith({ nums: [5, 4, 3, 2, 1] });
    });

    it('shows the loaded preset in the fields, not the values that were there before', () => {
      // The defect: Load ran the preset but left the old draft on screen, so the fields
      // described an input that was not the one being animated.
      const onRun = vi.fn();
      render(<Controlled onRun={onRun} initial={{ nums: [1, 1] }} />);
      save('Pair');
      fireEvent.change(screen.getByLabelText('Position 1 value'), { target: { value: '7' } });
      expect(chips()).toEqual([7, 1]);

      fireEvent.click(screen.getByRole('button', { name: /load pair/i }));

      expect(onRun).toHaveBeenCalledWith({ nums: [1, 1] });
      expect(chips()).toEqual([1, 1]);
    });

    it('removes a saved input without disturbing the others', () => {
      render(<Controlled />);
      save('a');
      save('b');

      fireEvent.click(screen.getByRole('button', { name: /remove preset a/i }));

      expect(screen.queryByRole('button', { name: /load a/i })).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: /load b/i })).toBeInTheDocument();
    });

    it('does not save a blank name', () => {
      render(<Controlled />);
      fireEvent.click(screen.getByRole('button', { name: /^save/i }));
      fireEvent.click(screen.getByRole('button', { name: /confirm save/i }));
      expect(screen.queryByText(/^load /i)).not.toBeInTheDocument();
    });

    it('keeps saved inputs scoped to their own problem', () => {
      const { rerender } = render(<Controlled problemId="two-sum" />);
      save('two-sum only');

      rerender(<Controlled problemId="kadane-algo" />);
      expect(screen.queryByRole('button', { name: /load two-sum only/i })).not.toBeInTheDocument();
    });
  });
});
