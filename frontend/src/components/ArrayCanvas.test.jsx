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

  it('draws only its stage - CanvasShell owns the chrome', () => {
    // App wraps every canvas in CanvasShell, which renders the title, the step counter and
    // Bench's five-state legend. This canvas also drew its own header and its own FOUR-badge
    // legend in the pre-Bench vocabulary, so the screen carried two headers and two legends
    // that disagreed about what the states are. GraphCanvas, DsuCanvas and DpTableCanvas
    // were migrated off their chrome when the shell was built; these three were missed.
    const { container } = render(
      <ArrayCanvas currentStep={{ arrayState: cells([5, 'current'], [7, 'default']) }} />
    );

    expect(screen.queryByText(/visualizer/i)).not.toBeInTheDocument();
    expect(screen.queryByText('Current')).not.toBeInTheDocument();
    expect(screen.queryByText('Visited')).not.toBeInTheDocument();
    // The stage itself is still there.
    expect(container.querySelector('[data-testid="array-stage"]')).toBeInTheDocument();
  });

  it('falls back to the index when a cell has no label', () => {
    render(<ArrayCanvas currentStep={{ arrayState: cells([5, 'default'], [7, 'default']) }} />);
    expect(screen.getByText('[0]')).toBeInTheDocument();
    expect(screen.getByText('[1]')).toBeInTheDocument();
  });
});
