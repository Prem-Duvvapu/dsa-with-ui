import { act, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom';
import { CANVAS_BY_DSTYPE } from '../canvas/registry';
import DpTableCanvas from './DpTableCanvas';

/**
 * jsdom has no real layout engine, so every element's getBoundingClientRect() is zero
 * by default. That is fine for the "no arrows" cases below, but proving the arrow
 * ARITHMETIC (not just that something rendered) needs deterministic, distinguishable
 * rects — mocked per element by its Bench state class, the same way GraphCanvas.test.jsx
 * asserts exact edge coordinates rather than just "an SVG line exists somewhere".
 */
function mockRect(el, rect) {
  vi.spyOn(el, 'getBoundingClientRect').mockReturnValue({
    left: rect.left, top: rect.top, width: rect.width, height: rect.height,
    right: rect.left + rect.width, bottom: rect.top + rect.height
  });
}

const dpTable = {
  rowLabels: ['base', 'take'],
  colLabels: ['0', '1', '2', '3', '4'],
  cells: [
    [
      { value: '0', state: 'probe' },
      { value: '1', state: 'read' },
      { value: '1', state: 'known' },
      { value: '2', state: 'resolved' },
      { value: '∞', state: 'void' }
    ],
    [
      { value: '1', state: 'known' },
      { value: '1', state: 'known' },
      { value: '2', state: 'known' },
      { value: '3', state: 'known' },
      { value: '5', state: 'known' }
    ]
  ]
};

describe('DpTableCanvas', () => {
  it('renders labels, values, and all five states with non-colour glyphs', () => {
    render(<DpTableCanvas currentStep={{ dpTable }} />);

    expect(screen.getByRole('table', { name: 'Dynamic programming table' })).toBeInTheDocument();
    expect(screen.getByText('base')).toBeInTheDocument();
    expect(screen.getByText('take')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();

    const probe = screen.getByLabelText('row base, column 0: 0 (probe)');
    const read = screen.getByLabelText('row base, column 1: 1 (read)');
    const known = screen.getByLabelText('row base, column 2: 1 (known)');
    const resolved = screen.getByLabelText('row base, column 3: 2 (resolved)');
    const voidCell = screen.getByLabelText('row base, column 4: ∞ (void)');

    expect(probe).toHaveTextContent('▼');
    expect(read).toHaveTextContent('○');
    expect(read).toHaveClass('dp-cell-read');
    expect(known).toHaveTextContent('□');
    expect(resolved).toHaveTextContent('✓');
    expect(voidCell).toHaveTextContent('▫');
    expect(voidCell).toHaveClass('dp-cell-void');
  });

  it('renders a safe empty state for missing or malformed table data', () => {
    const { rerender } = render(<DpTableCanvas currentStep={{}} />);
    expect(screen.getByText('No DP table data')).toBeInTheDocument();

    rerender(<DpTableCanvas currentStep={{ dpTable: { rowLabels: null, colLabels: {}, cells: 'bad' } }} />);
    expect(screen.getByText('No DP table data')).toBeInTheDocument();
  });

  it('does not fabricate blank cells from labels-only or ragged payloads', () => {
    const { rerender } = render(
      <DpTableCanvas
        currentStep={{ dpTable: { rowLabels: ['dp'], colLabels: ['0', '1'], cells: [] } }}
      />
    );

    expect(screen.getByText('No DP table data')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();

    rerender(
      <DpTableCanvas
        currentStep={{
          dpTable: {
            rowLabels: ['a', 'b'],
            colLabels: ['0', '1'],
            cells: [[{ value: '1', state: 'known' }], []]
          }
        }}
      />
    );

    expect(screen.getByText('No DP table data')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('is the registered renderer for DpTable traces', () => {
    expect(CANVAS_BY_DSTYPE.DpTable).toBe(DpTableCanvas);
  });

  describe('provenance arrows', () => {
    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('draws one arrow per read cell, each pointing at the probe cell', () => {
      const { container } = render(<DpTableCanvas currentStep={{ dpTable }} />);

      const wrap = container.querySelector('.dp-table-wrap');
      mockRect(wrap, { left: 0, top: 0, width: 400, height: 200 });
      const probe = container.querySelector('.dp-cell-probe');
      mockRect(probe, { left: 100, top: 20, width: 40, height: 30 });
      const read = container.querySelector('.dp-cell-read');
      mockRect(read, { left: 200, top: 20, width: 40, height: 30 });

      // Trigger the layout effect's recompute path deterministically.
      act(() => { window.dispatchEvent(new Event('resize')); });

      const arrows = container.querySelectorAll('.dp-arrow');
      expect(arrows).toHaveLength(1);
      // Centre of the read cell (220, 35) -> centre of the probe cell (120, 35).
      expect(arrows[0].getAttribute('x1')).toBe('220');
      expect(arrows[0].getAttribute('y1')).toBe('35');
      expect(arrows[0].getAttribute('x2')).toBe('120');
      expect(arrows[0].getAttribute('y2')).toBe('35');
      expect(arrows[0].getAttribute('stroke')).toBe('var(--probe)');
      expect(arrows[0].getAttribute('marker-end')).toBe('url(#dp-arrowhead)');
    });

    it('offsets by the wrap\'s own position and scroll, not just the cell rects', () => {
      const { container } = render(<DpTableCanvas currentStep={{ dpTable }} />);

      const wrap = container.querySelector('.dp-table-wrap');
      mockRect(wrap, { left: 50, top: 10, width: 400, height: 200 });
      Object.defineProperty(wrap, 'scrollLeft', { value: 15, configurable: true });
      Object.defineProperty(wrap, 'scrollTop', { value: 5, configurable: true });
      const probe = container.querySelector('.dp-cell-probe');
      mockRect(probe, { left: 150, top: 30, width: 40, height: 30 });
      const read = container.querySelector('.dp-cell-read');
      mockRect(read, { left: 250, top: 30, width: 40, height: 30 });

      act(() => { window.dispatchEvent(new Event('resize')); });

      const arrow = container.querySelector('.dp-arrow');
      // read centre relative to wrap: (250-50+15+20, 30-10+5+15) = (235, 40)
      expect(arrow.getAttribute('x1')).toBe('235');
      expect(arrow.getAttribute('y1')).toBe('40');
      // probe centre relative to wrap: (150-50+15+20, 30-10+5+15) = (135, 40)
      expect(arrow.getAttribute('x2')).toBe('135');
      expect(arrow.getAttribute('y2')).toBe('40');
    });

    it('draws no arrows when the step has no read cell', () => {
      const noReadTable = {
        rowLabels: ['dp'],
        colLabels: ['0', '1'],
        cells: [[
          { value: '1', state: 'resolved' },
          { value: '2', state: 'probe' }
        ]]
      };
      const { container } = render(<DpTableCanvas currentStep={{ dpTable: noReadTable }} />);

      act(() => { window.dispatchEvent(new Event('resize')); });

      expect(container.querySelector('.dp-arrows')).not.toBeInTheDocument();
      expect(container.querySelectorAll('.dp-arrow')).toHaveLength(0);
    });

    it('draws no arrows when the step has no probe cell', () => {
      const noProbeTable = {
        rowLabels: ['dp'],
        colLabels: ['0', '1'],
        cells: [[
          { value: '1', state: 'read' },
          { value: '2', state: 'resolved' }
        ]]
      };
      const { container } = render(<DpTableCanvas currentStep={{ dpTable: noProbeTable }} />);

      act(() => { window.dispatchEvent(new Event('resize')); });

      expect(container.querySelector('.dp-arrows')).not.toBeInTheDocument();
    });
  });

  describe('recurrence line (design D3)', () => {
    it('renders the formula and its live substitution when the tracer supplies both', () => {
      const tableWithRecurrence = {
        ...dpTable,
        formula: 'ways[i] = ways[i-1] + ways[i-2]',
        substitution: 'ways[4] = ways[3] + ways[2] = 3 + 2 = 5'
      };
      render(<DpTableCanvas currentStep={{ dpTable: tableWithRecurrence }} />);

      expect(screen.getByText('ways[i] = ways[i-1] + ways[i-2]')).toBeInTheDocument();
      expect(screen.getByText('ways[4] = ways[3] + ways[2] = 3 + 2 = 5')).toBeInTheDocument();
    });

    it('renders no recurrence block for a tracer that has not adopted D3 yet', () => {
      // The shared fixture `dpTable` carries no formula/substitution — the common case
      // today (most DP_TABLE tracers). Nothing should render, and nothing should throw.
      const { container } = render(<DpTableCanvas currentStep={{ dpTable }} />);

      expect(container.querySelector('.dp-recurrence')).not.toBeInTheDocument();
    });

    it('never renders one line without the other', () => {
      const formulaOnly = { ...dpTable, formula: 'ways[i] = ways[i-1] + ways[i-2]', substitution: null };
      const { container: c1 } = render(<DpTableCanvas currentStep={{ dpTable: formulaOnly }} />);
      expect(c1.querySelector('.dp-recurrence')).not.toBeInTheDocument();

      const substitutionOnly = { ...dpTable, formula: null, substitution: 'ways[4] = 5' };
      const { container: c2 } = render(<DpTableCanvas currentStep={{ dpTable: substitutionOnly }} />);
      expect(c2.querySelector('.dp-recurrence')).not.toBeInTheDocument();
    });
  });
});
