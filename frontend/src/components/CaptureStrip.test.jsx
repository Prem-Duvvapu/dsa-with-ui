import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import CaptureStrip, { benchState } from './CaptureStrip';

/**
 * jsdom has no 2d context, so without this the band paints nothing and its bucketing —
 * the only reason the band mode exists — is never executed by any test. The fake records
 * fills instead, which is both quieter and the thing worth asserting on.
 */
const PAINT = {
  '--probe': '#ffb000',
  '--settled': '#3ddc97',
  '--bench-fill': '#16202a',
  '--bench-recessed': '#0e141b'
};

function fakeCanvas() {
  const fills = [];
  let fillStyle = '';
  const ctx = {
    scale: () => {},
    set fillStyle(v) { fillStyle = v; },
    get fillStyle() { return fillStyle; },
    fillRect: (x, y, w, h) => fills.push({ x, y, w, h, fill: fillStyle })
  };
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(ctx);

  // jsdom does not inherit custom properties down the tree, so the component's
  // getComputedStyle(canvas).getPropertyValue('--probe') would come back empty and every
  // state would paint the same fallback colour — a test that cannot tell them apart.
  const real = window.getComputedStyle.bind(window);
  vi.spyOn(window, 'getComputedStyle').mockImplementation((el) => {
    const style = real(el);
    return {
      ...style,
      getPropertyValue: (name) => PAINT[name] ?? style.getPropertyValue(name)
    };
  });

  return fills;
}

const arrayStep = (n, states) => ({
  stepNumber: n,
  activeLine: 1,
  description: `step ${n}`,
  arrayState: states.map((state, index) => ({ index, value: index * 2, state }))
});

describe('benchState', () => {
  it('maps every backend vocabulary onto the two hues plus two neutrals', () => {
    expect(benchState('comparing')).toBe('probe');
    expect(benchState('visiting')).toBe('probe');
    expect(benchState('sorted')).toBe('settled');
    expect(benchState('visited')).toBe('settled');
    expect(benchState('unvisited')).toBe('void');
    expect(benchState('default')).toBe('known');
  });

  it('falls back to known rather than inventing a fifth state', () => {
    // A tracer is free to emit any state string. Guessing a new colour for one we do
    // not recognise is how a two-hue palette becomes a five-hue one.
    expect(benchState('some-future-state')).toBe('known');
    expect(benchState(undefined)).toBe('void');
  });
});

describe('CaptureStrip', () => {
  afterEach(() => vi.restoreAllMocks());

  const steps = [
    arrayStep(1, ['default', 'default', 'default']),
    arrayStep(2, ['comparing', 'default', 'default']),
    arrayStep(3, ['sorted', 'comparing', 'default'])
  ];

  it('renders one column per step and one row per slot', () => {
    const { container } = render(<CaptureStrip steps={steps} current={1} />);
    expect(screen.getAllByRole('button', { name: /^Step \d+ of 3$/ })).toHaveLength(3);
    expect(container.querySelector('.cs-label').textContent).toBe(
      'Execution capture — 3 steps, 3 rows'
    );
  });

  it('marks the current column', () => {
    render(<CaptureStrip steps={steps} current={2} />);
    const columns = screen.getAllByRole('button', { name: /^Step \d+ of 3$/ });
    expect(columns[2].className).toContain('cs-col-now');
    expect(columns[0].className).not.toContain('cs-col-now');
  });

  it('seeks when a column is clicked', () => {
    const onSeek = vi.fn();
    render(<CaptureStrip steps={steps} current={0} onSeek={onSeek} />);
    fireEvent.click(screen.getByRole('button', { name: 'Step 3 of 3' }));
    expect(onSeek).toHaveBeenCalledWith(2);
  });

  it('pairs every state with a glyph, never colour alone', () => {
    const { container } = render(<CaptureStrip steps={steps} current={0} />);
    expect(container.querySelector('.cs-probe').textContent).toContain('▼');
    expect(container.querySelector('.cs-settled').textContent).toContain('✓');
  });

  it('drops per-cell labels once the trace is too long to read them', () => {
    const many = Array.from({ length: 120 }, (_, i) => arrayStep(i + 1, ['default']));
    const { container } = render(<CaptureStrip steps={many} current={0} />);
    expect(screen.getByText('compressed')).toBeTruthy();
    expect(container.querySelector('.cs-row-label').textContent).toBe('');
  });

  it('switches to a painted band rather than 200,000 DOM nodes', () => {
    // 5000 columns x 40 rows of <span> will hang the browser. Past the threshold the
    // strip is a canvas, and it becomes a slider so it stays keyboard-reachable.
    const many = Array.from({ length: 600 }, (_, i) => arrayStep(i + 1, ['default']));
    const { container } = render(<CaptureStrip steps={many} current={0} />);
    expect(container.querySelector('canvas')).toBeTruthy();
    expect(screen.getByRole('slider')).toBeTruthy();
    expect(container.querySelectorAll('.cs-cell')).toHaveLength(0);
  });

  it('seeks with the keyboard in band mode', () => {
    const onSeek = vi.fn();
    const many = Array.from({ length: 600 }, (_, i) => arrayStep(i + 1, ['default']));
    render(<CaptureStrip steps={many} current={10} onSeek={onSeek} />);
    fireEvent.keyDown(screen.getByRole('slider'), { key: 'ArrowRight' });
    expect(onSeek).toHaveBeenCalledWith(11);
  });

  it('clamps a seek to the trace rather than running off the end', () => {
    const onSeek = vi.fn();
    render(<CaptureStrip steps={steps} current={2} onSeek={onSeek} />);
    // The band's ArrowRight at the last step, and any other out-of-range seek, must
    // land on a step that exists — App.jsx indexes steps[current] unguarded.
    fireEvent.click(screen.getByRole('button', { name: 'Step 3 of 3' }));
    expect(onSeek).toHaveBeenCalledWith(2);
  });

  it('buckets rather than samples when there are more steps than pixels', () => {
    // Downsampling would drop whole columns, and what it drops is exactly the sweeps
    // that make the band worth having. One lone probe among 5000 settled steps must
    // still be painted.
    const fills = fakeCanvas();
    const many = Array.from({ length: 5000 }, (_, i) =>
      arrayStep(i + 1, [i === 2500 ? 'comparing' : 'sorted'])
    );
    render(<CaptureStrip steps={many} current={0} />);

    // Everything except the playhead, which is painted last and is also --probe.
    const band = fills.slice(0, -1);
    const probes = band.filter((f) => f.fill === PAINT['--probe']);
    const settled = band.filter((f) => f.fill === PAINT['--settled']);

    expect(settled.length).toBeGreaterThan(100);
    expect(probes).toHaveLength(1);

    // ...and in the right place: halfway along, not folded into column 0.
    const width = Math.max(...band.map((f) => f.x + f.w));
    expect(probes[0].x / width).toBeGreaterThan(0.4);
    expect(probes[0].x / width).toBeLessThan(0.6);
  });

  it('renders nothing when there is no trace', () => {
    const { container } = render(<CaptureStrip steps={[]} current={0} />);
    expect(container.firstChild).toBeNull();
  });
});
