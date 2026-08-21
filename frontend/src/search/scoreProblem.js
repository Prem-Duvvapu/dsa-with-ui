/**
 * Problem search scoring and ranking engine.
 * Pure module: no React, no external imports, no side effects.
 */

export function normalize(s) {
  return (s ?? '').toLowerCase().trim();
}

export function tokenize(q) {
  return normalize(q).split(/\s+/).filter(Boolean);
}

export function words(field) {
  return normalize(field).split(/[\s\-_/&,()]+/).filter(Boolean);
}

function isSubsequence(token, str) {
  let i = 0;
  let j = 0;
  while (i < token.length && j < str.length) {
    if (token[i] === str[j]) i++;
    j++;
  }
  return i === token.length;
}

const FIELD_WEIGHTS = [
  { key: 'title', weight: 1.0 },
  { key: 'id', weight: 0.6 },
  { key: 'category', weight: 0.35 },
  { key: 'dsType', weight: 0.3 },
  { key: 'difficulty', weight: 0.25 }
];

/**
 * Computes the best match kind value (0 to 1.0) for a single token against a field string.
 *
 * Match kinds & values:
 *   exact:       1.00 (normalize(field) === token)
 *   prefix:      0.85 (normalize(field).startsWith(token))
 *   wordPrefix:  0.75 (any word in words(field) starts with token)
 *   acronym:     0.70 (token.length >= 2 and token === initials of words(field))
 *   contains:    0.50 (normalize(field).includes(token))
 *   subsequence: 0.30 (token.length >= 3 and every char appears in order)
 *   none:        0
 */
function fieldMatchKind(token, fieldStr) {
  const val = normalize(fieldStr);
  if (!val || !token) return 0;

  if (val === token) return 1.00;
  if (val.startsWith(token)) return 0.85;

  const w = words(fieldStr);
  if (w.some(word => word.startsWith(token))) return 0.75;

  if (token.length >= 2 && w.length > 0 && token === w.map(word => word[0]).join('')) {
    return 0.70;
  }

  if (val.includes(token)) return 0.50;

  if (token.length >= 3 && isSubsequence(token, val)) return 0.30;

  return 0;
}

/**
 * Computes token score across all fields for a problem:
 * tokenScore(token) = max over all fields of (fieldWeight × bestMatchKind)
 */
function tokenScore(token, problem) {
  let maxScore = 0;
  for (const { key, weight } of FIELD_WEIGHTS) {
    const kindValue = fieldMatchKind(token, problem?.[key]);
    const s = weight * kindValue;
    if (s > maxScore) {
      maxScore = s;
    }
  }
  return maxScore;
}

/**
 * Scores a problem against a query.
 * Returns 0 if no match (or if any token in query has tokenScore === 0).
 */
export function scoreProblem(query, problem) {
  if (!query || !problem) return 0;
  const tokens = tokenize(query);
  if (tokens.length === 0) return 0;

  let sum = 0;
  for (const token of tokens) {
    const ts = tokenScore(token, problem);
    if (ts === 0) {
      // AND semantics: if ANY token scores 0, entire problem scores 0
      return 0;
    }
    sum += ts;
  }

  const normTitle = normalize(problem.title);
  const normQuery = normalize(query);

  // Exact title bonus
  if (normTitle === normQuery) {
    sum += 0.50;
  }
  // Whole-query prefix bonus
  if (normTitle.startsWith(normQuery)) {
    sum += 0.25;
  }

  return sum;
}

/**
 * Searches and ranks problems against a query.
 * For an empty query, returns the input array unchanged and unsorted.
 * When query is non-empty, filters out 0-score problems and sorts deterministically:
 *   score DESC, then traced DESC (true first), then title ASC (localeCompare)
 */
export function searchProblems(query, problems) {
  if (!query || !normalize(query)) {
    return problems;
  }
  if (!Array.isArray(problems)) {
    return [];
  }

  const scored = [];
  for (const p of problems) {
    const s = scoreProblem(query, p);
    if (s > 0) {
      scored.push({ problem: p, score: s });
    }
  }

  scored.sort((a, b) => {
    if (b.score !== a.score) return b.score - a.score;
    const aTraced = Boolean(a.problem.traced);
    const bTraced = Boolean(b.problem.traced);
    if (aTraced !== bTraced) return bTraced ? 1 : -1;
    const aTitle = a.problem.title || '';
    const bTitle = b.problem.title || '';
    return aTitle.localeCompare(bTitle);
  });

  return scored.map(item => item.problem);
}

/**
 * Returns non-overlapping [start, end) index pairs of every token occurrence in text,
 * case-insensitive, sorted ascending and merged where they overlap.
 * For an empty query, returns [].
 */
export function matchRanges(query, text) {
  if (!query || !text) return [];
  const tokens = tokenize(query);
  if (tokens.length === 0) return [];

  const lowerText = text.toLowerCase();
  const ranges = [];

  for (const token of tokens) {
    let idx = lowerText.indexOf(token);
    while (idx !== -1) {
      ranges.push([idx, idx + token.length]);
      idx = lowerText.indexOf(token, idx + 1);
    }
  }

  if (ranges.length === 0) return [];

  // Sort ascending by start, then end
  ranges.sort((a, b) => a[0] - b[0] || a[1] - b[1]);

  // Merge overlapping / adjacent ranges
  const merged = [ranges[0]];
  for (let i = 1; i < ranges.length; i++) {
    const prev = merged[merged.length - 1];
    const curr = ranges[i];
    if (curr[0] <= prev[1]) {
      prev[1] = Math.max(prev[1], curr[1]);
    } else {
      merged.push(curr);
    }
  }

  return merged;
}
