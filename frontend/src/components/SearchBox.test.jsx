import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import React from 'react';
import '@testing-library/jest-dom';
import SearchBox from './SearchBox';

describe('SearchBox component', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  const mockProblems = [
    { id: 'two-sum', title: 'Two Sum', category: 'Arrays', difficulty: 'Easy', traced: true },
    { id: 'two-sum-ii', title: 'Two Sum II', category: 'Arrays', difficulty: 'Medium', traced: false },
    { id: '3sum', title: '3Sum', category: 'Arrays', difficulty: 'Medium', traced: false },
    { id: 'bfs-graph', title: 'BFS Traversal of Graph', category: 'Graph BFS/DFS', difficulty: 'Easy', traced: true },
    { id: 'dfs-graph', title: 'DFS Traversal of Graph', category: 'Graph BFS/DFS', difficulty: 'Easy', traced: false }
  ];

  // Test 16: Typing filters the rendered list
  it('filters the rendered list when typing a query', () => {
    render(
      <SearchBox
        problems={mockProblems}
        onSelectProblem={() => {}}
      />
    );

    const input = screen.getByRole('combobox');
    expect(screen.getByText('Two Sum')).toBeInTheDocument();
    expect(screen.getByText('BFS Traversal of Graph')).toBeInTheDocument();

    fireEvent.change(input, { target: { value: 'bfs' } });

    expect(document.getElementById('problem-opt-bfs-graph')).toBeInTheDocument();
    expect(document.getElementById('problem-opt-two-sum')).toBeNull();
  });

  // Test 17: ArrowDown then Enter calls onSelectProblem with the top-ranked id
  it('calls onSelectProblem with top-ranked id when pressing ArrowDown then Enter', () => {
    const onSelect = vi.fn();
    render(
      <SearchBox
        problems={mockProblems}
        onSelectProblem={onSelect}
      />
    );

    const input = screen.getByRole('combobox');
    // Type 'two sum' -> Two Sum is top-ranked
    fireEvent.change(input, { target: { value: 'two sum' } });

    fireEvent.keyDown(input, { key: 'ArrowDown' });
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith('two-sum');
  });

  // Test 18: Enter with no prior arrow key opens the first result
  it('opens the first result when Enter is pressed without prior arrow keys', () => {
    const onSelect = vi.fn();
    render(
      <SearchBox
        problems={mockProblems}
        onSelectProblem={onSelect}
      />
    );

    const input = screen.getByRole('combobox');
    fireEvent.change(input, { target: { value: 'dfs' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith('dfs-graph');
  });

  // Test 19: Escape clears a non-empty query and leaves focus on the input
  it('clears a non-empty query and leaves focus on input when pressing Escape', () => {
    render(
      <SearchBox
        problems={mockProblems}
        onSelectProblem={() => {}}
      />
    );

    const input = screen.getByRole('combobox');
    fireEvent.change(input, { target: { value: 'kadane' } });
    expect(input.value).toBe('kadane');

    fireEvent.keyDown(input, { key: 'Escape' });
    expect(input.value).toBe('');
  });

  // Test 20: ArrowDown wraps from the last option to the first
  it('wraps ArrowDown from the last option to the first', () => {
    const subset = [
      { id: 'prob-1', title: 'Problem One', category: 'Arrays', difficulty: 'Easy' },
      { id: 'prob-2', title: 'Problem Two', category: 'Arrays', difficulty: 'Easy' }
    ];

    render(
      <SearchBox
        problems={subset}
        onSelectProblem={() => {}}
      />
    );

    const input = screen.getByRole('combobox');

    // ArrowDown from -1 -> index 0 (prob-1)
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    expect(input).toHaveAttribute('aria-activedescendant', 'problem-opt-prob-1');

    // ArrowDown from index 0 -> index 1 (prob-2)
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    expect(input).toHaveAttribute('aria-activedescendant', 'problem-opt-prob-2');

    // ArrowDown from index 1 -> wraps to index 0 (prob-1)
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    expect(input).toHaveAttribute('aria-activedescendant', 'problem-opt-prob-1');
  });

  // Test 21: aria-activedescendant matches the highlighted row's id
  it('updates aria-activedescendant to match highlighted row id', () => {
    render(
      <SearchBox
        problems={mockProblems}
        onSelectProblem={() => {}}
      />
    );

    const input = screen.getByRole('combobox');
    expect(input).not.toHaveAttribute('aria-activedescendant');

    fireEvent.keyDown(input, { key: 'ArrowDown' });
    expect(input).toHaveAttribute('aria-activedescendant', 'problem-opt-two-sum');

    const opt = document.getElementById('problem-opt-two-sum');
    expect(opt).toHaveAttribute('aria-selected', 'true');
  });

  // Test 22: Zero results renders empty state and "Search all N matches" clears category while preserving query
  it('renders empty state on 0 matches and "Search all N matches" button clears category while preserving query', () => {
    const onSelectCategory = vi.fn();
    render(
      <SearchBox
        problems={mockProblems}
        activeCategory="Graph BFS/DFS"
        onSelectCategory={onSelectCategory}
        onSelectProblem={() => {}}
      />
    );

    const input = screen.getByRole('combobox');
    // Search 'two sum' which is in Arrays, not in Graph BFS/DFS
    fireEvent.change(input, { target: { value: 'two sum' } });

    expect(screen.getByText('No algorithm matches \u201ctwo sum\u201d.')).toBeInTheDocument();

    const searchAllBtn = screen.getByRole('button', { name: /Search all 2 matches/i });
    expect(searchAllBtn).toBeInTheDocument();

    fireEvent.click(searchAllBtn);
    expect(onSelectCategory).toHaveBeenCalledWith(null);
    expect(input.value).toBe('two sum');
  });

  // Test 23: the runnable signal appears only on traced problems.
  // It is the row's left rail (.sb-row-runnable), not a text badge — a badge
  // ate horizontal space and pushed titles into ellipsis.
  it('marks only traced problems as runnable', () => {
    render(
      <SearchBox
        problems={mockProblems}
        onSelectProblem={() => {}}
      />
    );

    // two-sum is traced: true, two-sum-ii is traced: false
    const optTwoSum = document.getElementById('problem-opt-two-sum');
    const optTwoSumII = document.getElementById('problem-opt-two-sum-ii');

    expect(optTwoSum.className).toContain('sb-row-runnable');
    expect(optTwoSumII.className).not.toContain('sb-row-runnable');
  });

  // Test 24: The "Runnable only" toggle is absent when no problem carries a traced field
  it('does not render the runnable filter when no problem has a traced field', () => {
    const untracedProblems = [
      { id: 'p1', title: 'Problem 1', category: 'Arrays', difficulty: 'Easy' },
      { id: 'p2', title: 'Problem 2', category: 'Arrays', difficulty: 'Easy' }
    ];

    const { rerender } = render(
      <SearchBox
        problems={untracedProblems}
        onSelectProblem={() => {}}
      />
    );

    expect(screen.queryByRole('button', { name: /runnable/i })).toBeNull();

    // Rerender with traced problems -> toggle appears
    rerender(
      <SearchBox
        problems={mockProblems}
        onSelectProblem={() => {}}
      />
    );

    expect(screen.getByRole('button', { name: /runnable/i })).toBeInTheDocument();
  });

  // --- Review additions -------------------------------------------------

  // The zero-result copy must describe the actual cause. With no query typed,
  // "No algorithm matches """ is nonsense; the cause is the Runnable filter.
  it('does not blame a nonexistent query when the runnable filter empties the list', () => {
    const problems = [
      { id: 'alpha', title: 'Alpha', category: 'Arrays', difficulty: 'Easy', traced: false },
      { id: 'beta', title: 'Beta', category: 'Graph BFS/DFS', difficulty: 'Easy', traced: true }
    ];
    render(
      <SearchBox problems={problems} activeCategory="Arrays" onSelectProblem={() => {}} />
    );

    fireEvent.click(screen.getByRole('button', { name: /runnable/i }));

    expect(screen.queryByText(/No algorithm matches ""/)).toBeNull();
    expect(screen.getByText('Nothing in Arrays is runnable yet.')).toBeInTheDocument();
  });

  // aria-describedby must always resolve. The hint element is swapped out for the
  // clear button once a query exists, which left the attribute dangling.
  it('keeps aria-describedby pointing at an element that exists', () => {
    render(<SearchBox problems={mockProblems} onSelectProblem={() => {}} />);
    const input = screen.getByRole('combobox');

    fireEvent.change(input, { target: { value: 'two' } });

    const describedBy = input.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy)).not.toBeNull();
  });

  // Ids are interpolated into a DOM lookup. A dot or slash in an id turns a
  // querySelector call into a SyntaxError and takes the sidebar down.
  it('navigates without throwing when a problem id contains CSS-selector characters', () => {
    const problems = [
      { id: 'a.b/c', title: 'Weird Identifier', category: 'Arrays', difficulty: 'Easy' }
    ];
    render(<SearchBox problems={problems} onSelectProblem={() => {}} />);
    const input = screen.getByRole('combobox');

    fireEvent.change(input, { target: { value: 'weird' } });
    expect(() => fireEvent.keyDown(input, { key: 'ArrowDown' })).not.toThrow();
  });
});

