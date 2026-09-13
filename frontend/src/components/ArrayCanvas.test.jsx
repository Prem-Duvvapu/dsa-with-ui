import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import ArrayCanvas from './ArrayCanvas';

const cells = (...spec) => spec.map(([value, state, label], index) => ({ index, value, state, label }));

describe('ArrayCanvas', () => {
  it('shows the label a tracer attached to a cell', () => {
    // Three tracers set labels and none of them was ever drawn: candy's "0→1" (rating to
    // candies), job-sequencing's "J1 d2 p100", and minimum-platforms' A/D event kinds. The
    // bar height still means the value, so the label goes underneath, where the index sits
    // when there is nothing better to say.
    render(<ArrayCanvas currentStep={{ arrayState: cells([900, 'current', 'A'], [910, 'default', 'D']) }} />);

    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText('D')).toBeInTheDocument();
    // The value stays on top - it is what the bar's height encodes.
    expect(screen.getByText('900')).toBeInTheDocument();
  });

  it('falls back to the index when a cell has no label', () => {
    render(<ArrayCanvas currentStep={{ arrayState: cells([5, 'default'], [7, 'default']) }} />);
    expect(screen.getByText('[0]')).toBeInTheDocument();
    expect(screen.getByText('[1]')).toBeInTheDocument();
  });
});
