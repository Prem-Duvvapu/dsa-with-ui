import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import InputSummary from './InputSummary';

describe('InputSummary', () => {
  it('states each input the server actually ran on', () => {
    render(<InputSummary resolvedInput={{ nums: [-2, 1, -3, 4], target: 9 }} />);
    expect(screen.getByText('nums')).toBeInTheDocument();
    expect(screen.getByText('[-2, 1, -3, 4]')).toBeInTheDocument();
    expect(screen.getByText('target')).toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
  });

  it('keeps a grid readable as rows rather than flattening it', () => {
    render(<InputSummary resolvedInput={{ grid: [[1, 0], [0, 1]] }} />);
    expect(screen.getByText('[1, 0] [0, 1]')).toBeInTheDocument();
  });

  it('renders nothing when the trace carried no resolved input', () => {
    // An offline or legacy trace has none. The editor is still reachable from the bottom
    // bar, which is keyed off the problem's inputSpec rather than off this echo.
    const { container } = render(<InputSummary resolvedInput={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing for an empty input map', () => {
    const { container } = render(<InputSummary resolvedInput={{}} />);
    expect(container).toBeEmptyDOMElement();
  });
});
