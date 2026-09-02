import { describe, expect, it } from 'vitest';
import { getCompanions } from './companions';
import QueueCanvas from '../components/QueueCanvas';

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

  it('adds nothing for a non-Graph hero, even with a populated queueOrStackState', () => {
    // A dsType whose OWN hero already draws queueOrStackState (once one exists) must not
    // also get a companion for the same field — that would draw the same structure twice.
    const allSteps = [{ queueOrStackState: ['0'] }];
    expect(getCompanions('Array', allSteps[0], allSteps)).toEqual([]);
  });

  it('handles a missing step or trace without throwing', () => {
    expect(getCompanions('Graph', undefined, undefined)).toEqual([]);
    expect(getCompanions('Graph', undefined, [])).toEqual([]);
  });
});
