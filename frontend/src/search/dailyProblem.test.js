import { describe, expect, it } from 'vitest';
import { pickDailyProblem } from './dailyProblem';

const PROBLEMS = [{ id: 'a' }, { id: 'b' }, { id: 'c' }, { id: 'd' }, { id: 'e' }];

describe('pickDailyProblem', () => {
  it('is null with no problems', () => {
    expect(pickDailyProblem([], '2026-09-15')).toBeNull();
  });

  it('picks the same problem for the same date, every time it is called', () => {
    const first = pickDailyProblem(PROBLEMS, '2026-09-15');
    const second = pickDailyProblem(PROBLEMS, '2026-09-15');
    expect(first).toBe(second);
    expect(PROBLEMS.map(p => p.id)).toContain(first.id);
  });

  it('usually differs across two different dates', () => {
    // Deterministic, not literally guaranteed different for EVERY pair of dates (a hash can
    // collide) - across a run of consecutive days on a 5-problem list, at least one must
    // differ, or the "pick" is not reading the date at all.
    const picks = new Set();
    for (let day = 1; day <= 10; day += 1) {
      picks.add(pickDailyProblem(PROBLEMS, `2026-09-${String(day).padStart(2, '0')}`).id);
    }
    expect(picks.size).toBeGreaterThan(1);
  });

  it('stays in range for a single-problem list', () => {
    expect(pickDailyProblem([{ id: 'only' }], '2026-01-01').id).toBe('only');
  });
});