describe('SearchBox: curriculum sections', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  const sectioned = [
    { id: 'frog-jump', title: 'Frog Jump', category: 'DP', striverSheetSection: 'DP - Basic DP', traced: true },
    { id: 'max-sum', title: 'Max Sum Non Adjacent', category: 'DP', striverSheetSection: 'DP - Basic DP', traced: true },
    { id: 'ninjas-training', title: "Ninja's Training", category: 'DP', striverSheetSection: 'DP - Grids', traced: true }
  ];

  it('groups the browse list under a section header, showing watched progress for the WHOLE section', () => {
    render(
      <SearchBox
        problems={sectioned}
        progress={{ 'frog-jump': { watched: true } }}
        onSelectProblem={() => {}}
      />
    );

    expect(screen.getByText('DP - Basic DP')).toBeInTheDocument();
    expect(screen.getByText('1/2 watched')).toBeInTheDocument();
    expect(screen.getByText('DP - Grids')).toBeInTheDocument();
    expect(screen.getByText('0/1 watched')).toBeInTheDocument();
  });

  it('keeps search results flat and relevance-ordered - no section headers while searching', () => {
    render(<SearchBox problems={sectioned} onSelectProblem={() => {}} />);
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'jump' } });

    expect(screen.queryByText('DP - Basic DP')).not.toBeInTheDocument();
    // Not a plain getByText: a matched query splits the title across a <mark>, e.g.
    // "Frog " + <mark>Jump</mark>, so the exact string is not one text node.
    expect(screen.getByText((_, el) => el?.className === 'sb-title' && el.textContent === 'Frog Jump')).toBeInTheDocument();
  });

  it('falls through to the flat list when nothing carries a section', () => {
    // Every one of the 13 pre-existing tests above uses fixtures with no
    // striverSheetSection at all, and none of them may see a behaviour change.
    const noSections = [{ id: 'two-sum', title: 'Two Sum', category: 'Arrays', traced: true }];
    render(<SearchBox problems={noSections} onSelectProblem={() => {}} />);
    expect(screen.queryByRole('presentation')).not.toBeInTheDocument();
    expect(screen.getByText('Two Sum')).toBeInTheDocument();
  });

  it('still opens a problem by click when the list is grouped', () => {
    const onSelectProblem = vi.fn();
    render(<SearchBox problems={sectioned} onSelectProblem={onSelectProblem} />);
    fireEvent.click(screen.getByText('Frog Jump'));
    expect(onSelectProblem).toHaveBeenCalledWith('frog-jump');
  });

  it('keyboard navigation still walks the flat row order across a group boundary', () => {
    // The grouping only inserts headers between runs of the SAME flat array - activeIndex
    // must still address rows in that array's order, headers or not.
    render(<SearchBox problems={sectioned} onSelectProblem={() => {}} />);
    const input = screen.getByRole('combobox');
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    expect(input).toHaveAttribute('aria-activedescendant', 'problem-opt-ninjas-training');
  });
});
