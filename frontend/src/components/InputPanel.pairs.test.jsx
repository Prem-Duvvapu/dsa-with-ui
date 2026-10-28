import React, { useState } from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi } from 'vitest';
import InputPanel from './InputPanel';

const inputSpec = {
  fields: [
    { name: 'weights', label: 'Weights', type: 'INT_ARRAY', defaultValue: [1, 2, 4, 5],
      constraints: { minLength: 1, maxLength: 8, minValue: 1, maxValue: 20 } },
    { name: 'values', label: 'Values', type: 'INT_ARRAY', defaultValue: [5, 4, 8, 6],
      constraints: { minLength: 1, maxLength: 8, minValue: 1, maxValue: 100 } },
    { name: 'capacity', label: 'Capacity', type: 'INT', defaultValue: 5,
      constraints: { min: 1, max: 30 } }
  ],
  equalLengthGroups: [['weights', 'values']]
};
const unequalDraft = { weights: [3, 3], values: [31, 41, 51], capacity: 1 };

function Controlled({ onRun }) {
  const [draft, setDraft] = useState(unequalDraft);
  return <InputPanel problemId="knapsack-01" inputSpec={inputSpec}
    values={draft} onChange={setDraft} onRun={onRun} />;
}

function arrayValues(label) {
  return within(screen.getByRole('group', { name: label }))
    .getAllByLabelText(/Position \d+ value/).map(input => Number(input.value));
}

describe('InputPanel paired-array drafts', () => {
  it('preserves and submits an unequal manual draft instead of silently normalizing it', () => {
    const onRun = vi.fn();
    render(<Controlled onRun={onRun} />);
    expect(arrayValues('Weights')).toEqual([3, 3]);
    expect(arrayValues('Values')).toEqual([31, 41, 51]);
    expect(screen.getByLabelText('Capacity')).toHaveValue(1);
    expect(onRun).not.toHaveBeenCalled();

    fireEvent.change(within(screen.getByRole('group', { name: 'Values' }))
      .getByLabelText('Position 1 value'), { target: { value: '45' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run input' }));
    expect(onRun).toHaveBeenCalledTimes(1);
    expect(onRun).toHaveBeenCalledWith({ weights: [3, 3], values: [45, 41, 51], capacity: 1 });
    expect(arrayValues('Weights')).toEqual([3, 3]);
    expect(arrayValues('Values')).toEqual([45, 41, 51]);
  });

  it('randomizes only the draft, then Run submits the exact visible paired values in index order', () => {
    const onRun = vi.fn();
    const originalUrl = window.location.href;
    window.history.replaceState({}, '', '/problem/knapsack-01?input=previous-run&step=3');
    const priorShareUrl = window.location.href;
    const replace = vi.spyOn(window.history, 'replaceState');
    const push = vi.spyOn(window.history, 'pushState');
    const draws = [0.125, 0.1, 0.1, 0.25, 0.3, 0.4, 0.5, 0];
    let index = 0;
    const random = vi.spyOn(Math, 'random').mockImplementation(() => {
      if (index === draws.length) throw new Error('Unexpected extra random draw');
      return draws[index++];
    });
    try {
      render(<Controlled onRun={onRun} />);
      expect(arrayValues('Weights')).toEqual([3, 3]);
      expect(arrayValues('Values')).toEqual([31, 41, 51]);

      fireEvent.click(screen.getByRole('button', { name: 'Randomize' }));
      // Independent expectations for the recorded draw sequence: no post-generation
      // sorting, padding or copied defaults can satisfy these paired index values.
      const generated = { weights: [3, 3], values: [26, 31], capacity: 13 };
      expect(arrayValues('Weights')).toEqual(generated.weights);
      expect(arrayValues('Values')).toEqual(generated.values);
      expect(screen.getByLabelText('Capacity')).toHaveValue(generated.capacity);
      expect(onRun).not.toHaveBeenCalled();
      expect(window.location.href).toBe(priorShareUrl);
      expect(replace).not.toHaveBeenCalled();
      expect(push).not.toHaveBeenCalled();

      fireEvent.click(screen.getByRole('button', { name: 'Run input' }));
      expect(onRun).toHaveBeenCalledTimes(1);
      expect(onRun).toHaveBeenCalledWith(generated);
      expect(arrayValues('Weights')).toEqual(generated.weights);
      expect(arrayValues('Values')).toEqual(generated.values);
      // InputPanel delegates execution; successful-run sharing belongs to the session.
    } finally {
      random.mockRestore();
      replace.mockRestore();
      push.mockRestore();
      window.history.replaceState({}, '', originalUrl);
    }
  });
});
