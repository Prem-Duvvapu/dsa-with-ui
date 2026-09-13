import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import React from 'react';
import '@testing-library/jest-dom';
import IntervalCanvas from './IntervalCanvas';

describe('IntervalCanvas', () => {
  it('says it has no intervals rather than inventing four', () => {
    // This used to draw a hardcoded [1,2] [3,4] [0,6] [5,7] - n-meetings-in-one-room's own
    // default meetings, complete with invented 'settled' and 'probe' states - for any run
    // that reached the end of the resolution chain. Same defect as RCA-031's DsuCanvas:
    // a canvas that answers "I don't know" with a plausible picture. Currently unreachable
    // for both Interval tracers, which is exactly when it is cheap to remove.
    render(<IntervalCanvas problem={{ title: 'Meeting Rooms' }} />);
    expect(screen.getByText('Meeting Rooms')).toBeInTheDocument();
    expect(screen.queryByTestId('interval-span-1')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent(/no intervals/i);
  });

  it('renders intervals from resolvedInput start and end arrays', () => {
    // resolvedInput arrives as a PROP, off the trace. This test used to hang it on the step
    // instead, which is a shape the server has never sent - so it exercised a branch that
    // could not run in production and reported the dead code as covered.
    const mockStep = {
      arrayState: [
        { index: 0, state: 'settled' },
        { index: 1, state: 'probe' },
        { index: 2, state: 'default' }
      ]
    };

    render(
      <IntervalCanvas
        problem={{ title: 'N Meetings' }}
        step={mockStep}
        resolvedInput={{ start: [1, 3, 5], end: [4, 6, 8] }}
      />
    );

    expect(screen.getByText('N Meetings')).toBeInTheDocument();
    expect(screen.getByText('3 intervals (span 0 → 8)')).toBeInTheDocument();

    const span1 = screen.getByTestId('interval-span-1');
    const span2 = screen.getByTestId('interval-span-2');
    const span3 = screen.getByTestId('interval-span-3');

    expect(span1).toHaveTextContent('[1, 4]');
    expect(span2).toHaveTextContent('[3, 6]');
    expect(span3).toHaveTextContent('[5, 8]');
  });

  it('renders interval-grid inputs instead of falling back to canned meetings', () => {
    const mockStep = {
      resolvedInput: {
        intervals: [[2, 5], [6, 9], [10, 14]]
      },
      arrayState: [
        { index: 0, state: 'settled' },
        { index: 1, state: 'rejected' },
        { index: 2, state: 'probe' }
      ]
    };

    render(<IntervalCanvas problem={{ title: 'Erase Overlaps' }} step={mockStep} />);

    expect(screen.getByText('3 intervals (span 0 → 14)')).toBeInTheDocument();
    expect(screen.getByTestId('interval-span-1')).toHaveTextContent('[2, 5]');
    expect(screen.getByTestId('interval-span-2')).toHaveTextContent('[6, 9]');
    expect(screen.getByTestId('interval-span-3')).toHaveTextContent('[10, 14]');
  });

  it('uses live labelled array state when an interval algorithm reorders its input', () => {
    const mockStep = {
      resolvedInput: { intervals: [[8, 10], [1, 2]] },
      arrayState: [
        { index: 0, label: '[1,2]', state: 'settled' },
        { index: 1, label: '[8,10]', state: 'current' }
      ]
    };

    render(<IntervalCanvas problem={{ title: 'Sorted intervals' }} step={mockStep} />);

    expect(screen.getByTestId('interval-span-1')).toHaveTextContent('[1, 2]');
    expect(screen.getByTestId('interval-span-2')).toHaveTextContent('[8, 10]');
  });

  it('renders sweep line when lastEnd variable is present', () => {
    const mockStep = {
      resolvedInput: {
        start: [1, 5],
        end: [3, 7]
      },
      variables: {
        lastEnd: '3'
      }
    };

    render(
      <IntervalCanvas
        problem={{ title: 'Sweep Line Test' }}
        step={mockStep}
      />
    );

    expect(screen.getByText('t = 3')).toBeInTheDocument();
  });

  describe('the input the trace actually ran on', () => {
    // n-meetings-in-one-room is the real case: 15 of its 16 steps carry neither `intervals`
    // nor parseable arrayState labels, so they fall to the end of the chain. That last stop
    // used to be the inputSpec DEFAULTS, which are wrong the moment someone runs their own
    // input - the canvas drew the default meetings while the trace narrated theirs.
    const problem = {
      title: 'N Meetings in One Room',
      inputSpec: {
        fields: [
          { name: 'start', defaultValue: [1, 3, 0] },
          { name: 'end', defaultValue: [2, 4, 6] }
        ]
      }
    };

    it('draws the run\'s own intervals, not the spec defaults', () => {
      render(
        <IntervalCanvas
          problem={problem}
          currentStep={{ variables: {} }}
          resolvedInput={{ start: [10, 20], end: [15, 25] }}
        />
      );
      const stage = screen.getByTestId('interval-canvas-stage');
      expect(stage.textContent).toContain('10');
      expect(stage.textContent).toContain('25');
      // The defaults must not be what is on screen.
      expect(screen.queryByTestId('interval-span-3')).not.toBeInTheDocument();
    });

    it('accepts the array-of-pairs shape too', () => {
      render(
        <IntervalCanvas
          problem={problem}
          currentStep={{ variables: {} }}
          resolvedInput={{ intervals: [[7, 9], [11, 13]] }}
        />
      );
      const stage = screen.getByTestId('interval-canvas-stage');
      expect(stage.textContent).toContain('7');
      expect(stage.textContent).toContain('13');
    });

    it('still falls back to the spec defaults when no run has happened yet', () => {
      // Before the first trace arrives there is no resolvedInput, and the defaults are the
      // honest thing to draw - they are what a run would use.
      render(<IntervalCanvas problem={problem} currentStep={{ variables: {} }} />);
      expect(screen.getByTestId('interval-span-1')).toBeInTheDocument();
    });
  });
});
