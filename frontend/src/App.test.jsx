import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import React from 'react';
import '@testing-library/jest-dom';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import Controls from './components/Controls';

describe('Frontend Component Tests', () => {
  it('renders Header with title and Striver sheet link', () => {
    render(<Header totalProblems={11} completedCount={11} />);
    expect(screen.getByText('DSA with UI')).toBeInTheDocument();
    expect(screen.getByText("Striver's A2Z Sheet")).toBeInTheDocument();
  });

  it('renders Sidebar with list of graph problems', () => {
    const mockProblems = [
      { id: 'bfs-traversal', title: 'BFS Traversal of Graph', category: 'Graph BFS/DFS', difficulty: 'Easy', dsType: 'Queue' },
      { id: 'dfs-traversal', title: 'DFS Traversal of Graph', category: 'Graph BFS/DFS', difficulty: 'Easy', dsType: 'Stack' }
    ];
    render(
      <Sidebar
        problems={mockProblems}
        activeProblemId="bfs-traversal"
        activeCategory="Graph BFS/DFS"
        onSelectCategory={() => {}}
        onSelectProblem={() => {}}
      />
    );
    expect(screen.getByText('BFS Traversal of Graph')).toBeInTheDocument();
    expect(screen.getByText('DFS Traversal of Graph')).toBeInTheDocument();
  });

  it('renders Controls player buttons', () => {
    render(
      <Controls
        isPlaying={false}
        currentStepIndex={0}
        totalSteps={5}
        speed={800}
        onPlayPause={() => {}}
        onStepNext={() => {}}
        onStepPrev={() => {}}
        onReset={() => {}}
        onSpeedChange={() => {}}
        stepDescription="Test Step"
      />
    );
    expect(screen.getByText('Play')).toBeInTheDocument();
    expect(screen.getByText('Reset')).toBeInTheDocument();
    expect(screen.getByText('Test Step')).toBeInTheDocument();
  });
});
