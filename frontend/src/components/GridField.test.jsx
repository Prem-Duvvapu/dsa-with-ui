import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import '@testing-library/jest-dom';
import GridField from './GridField';

describe('GridField', () => {
  it('respects a declared minimum column count such as three training activities', () => {
    const onChange = vi.fn();
    render(<GridField field={{ constraints: { minCols: 3, maxCols: 3, maxValue: 100 } }} value={[[10, 20, 30]]} onChange={onChange} />);
    expect(screen.getByRole('button', { name: 'Remove a column' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Add a column' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'Remove a column' }));
    expect(onChange).not.toHaveBeenCalled();
  });

  it('keeps a below-minimum draft exact until the user explicitly adds its missing column', () => {
    const onChange = vi.fn();
    render(<GridField field={{ constraints: { minCols: 3, maxCols: 3, minValue: 0, maxValue: 100 } }} value={[[10, 20], [30, 40]]} onChange={onChange} />);
    expect(screen.getAllByRole('button', { name: /^Cell row/ })).toHaveLength(4);
    expect(screen.getByLabelText('Cell row 2, column 2, value 40')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Remove a column' })).toBeDisabled();
    expect(onChange).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Add a column' }));
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith([[10, 20, 0], [30, 40, 0]]);
  });

  it.each([[], null, [[1, 2], null], [[1, 2], 3], [[1], [2, 3]]].map(value => ({ value })))('preserves unsupported grid input instead of inventing cells or crashing: $value', ({ value }) => {
    const onChange = vi.fn();
    render(<GridField field={{ type: 'INT_GRID', constraints: { maxRows: 5, maxCols: 5 } }} value={value} onChange={onChange} />);
    expect(screen.getByText(/cannot show this input as grid cells/i)).toBeInTheDocument();
    expect(screen.getByLabelText('Current grid input').textContent).toBe(JSON.stringify(value, null, 2));
    expect(screen.queryByRole('button', { name: /Cell row/ })).not.toBeInTheDocument();
    expect(onChange).not.toHaveBeenCalled();
  });

  it('clicking a cell cycles it toward maxValue then wraps to minValue', () => {
    const field = { name: 'grid', constraints: { minValue: 0, maxValue: 1 } };
    const onChange = vi.fn();
    render(<GridField field={field} value={[[0, 0], [0, 0]]} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText('Cell row 1, column 1, value 0'));
    expect(onChange).toHaveBeenCalledWith([[1, 0], [0, 0]]);
  });

  it('wraps a cell already at maxValue back to minValue', () => {
    const field = { name: 'grid', constraints: { minValue: 0, maxValue: 1 } };
    const onChange = vi.fn();
    render(<GridField field={field} value={[[1, 0]]} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText('Cell row 1, column 1, value 1'));
    expect(onChange).toHaveBeenCalledWith([[0, 0]]);
  });

  it('Add a row appends a row of minValue, and disables at maxRows', () => {
    const field = { name: 'grid', constraints: { minValue: 0, maxValue: 1, maxRows: 2 } };
    const onChange = vi.fn();
    const { rerender } = render(<GridField field={field} value={[[0, 0]]} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText('Add a row'));
    expect(onChange).toHaveBeenCalledWith([[0, 0], [0, 0]]);

    rerender(<GridField field={field} value={[[0, 0], [0, 0]]} onChange={onChange} />);
    expect(screen.getByLabelText('Add a row')).toBeDisabled();
  });

  it('Remove a column removes the last column, and disables at one column', () => {
    const field = { name: 'grid', constraints: { minValue: 0, maxValue: 1 } };
    const onChange = vi.fn();
    const { rerender } = render(<GridField field={field} value={[[0, 1], [1, 0]]} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText('Remove a column'));
    expect(onChange).toHaveBeenCalledWith([[0], [1]]);

    rerender(<GridField field={field} value={[[0], [1]]} onChange={onChange} />);
    expect(screen.getByLabelText('Remove a column')).toBeDisabled();
  });
});
