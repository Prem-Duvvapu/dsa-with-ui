import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import '@testing-library/jest-dom';
import GraphField from './GraphField';

describe('GraphField', () => {
  it('renders vertex count and each edge as a from/to pair', () => {
    const field = { name: 'graph', constraints: { maxVertices: 10, maxEdges: 10 } };
    render(<GraphField field={field} value={{ vertices: 4, edges: [[0, 1], [1, 2]] }} onChange={() => {}} />);

    expect(screen.getByLabelText('Vertices')).toHaveValue(4);
    expect(screen.getByLabelText('Edge 1 from vertex')).toHaveValue(0);
    expect(screen.getByLabelText('Edge 1 to vertex')).toHaveValue(1);
    expect(screen.getByLabelText('Edge 2 from vertex')).toHaveValue(1);
    expect(screen.getByLabelText('Edge 2 to vertex')).toHaveValue(2);
  });

  it('changing vertices clamps to [1, maxVertices]', () => {
    const field = { name: 'graph', constraints: { maxVertices: 5 } };
    const onChange = vi.fn();
    render(<GraphField field={field} value={{ vertices: 3, edges: [] }} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Vertices'), { target: { value: '99' } });
    expect(onChange).toHaveBeenCalledWith({ vertices: 5, edges: [] });

    fireEvent.change(screen.getByLabelText('Vertices'), { target: { value: '0' } });
    expect(onChange).toHaveBeenCalledWith({ vertices: 1, edges: [] });
  });

  it('Add edge appends [0,0], bounded by maxEdges', () => {
    const field = { name: 'graph', constraints: { maxVertices: 5, maxEdges: 1 } };
    const onChange = vi.fn();
    const { rerender } = render(<GraphField field={field} value={{ vertices: 3, edges: [] }} onChange={onChange} />);

    fireEvent.click(screen.getByRole('button', { name: /add edge/i }));
    expect(onChange).toHaveBeenCalledWith({ vertices: 3, edges: [[0, 0]] });

    rerender(<GraphField field={field} value={{ vertices: 3, edges: [[0, 0]] }} onChange={onChange} />);
    expect(screen.getByRole('button', { name: /add edge/i })).toBeDisabled();
  });

  it('Add edge appends a weight too when the spec is weighted', () => {
    const field = { name: 'graph', constraints: { maxVertices: 5, weighted: true } };
    const onChange = vi.fn();
    render(<GraphField field={field} value={{ vertices: 3, edges: [] }} onChange={onChange} />);

    fireEvent.click(screen.getByRole('button', { name: /add edge/i }));
    expect(onChange).toHaveBeenCalledWith({ vertices: 3, edges: [[0, 0, 1]] });
  });

  it('shows a weight input per edge only when weighted', () => {
    const plain = { name: 'graph', constraints: {} };
    const { rerender } = render(<GraphField field={plain} value={{ vertices: 3, edges: [[0, 1]] }} onChange={() => {}} />);
    expect(screen.queryByLabelText('Edge 1 weight')).not.toBeInTheDocument();

    const weighted = { name: 'graph', constraints: { weighted: true } };
    rerender(<GraphField field={weighted} value={{ vertices: 3, edges: [[0, 1, 5]] }} onChange={() => {}} />);
    expect(screen.getByLabelText('Edge 1 weight')).toHaveValue(5);
  });

  it('removing an edge drops just that one', () => {
    const field = { name: 'graph', constraints: {} };
    const onChange = vi.fn();
    render(<GraphField field={field} value={{ vertices: 3, edges: [[0, 1], [1, 2]] }} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText('Remove edge 1'));
    expect(onChange).toHaveBeenCalledWith({ vertices: 3, edges: [[1, 2]] });
  });
});
