import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import React from 'react';
import '@testing-library/jest-dom';
import IntervalCanvas from './IntervalCanvas';

describe('IntervalCanvas', () => {
  it('renders default intervals when step is empty', () => {
    render(<IntervalCanvas problem={{ title: 'Meeting Rooms' }} />);
    expect(screen.getByText('Meeting Rooms')).toBeInTheDocument();
    expect(screen.getByTestId('interval-canvas-stage')).toBeInTheDocument();
    expect(screen.getByTestId('interval-span-1')).toBeInTheDocument();
  });

  it('renders intervals from resolvedInput start and end arrays', () => {
    const mockStep = {
      resolvedInput: {
        start: [1, 3, 5],
        end: [4, 6, 8]
      },
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
});
