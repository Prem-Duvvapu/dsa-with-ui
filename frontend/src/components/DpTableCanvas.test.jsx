import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import '@testing-library/jest-dom';
import { CANVAS_BY_DSTYPE } from '../canvas/registry';
import DpTableCanvas from './DpTableCanvas';

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

  it('is the registered renderer for DpTable traces', () => {
    expect(CANVAS_BY_DSTYPE.DpTable).toBe(DpTableCanvas);
  });
});
