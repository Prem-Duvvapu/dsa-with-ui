import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import QueueCanvas from './QueueCanvas';

describe('QueueCanvas', () => {
  it('shows an explicit empty state when the step carries no queue', () => {
    render(<QueueCanvas step={{ queueOrStackState: [] }} />);
    expect(screen.getByText('empty')).toBeInTheDocument();
  });

  it('renders every entry and tags only the front of the queue', () => {
    render(<QueueCanvas step={{ queueOrStackState: ['1:4', '3:6'] }} title="Priority queue" />);

    expect(screen.getByLabelText('Priority queue')).toBeInTheDocument();
    expect(screen.getByText('1:4')).toBeInTheDocument();
    expect(screen.getByText('3:6')).toBeInTheDocument();
    expect(screen.getByText('front')).toBeInTheDocument();
    expect(screen.getAllByText('front')).toHaveLength(1);
  });

  it('does not throw when the step itself is missing', () => {
    render(<QueueCanvas step={undefined} />);
    expect(screen.getByText('empty')).toBeInTheDocument();
  });
});
