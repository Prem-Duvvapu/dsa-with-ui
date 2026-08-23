import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import '@testing-library/jest-dom';
import IntArrayField from './IntArrayField';

const FIELD = { name: 'nums', constraints: { minLength: 2, maxLength: 4, minValue: -10, maxValue: 10 } };

describe('IntArrayField', () => {
  it('renders one chip per value', () => {
    render(<IntArrayField field={FIELD} value={[1, 2, 3]} onChange={() => {}} />);
    expect(screen.getByLabelText('Position 1 value')).toHaveValue(1);
    expect(screen.getByLabelText('Position 2 value')).toHaveValue(2);
    expect(screen.getByLabelText('Position 3 value')).toHaveValue(3);
  });

  it('editing a chip calls onChange with that position replaced', () => {
    const onChange = vi.fn();
    render(<IntArrayField field={FIELD} value={[1, 2, 3]} onChange={onChange} />);
    fireEvent.change(screen.getByLabelText('Position 2 value'), { target: { value: '99' } });
    expect(onChange).toHaveBeenCalledWith([1, 99, 3]);
  });

  it('Add appends a value, up to maxLength', () => {
    const onChange = vi.fn();
    const { rerender } = render(<IntArrayField field={FIELD} value={[1, 2, 3]} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /add/i }));
    expect(onChange).toHaveBeenCalledWith([1, 2, 3, 0]);

    rerender(<IntArrayField field={FIELD} value={[1, 2, 3, 0]} onChange={onChange} />);
    expect(screen.getByRole('button', { name: /add/i })).toBeDisabled();
  });

  it('Remove deletes that position, but disables at minLength', () => {
    const onChange = vi.fn();
    const { rerender } = render(<IntArrayField field={FIELD} value={[1, 2, 3]} onChange={onChange} />);
    fireEvent.click(screen.getByLabelText('Remove position 2'));
    expect(onChange).toHaveBeenCalledWith([1, 3]);

    rerender(<IntArrayField field={FIELD} value={[1, 3]} onChange={onChange} />);
    expect(screen.getByLabelText('Remove position 1')).toBeDisabled();
    expect(screen.getByLabelText('Remove position 2')).toBeDisabled();
  });

  it('a disabled Remove button does not fire onChange even when clicked', () => {
    const onChange = vi.fn();
    render(<IntArrayField field={FIELD} value={[1, 3]} onChange={onChange} />);
    fireEvent.click(screen.getByLabelText('Remove position 1'));
    expect(onChange).not.toHaveBeenCalled();
  });

  it('allowNulls lets a chip toggle to null and back', () => {
    const onChange = vi.fn();
    const treeField = { name: 'tree', constraints: { minLength: 1, maxLength: 10, minValue: -10, maxValue: 10 } };
    render(<IntArrayField field={treeField} value={[5, 6]} onChange={onChange} allowNulls />);

    fireEvent.click(screen.getByLabelText(/Position 2: value 6, click to clear/));
    expect(onChange).toHaveBeenCalledWith([5, null]);
  });

  it('without allowNulls, no toggle-to-null control is rendered', () => {
    render(<IntArrayField field={FIELD} value={[1, 2]} onChange={() => {}} />);
    expect(screen.queryByLabelText(/click to clear/)).not.toBeInTheDocument();
  });
});
