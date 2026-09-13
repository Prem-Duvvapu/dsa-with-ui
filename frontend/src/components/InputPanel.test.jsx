import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi } from 'vitest';
import InputPanel from './InputPanel';

const spec = {
  fields: [
    { name: 'nums', type: 'INT_ARRAY', label: 'Array', defaultValue: [5, 4, 3, 2, 1] }
  ]
};

describe('InputPanel', () => {
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
});
