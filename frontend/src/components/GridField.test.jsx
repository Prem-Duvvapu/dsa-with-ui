import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import '@testing-library/jest-dom';
import GridField from './GridField';

describe('GridField', () => {
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
