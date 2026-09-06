import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import StackCanvas from './StackCanvas';

describe('StackCanvas', () => {
  it('shows an explicit empty state when the step carries no stack', () => {
    render(<StackCanvas step={{ queueOrStackState: [] }} />);
    expect(screen.getByText('empty')).toBeInTheDocument();
  });

  it('renders every entry and tags only the top of the stack', () => {
    render(<StackCanvas step={{ queueOrStackState: ['4', '2', '9'] }} title="Stack" />);

    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
    expect(screen.getByText('top')).toBeInTheDocument();
    expect(screen.getAllByText('top')).toHaveLength(1);
  });

  it('reads currentStep when step is not supplied', () => {
    render(<StackCanvas currentStep={{ queueOrStackState: ['7'] }} />);
    expect(screen.getByText('7')).toBeInTheDocument();
  });

  it('does not throw when both step and currentStep are missing', () => {
    render(<StackCanvas />);
    expect(screen.getByText('empty')).toBeInTheDocument();
  });
});
