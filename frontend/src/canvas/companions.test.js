import { describe, expect, it } from 'vitest';
import { getCompanions } from './companions';
import QueueCanvas from '../components/QueueCanvas';
import { GridCompanion } from '../components/GridCanvas';

describe('getCompanions', () => {
  it('adds a queue companion when ANY step in the run carries a queue, even if the current one is empty', () => {
    // bfs-traversal's own trace: the queue is empty on the init step and non-empty on the
    // next. Presence must come from the whole run, not this one (empty) step, or the pane
    // would pop in and out on nearly every click through the animation.
    const currentStep = { queueOrStackState: [] };
    const allSteps = [currentStep, { queueOrStackState: ['0'] }];
    const companions = getCompanions('Graph', currentStep, allSteps);

    expect(companions).toHaveLength(1);
    expect(companions[0].Component).toBe(QueueCanvas);
    expect(companions[0].props.step).toBe(currentStep);
  });

  it('shows the live (possibly empty) content once presence is established', () => {
    const doneStep = { queueOrStackState: [] };
    const allSteps = [{ queueOrStackState: ['0'] }, doneStep];
    const companions = getCompanions('Graph', doneStep, allSteps);

    expect(companions[0].props.step).toBe(doneStep);
    expect(companions[0].props.step.queueOrStackState).toEqual([]);
  });

  it('adds nothing when no step in the whole run ever carries a queue', () => {
    const allSteps = [{ graphNodes: [] }, { graphNodes: [] }];
    expect(getCompanions('Graph', allSteps[0], allSteps)).toEqual([]);
  });

  it('adds nothing for a non-Graph, non-Matrix hero, even with a populated queueOrStackState', () => {
    // A dsType whose OWN hero already draws queueOrStackState (once one exists) must not
    // also get a companion for the same field — that would draw the same structure twice.
    const allSteps = [{ queueOrStackState: ['0'] }];
    expect(getCompanions('Array', allSteps[0], allSteps)).toEqual([]);
  });

  it('adds a queue companion for a Matrix hero too', () => {
    // rotting-oranges is Matrix-hero (the grid is the point) but its multi-source BFS
    // still runs a real queue, which was previously computed and never shown anywhere.
    const currentStep = { queueOrStackState: ['0,0', '0,1'] };
    const allSteps = [currentStep];
    const companions = getCompanions('Matrix', currentStep, allSteps);

    expect(companions).toHaveLength(1);
    expect(companions[0].Component).toBe(QueueCanvas);
  });

  it('handles a missing step or trace without throwing', () => {
    expect(getCompanions('Graph', undefined, undefined)).toEqual([]);
    expect(getCompanions('Graph', undefined, [])).toEqual([]);
  });

  it('adds a grid companion for a Stack hero when any step carries a grid', () => {
    // maximum-rectangles-binary-matrix is Stack-hero (the row's histogram is the active
    // structure) but emits the LeetCode 85 board via .grid(matrix) on some steps too -
    // previously invisible, since Stack's own hero only reads queueOrStackState.
    const currentStep = { queueOrStackState: ['0'], gridState: [[1, 0], [1, 1]] };
    const allSteps = [currentStep];
    const companions = getCompanions('Stack', currentStep, allSteps);

    expect(companions).toHaveLength(1);
    expect(companions[0].Component).toBe(GridCompanion);
    expect(companions[0].props.step.gridState).toEqual([[1, 0], [1, 1]]);
  });

  it('carries the last-known grid forward on a step that did not re-emit it', () => {
    // maximum-rectangles-binary-matrix only calls .grid(matrix) on its row-start and
    // row-done steps; every step in between (the histogram's own push/pop narration)
    // carries no gridState at all. That is "unchanged", not "the matrix went empty" -
    // unlike queueOrStackState, which genuinely IS empty between elements (see the
    // queue tests above) - so the companion must keep showing the matrix, not blank out.
    const rowStart = { gridState: [[1, 0], [1, 1]] };
    const pushCol = { queueOrStackState: ['0'] }; // no gridState of its own
    const allSteps = [rowStart, pushCol];

    const companions = getCompanions('Stack', pushCol, allSteps);

    expect(companions[0].props.step.gridState).toEqual([[1, 0], [1, 1]]);
  });

  it('adds nothing for a Stack hero when no step in the run ever carries a grid', () => {
    const allSteps = [{ queueOrStackState: ['0'] }];
    expect(getCompanions('Stack', allSteps[0], allSteps)).toEqual([]);
  });

  it('adds nothing for a Matrix hero with a populated grid, even though it carries one', () => {
    // A dsType whose OWN hero already draws gridState (Matrix) must not also get a
    // companion for the same field - that would draw the same structure twice.
    const allSteps = [{ gridState: [[1]] }];
    expect(getCompanions('Matrix', allSteps[0], allSteps)).toEqual([]);
  });

  it('can add both a queue and a grid companion at once for whichever heroes need them', () => {
    const currentStep = { queueOrStackState: ['a'], gridState: [[0]] };
    const allSteps = [currentStep];

    expect(getCompanions('Graph', currentStep, allSteps).map((c) => c.key)).toEqual(['queue']);
    expect(getCompanions('Stack', currentStep, allSteps).map((c) => c.key)).toEqual(['grid']);
  });
});
