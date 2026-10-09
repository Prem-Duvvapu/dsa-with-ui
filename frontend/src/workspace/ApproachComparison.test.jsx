import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi } from 'vitest';
import ApproachComparison from './ApproachComparison';

vi.mock('../hooks/useApproachComparison', () => ({ default: () => ({
  pending: false, results: null, compare: vi.fn(), clear: vi.fn()
}) }));

function mount(constraints = { minLength: 2, maxLength: 10, minValue: 0, maxValue: 999 }) {
  const approaches = ['canonical', 'recursion'].map(id => ({ id,
    label: id === 'canonical' ? 'Tabulation' : 'Recursion',
    inputSpec: { fields: [{ name: 'heights', label: 'Stair heights', type: 'INT_ARRAY', constraints }] }
  }));
  render(<ApproachComparison problemId="frog-jump" run={{ approachId: 'canonical', resolvedInput: { heights: [5, 5] } }}
    approaches={approaches} onPrepare={vi.fn()} />);
}

describe('truthful comparison safety limits for collection inputs', () => {
  it('shows the declared array length and element bounds before a comparison runs', () => {
    mount();
    expect(screen.getByText(/Recursion: Stair heights length 2–10, values 0–999/)).toBeVisible();
    expect(screen.queryByText(/see input constraints/)).not.toBeInTheDocument();
  });
  it('does not treat zero length or zero value bounds as missing', () => {
    mount({ minLength: 0, maxLength: 0, minValue: 0, maxValue: 0 });
    expect(screen.getByText(/Recursion: Stair heights length 0–0, values 0–0/)).toBeVisible();
  });
  it('does not invent bounds when the metadata is absent', () => {
    mount({});
    expect(screen.getByText(/Recursion: Stair heights see input constraints/)).toBeVisible();
    expect(screen.queryByText(/values 0–999/)).not.toBeInTheDocument();
  });
});
