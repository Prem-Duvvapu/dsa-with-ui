import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
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

  it('never centres the row with justify-content, which hides the left end of a wide array', () => {
    // task-scheduler draws 26 cells. Centred, the overflow spilled past both edges and the
    // cells left of the stage could not be scrolled to: the canvas opened on D..W.
    const css = readFileSync(join(__dirname, 'ArrayCanvas.module.css'), 'utf8').replace(/\/\*[\s\S]*?\*\//g, '');
    const stage = css.match(/\.stage\s*\{([^}]*)\}/)[1];
    expect(stage).not.toMatch(/justify-content:\s*center/);
    expect(css).toMatch(/\.stage > :first-child\s*\{\s*margin-inline-start: auto;/);
  });

  it('scrolls the stage, not the page, to the cell the step is about', () => {
    // jsdom has no layout: give each cell a 60px slot and the stage a 300px viewport.
    const proto = window.HTMLElement.prototype;
    const saved = ['offsetLeft', 'offsetWidth', 'clientWidth'].map((k) => [k, Object.getOwnPropertyDescriptor(proto, k)]);
    Object.defineProperty(proto, 'offsetLeft', { configurable: true, get() { return [...(this.parentElement?.children || [])].indexOf(this) * 60; } });
    Object.defineProperty(proto, 'offsetWidth', { configurable: true, get() { return 40; } });
    Object.defineProperty(proto, 'clientWidth', { configurable: true, get() { return 300; } });
    try {
      const spec = Array.from({ length: 26 }, (_, i) => [i, i === 25 ? 'current' : 'default']);
      render(<ArrayCanvas currentStep={{ arrayState: cells(...spec) }} />);
      const stage = screen.getByTestId('array-stage');
      // Cell 25 spans 1500..1540; the stage must show it.
      expect(stage.scrollLeft).toBe(1540 - 300 + 24);
    } finally {
      for (const [k, d] of saved) {
        if (d) Object.defineProperty(proto, k, d); else delete proto[k];
      }
    }
  });
});
