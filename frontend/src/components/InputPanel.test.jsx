import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import InputPanel from './InputPanel';

const spec = {
  fields: [
    { name: 'nums', type: 'INT_ARRAY', label: 'Array', defaultValue: [5, 4, 3, 2, 1] }
  ]
};

describe('InputPanel', () => {
  beforeEach(() => window.localStorage.clear());

  it('offers the tracer\'s other case, and runs it', () => {
    // Every tracer declares a second, materially different input. It existed only for the
    // contract tests, so the branches only it reaches - next-permutation's swap and suffix
    // reverse, word-break's memo hit, every not-found path - were greyed out in the code
    // panel with no way for a reader to go and take them.
    const onRun = vi.fn();
    render(
      <InputPanel
        problemId="next-permutation"
        inputSpec={spec}
        alternateInput={{ nums: [2, 3, 1] }}
        onRun={onRun}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /other case/i }));

    expect(onRun).toHaveBeenCalledWith({ nums: [2, 3, 1] });
  });

  it('hides the other-case button when the problem has no alternate', () => {
    render(<InputPanel problemId="x" inputSpec={spec} onRun={() => {}} />);
    expect(screen.queryByRole('button', { name: /other case/i })).not.toBeInTheDocument();
  });

  describe('saved inputs', () => {
    it('saves the current form values under a name, and lists it', () => {
      render(<InputPanel problemId="two-sum" inputSpec={spec} onRun={() => {}} />);

      fireEvent.click(screen.getByRole('button', { name: /^save/i }));
      fireEvent.change(screen.getByLabelText(/preset name/i), { target: { value: 'Tricky one' } });
      fireEvent.click(screen.getByRole('button', { name: /confirm save/i }));

      expect(screen.getByRole('button', { name: /load tricky one/i })).toBeInTheDocument();
    });

    it('loads a saved input and runs it in one click', () => {
      // Presets exist to bring someone back to a case they already built, so this follows
      // the "Other case" precedent rather than the plain field editor's: loading without
      // running would put the friction back that saving was supposed to remove.
      const onRun = vi.fn();
      const { rerender } = render(<InputPanel problemId="two-sum" inputSpec={spec} onRun={() => {}} />);
      fireEvent.click(screen.getByRole('button', { name: /^save/i }));
      fireEvent.change(screen.getByLabelText(/preset name/i), { target: { value: 'Mine' } });
      fireEvent.click(screen.getByRole('button', { name: /confirm save/i }));

      rerender(<InputPanel problemId="two-sum" inputSpec={spec} onRun={onRun} />);
      fireEvent.click(screen.getByRole('button', { name: /load mine/i }));

      expect(onRun).toHaveBeenCalledWith({ nums: [5, 4, 3, 2, 1] });
    });

    it('removes a saved input without disturbing the others', () => {
      render(<InputPanel problemId="two-sum" inputSpec={spec} onRun={() => {}} />);
      for (const name of ['a', 'b']) {
        fireEvent.click(screen.getByRole('button', { name: /^save/i }));
        fireEvent.change(screen.getByLabelText(/preset name/i), { target: { value: name } });
        fireEvent.click(screen.getByRole('button', { name: /confirm save/i }));
      }

      fireEvent.click(screen.getByRole('button', { name: /remove preset a/i }));

      expect(screen.queryByRole('button', { name: /load a/i })).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: /load b/i })).toBeInTheDocument();
    });

    it('does not save a blank name', () => {
      render(<InputPanel problemId="two-sum" inputSpec={spec} onRun={() => {}} />);
      fireEvent.click(screen.getByRole('button', { name: /^save/i }));
      fireEvent.click(screen.getByRole('button', { name: /confirm save/i }));
      expect(screen.queryByText(/^load /i)).not.toBeInTheDocument();
    });

    it('keeps saved inputs scoped to their own problem', () => {
      const { rerender } = render(<InputPanel problemId="two-sum" inputSpec={spec} onRun={() => {}} />);
      fireEvent.click(screen.getByRole('button', { name: /^save/i }));
      fireEvent.change(screen.getByLabelText(/preset name/i), { target: { value: 'two-sum only' } });
      fireEvent.click(screen.getByRole('button', { name: /confirm save/i }));

      rerender(<InputPanel problemId="kadane-algo" inputSpec={spec} onRun={() => {}} />);
      expect(screen.queryByRole('button', { name: /load two-sum only/i })).not.toBeInTheDocument();
    });
  });
});
