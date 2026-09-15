import { describe, expect, it } from 'vitest';
import { groupBySection } from './groupBySection';

const p = (id, section) => ({ id, striverSheetSection: section });

describe('groupBySection', () => {
  it('groups by section in first-appearance order, preserving item order within a group', () => {
    const visible = [p('a', 'DP - Basic DP'), p('b', 'DP - Basic DP'), p('c', 'DP - Grids')];
    const groups = groupBySection(visible, visible, {});

    expect(groups.map((g) => g.section)).toEqual(['DP - Basic DP', 'DP - Grids']);
    expect(groups[0].items.map((i) => i.id)).toEqual(['a', 'b']);
    expect(groups[1].items.map((i) => i.id)).toEqual(['c']);
  });

  it('counts section totals and watched from the full scope, not just the visible slice', () => {
    // A section can hold more members than fit in a capped 50-row visible list; the header
    // must say "4 of 7", not "2 of 2" just because only two happened to make the cut.
    const fullScope = [
      p('a', 'DP - Basic DP'), p('b', 'DP - Basic DP'), p('c', 'DP - Basic DP'),
      p('d', 'DP - Basic DP'), p('e', 'DP - Basic DP'), p('f', 'DP - Basic DP'), p('g', 'DP - Basic DP')
    ];
    const visible = [fullScope[0], fullScope[1]];
    const progress = { a: { watched: true }, c: { watched: true }, e: { watched: true }, g: { watched: true } };

    const groups = groupBySection(visible, fullScope, progress);

    expect(groups[0]).toMatchObject({ section: 'DP - Basic DP', totalInSection: 7, watchedInSection: 4 });
    expect(groups[0].items).toHaveLength(2);
  });

  it('buckets sectionless items together without a header, rather than one bucket each', () => {
    const visible = [p('a', null), p('b', undefined), p('c', 'DP - Grids')];
    const groups = groupBySection(visible, visible, {});

    expect(groups[0].section).toBeNull();
    expect(groups[0].items.map((i) => i.id)).toEqual(['a', 'b']);
    expect(groups[1].section).toBe('DP - Grids');
  });

  it('merges a section that reappears non-contiguously, at its first position', () => {
    const visible = [p('a', 'X'), p('b', 'Y'), p('c', 'X')];
    const groups = groupBySection(visible, visible, {});

    expect(groups.map((g) => g.section)).toEqual(['X', 'Y']);
    expect(groups[0].items.map((i) => i.id)).toEqual(['a', 'c']);
  });

  it('returns nothing for an empty list', () => {
    expect(groupBySection([], [], {})).toEqual([]);
  });
});
