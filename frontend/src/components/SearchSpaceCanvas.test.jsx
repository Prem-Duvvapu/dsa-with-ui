import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import SearchSpaceCanvas from './SearchSpaceCanvas';
import styles from './SearchSpaceCanvas.module.css';

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

  it('renders an exhausted interval as zero candidates without a live marker', () => {
    const steps = [answerStep(90, 203, 146), {
      ...answerStep(113, 112, null), variables: { low: '113', high: '112', answer: '113' }
    }];
    render(<SearchSpaceCanvas steps={steps} currentStepIndex={1} currentStep={steps[1]} />);
    expect(screen.getByTestId('search-range')).toHaveTextContent('0 left of 114');
    expect(screen.queryByTestId('search-live')).not.toBeInTheDocument();
    expect(screen.queryByTestId('search-mid')).not.toBeInTheDocument();
    expect(screen.getByTestId('search-result')).toHaveTextContent('Result: 113');
  });

  it('does not render a negative candidate count if an interval crosses by more than one', () => {
    render(<SearchSpaceCanvas currentStep={answerStep(9, 3, null)} />);
    expect(screen.getByTestId('search-range')).toHaveTextContent('0 left');
    expect(screen.queryByTestId('search-live')).not.toBeInTheDocument();
  });

  it('reads current input highlights even when commentary carries the prior paired range', () => {
    const first = answerStep(1, 11, 6);
    const scan = { variables: { hours: '4' }, arrayState: first.arrayState.map((cell, i) => ({
      ...cell, state: i === 2 ? 'current' : 'default'
    })) };
    render(<SearchSpaceCanvas steps={[first, scan]} currentStepIndex={1} currentStep={scan} />);
    const cells = screen.getByTestId('search-input-row').querySelectorAll('[data-state]');
    expect([...cells].map((cell) => cell.getAttribute('data-state'))).toEqual(['default', 'default', 'current', 'default']);
    expect(cells[2]).toHaveClass(styles.cellMid);
    expect(cells[0]).not.toHaveClass(styles.cellMid);
  });

  it('does not treat student counts or exponents named m as a midpoint', () => {
    render(<SearchSpaceCanvas currentStep={{ variables: { low: '1', high: '20', m: '3' } }} />);
    expect(screen.queryByTestId('search-mid')).not.toBeInTheDocument();
  });

  it.each([null, '', '  ', false, [], 'Infinity', 'not-a-number'])('does not fabricate a range from invalid low=%j', (low) => {
    render(<SearchSpaceCanvas currentStep={{ variables: { low, high: '6' } }} />);
    expect(screen.getByRole('status')).toHaveTextContent('No search range');
  });

  it('never combines partial bounds across steps or different alias families', () => {
    const steps = [{ variables: { low: '1' } }, { variables: { high: '9' } }, { variables: { lo: '2', high: '8' } }];
    render(<SearchSpaceCanvas steps={steps} currentStepIndex={2} currentStep={steps[2]} />);
    expect(screen.getByRole('status')).toHaveTextContent('No search range');
  });

  it.each([['lo', 'hi'], ['left', 'right'], ['l', 'r'], ['start', 'end']])('still accepts the complete %s/%s alias pair', (low, high) => {
    render(<SearchSpaceCanvas currentStep={{ variables: { [low]: '2', [high]: '8', mid: '5' } }} />);
    expect(screen.getByTestId('search-range')).toHaveTextContent('[2, 8]');
    expect(screen.getByTestId('search-mid')).toHaveTextContent('probing 5');
  });

  it('does not infer a result from the last visible probe or an intermediate ans', () => {
    const finalVisible = { ...answerStep(4, 6, 5), variables: { low: '4', high: '6', mid: '5', ans: '9' } };
    render(<SearchSpaceCanvas steps={[finalVisible]} currentStepIndex={0} currentStep={finalVisible} />);
    expect(screen.getByTestId('search-mid')).toHaveTextContent('probing 5');
    expect(screen.queryByTestId('search-result')).not.toBeInTheDocument();
    expect(screen.queryByText(/complete|finished|converged/i)).not.toBeInTheDocument();
  });

  it('does not carry a prior answer into a later phase', () => {
    const first = { ...answerStep(1, 8, null), variables: { low: '1', high: '8', answer: '4' } };
    const next = answerStep(2, 7, 4);
    const { rerender } = render(<SearchSpaceCanvas steps={[first, next]} currentStepIndex={0} currentStep={first} />);
    expect(screen.getByTestId('search-result')).toHaveTextContent('Result: 4');
    rerender(<SearchSpaceCanvas steps={[first, next]} currentStepIndex={1} currentStep={next} />);
    expect(screen.queryByTestId('search-result')).not.toBeInTheDocument();
  });
});
