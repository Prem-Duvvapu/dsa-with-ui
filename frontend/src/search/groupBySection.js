/**
 * Partition a visible row list into its curriculum sections, for display only.
 *
 * `problems` was always a flat wall - 431 items, or however many a category narrows it
 * to - with nothing marking where one topic's progression ends and the next begins. Every
 * catalogue entry already carries `striverSheetSection`, and the catalogue already serves
 * problems in curriculum order (`ProblemCatalog` merges providers into one
 * insertion-ordered map), so grouping is purely a rendering concern: the underlying list
 * and its order are untouched, which is what keeps keyboard navigation (`activeIndex` into
 * the same flat `visible` array) working without any change on that side.
 *
 * @param {Array} visible   the rows actually being rendered - typically capped at 50
 * @param {Array} fullScope the same filtering as `visible` (category, runnable-only) but
 *                          WITHOUT the render cap, so a section's true size and watched
 *                          count are not understated just because most of it did not make
 *                          the visible slice
 * @param {Object} progress the `{ [id]: { watched, starred } }` map
 * @returns {Array<{section: string|null, totalInSection: number, watchedInSection: number, items: Array}>}
 *   in first-appearance order within `visible`. A section that recurs non-contiguously
 *   merges into its first occurrence rather than opening a second header for the same
 *   name. Items with no section share one `section: null` group rather than one each.
 */
export function groupBySection(visible, fullScope, progress = {}) {
  const stats = new Map();
  for (const p of fullScope) {
    const key = p.striverSheetSection || null;
    const entry = stats.get(key) || { total: 0, watched: 0 };
    entry.total += 1;
    if (progress[p.id]?.watched) entry.watched += 1;
    stats.set(key, entry);
  }

  const order = [];
  const buckets = new Map();
  for (const p of visible) {
    const key = p.striverSheetSection || null;
    if (!buckets.has(key)) {
      buckets.set(key, []);
      order.push(key);
    }
    buckets.get(key).push(p);
  }

  return order.map((key) => ({
    section: key,
    totalInSection: stats.get(key)?.total ?? buckets.get(key).length,
    watchedInSection: stats.get(key)?.watched ?? 0,
    items: buckets.get(key)
  }));
}
