import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi } from 'vitest';
import WelcomeGuide from './WelcomeGuide';

describe('WelcomeGuide', () => {
  it('names the three regions a newcomer has to find', () => {
    render(<WelcomeGuide open onDismiss={() => {}} onShowShortcuts={() => {}} />);
    expect(screen.getByRole('dialog', { name: /Welcome to the DSA Visualizer/i })).toBeInTheDocument();
    expect(screen.getByText('Pick a problem')).toBeInTheDocument();
    expect(screen.getByText('Watch it run')).toBeInTheDocument();
    expect(screen.getByText('Follow the code')).toBeInTheDocument();
  });

  it('renders nothing when closed', () => {
    const { container } = render(<WelcomeGuide open={false} onDismiss={() => {}} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('dismisses from the primary action', () => {
    const onDismiss = vi.fn();
    render(<WelcomeGuide open onDismiss={onDismiss} onShowShortcuts={() => {}} />);
    fireEvent.click(screen.getByRole('button', { name: /Start exploring/i }));
    expect(onDismiss).toHaveBeenCalled();
  });

  it('hands off to the shortcut list', () => {
    const onShowShortcuts = vi.fn();
    render(<WelcomeGuide open onDismiss={() => {}} onShowShortcuts={onShowShortcuts} />);
    fireEvent.click(screen.getByRole('button', { name: /See all shortcuts/i }));
    expect(onShowShortcuts).toHaveBeenCalled();
  });

  it('focuses its primary action so the keyboard lands inside the dialog', () => {
    render(<WelcomeGuide open onDismiss={() => {}} onShowShortcuts={() => {}} />);
    expect(document.activeElement).toBe(screen.getByRole('button', { name: /Start exploring/i }));
  });
});
