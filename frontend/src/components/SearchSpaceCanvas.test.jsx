import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import SearchSpaceCanvas from './SearchSpaceCanvas';

/** An index-space step: high indexes the array and the discarded half is already marked. */
const indexStep = (low, high, mid, size = 7) => ({
  variables: { low: String(low), high: String(high), ...(mid !== null ? { mid: String(mid) } : {}) },
  arrayState: Array.from({ length: size }, (_, i) => ({
    index: i,
    value: (i + 1) * 2,
    state: i >= low && i <= high ? 'target' : 'visited'
  }))
});

/** An answer-space step: the range is over values, the array is just the input. */
const answerStep = (low, high, mid) => ({
  variables: { low: String(low), high: String(high), ...(mid !== null ? { mid: String(mid) } : {}) },
  arrayState: [3, 6, 7, 11].map((v, i) => ({ index: i, value: v, state: 'default' }))
});

describe('SearchSpaceCanvas', () => {
  it('shows how much of the space is left, against how much there was', () => {
    // Halving reads as halving only when the original span is still on screen beside it.
    const steps = [indexStep(0, 6, 3), indexStep(2, 6, 4)];
    render(<SearchSpaceCanvas steps={steps} currentStepIndex={1} currentStep={steps[1]} />);
    expect(screen.getByTestId('search-range').textContent).toContain('[2, 6]');
    expect(screen.getByTestId('search-range').textContent).toContain('5 left of 7');
  });

  it('does not change its mind about what the range means mid-animation', () => {
    // aggressive-cows searches DISTANCES 1..8 while its array holds five cow positions.
    // Once high shrinks below the cell count the per-step heuristic flips to index mode
    // and starts calling a distance an index - a lie the viewer watches appear halfway
    // through the run. The kind of space a tracer searches is fixed for the whole trace,
    // so read it once, off the first step that states a range.
    const distances = (low, high, mid) => ({
      variables: { low: String(low), high: String(high), mid: String(mid) },
      arrayState: [0, 3, 4, 7, 9].map((v, i) => ({ index: i, value: v, state: 'default' }))
    });
    const steps = [distances(1, 8, 4), distances(1, 3, 2), distances(3, 3, 3)];
    render(<SearchSpaceCanvas steps={steps} currentStepIndex={2} currentStep={steps[2]} />);
    expect(screen.getByTestId('search-range').textContent).toContain('answers [3, 3]');
    expect(screen.queryByTestId('search-cells')).not.toBeInTheDocument();
  });

  it('marks the value being probed', () => {
    render(<SearchSpaceCanvas currentStep={indexStep(0, 6, 3)} />);
    expect(screen.getByTestId('search-mid').textContent).toContain('probing 3');
  });

  it('keeps discarded cells on screen in an index search', () => {
    // Struck through, not removed: the half thrown away is the lesson.
    const { container } = render(<SearchSpaceCanvas currentStep={indexStep(4, 6, 5)} />);
    expect(container.querySelectorAll('[data-alive="true"]')).toHaveLength(3);
    expect(screen.getByTestId('search-cells').children).toHaveLength(7);
  });

  it('does not frame an answer-space array as if it were the search space', () => {
    // koko searches speeds 1..11 while its array holds four piles. Treating the array as
    // the range would frame four piles as a span of eleven.
    render(<SearchSpaceCanvas currentStep={answerStep(1, 11, 6)} />);
    expect(screen.getByTestId('search-range').textContent).toContain('answers [1, 11]');
    expect(screen.getByTestId('search-input-row')).toBeInTheDocument();
    expect(screen.queryByTestId('search-cells')).not.toBeInTheDocument();
  });

  it('calls an index search what it is', () => {
    render(<SearchSpaceCanvas currentStep={indexStep(0, 6, 3)} />);
    expect(screen.getByTestId('search-range').textContent).toContain('indices [0, 6]');
    expect(screen.queryByTestId('search-input-row')).not.toBeInTheDocument();
  });

  it('holds the range through a step that states no bounds', () => {
    const steps = [indexStep(0, 6, 3), { variables: { hours: '4' } }];
    render(<SearchSpaceCanvas steps={steps} currentStepIndex={1} currentStep={steps[1]} />);
    expect(screen.getByTestId('search-range').textContent).toContain('[0, 6]');
  });

  it('says so when no step has stated a range yet', () => {
    render(<SearchSpaceCanvas currentStep={{ variables: {} }} />);
    expect(screen.getByRole('status').textContent).toContain('No search range');
  });
});
