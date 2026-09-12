import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import StepStateSummary, { describeStep } from './StepStateSummary';

describe('StepStateSummary', () => {
  it('says what the array holds and where the pointers are', () => {
    render(<StepStateSummary step={{
      arrayState: [
        { index: 0, value: 2, state: 'default' },
        { index: 1, value: 7, state: 'current' },
        { index: 2, value: 11, state: 'default' }
      ]
    }} dsType="Array" />);
    const text = screen.getByTestId('step-state-summary').textContent;
    expect(text).toContain('Array of 3: 2, 7, 11.');
    expect(text).toContain('current at index 1');
  });

  it('reads a grid row by row rather than as one flat run', () => {
    const text = describeStep({ gridState: [[1, 0], [0, 1]] }, 'Matrix');
    expect(text).toContain('2 rows by 2 columns');
    expect(text).toContain('Row 0: 1, 0');
    expect(text).toContain('Row 1: 0, 1');
  });

  it('names the end that matters for a stack versus a queue', () => {
    expect(describeStep({ queueOrStackState: ['a', 'b', 'c'] }, 'Stack')).toContain('Top is c.');
    expect(describeStep({ queueOrStackState: ['a', 'b', 'c'] }, 'Queue')).toContain('Front is a.');
  });

  it('falls back to variables when the state is entirely scalar', () => {
    // Bit tricks and maths problems have no structure to draw; the variables are the state.
    render(<StepStateSummary step={{ variables: { N: '12', result: '13' } }} dsType="Bits" />);
    expect(screen.getByTestId('step-state-summary').textContent)
      .toContain('Values: N is 12, result is 13.');
  });

  it('truncates a long array instead of reading out hundreds of numbers', () => {
    const big = Array.from({ length: 200 }, (_, i) => ({ index: i, value: i, state: 'default' }));
    expect(describeStep({ arrayState: big }, 'Array')).toContain('and 140 more');
  });

  it('renders nothing when the step carries no state at all', () => {
    const { container } = render(<StepStateSummary step={{}} dsType="Array" />);
    expect(container).toBeEmptyDOMElement();
  });

  it('stays out of the visual layout', () => {
    // sr-only, not a second live region: LiveTraceTicker is already polite, and two
    // announcers firing on the same tick talk over each other.
    render(<StepStateSummary step={{ variables: { n: '1' } }} dsType="Array" />);
    const el = screen.getByTestId('step-state-summary');
    expect(el.className).toBe('sr-only');
    expect(el.getAttribute('aria-live')).toBeNull();
  });
});
