// Independent subsequence sets, never the traced prefix recurrence.
const id = 'longest-common-subsequence';
const cases = [{
  id, input: { first: 'abcd', second: 'acd' }, answer: '3', roots: 1,
  stringLabels: { first: 'First string', second: 'Second string' },
  draftStringField: 'first', draftStringValue: 'zzzz',
  defaults: {
    recursion: { first: 'abcd', second: 'acd' },
    memoization: { first: 'abcde', second: 'ace' },
    canonical: { first: 'abcde', second: 'ace' }
  },
  rejected: { first: 'aaaaa', second: 'bbbb' },
  memoExtra: {
    'one-by-eight': { first: 'a', second: 'bbbbbbbb' },
    'eight-by-one': { first: 'aaaaaaaa', second: 'b' },
    'seven-by-eight': { first: 'aaaaaaa', second: 'bbbbbbbb' },
    'eight-by-seven': { first: 'aaaaaaaa', second: 'bbbbbbb' },
    'equal-eight': { first: 'abababab', second: 'abababab' }
  },
  extra: {
    'zero-cache': { first: 'aaaa', second: 'bbbb' },
    repeated: { first: 'abab', second: 'baba' },
    equal: { first: 'abba', second: 'abba' },
    'one-character': { first: 'a', second: 'b' }
  }
}];
function maximum(problemId, form) {
  if (problemId !== id) throw Error(`No authored LCS maximum for ${problemId}`);
  if (!['recursion', 'memoization', 'canonical'].includes(form)) throw Error(`Unknown form ${form}`);
  const n = form === 'recursion' ? 4 : form === 'memoization' ? 8 : 12;
  return { first: 'a'.repeat(n), second: 'b'.repeat(n) };
}
function subsequences(text) {
  const result = new Set(['']);
  for (const character of text) for (const prior of [...result]) result.add(prior + character);
  return result;
}
function oracle(problemId, input) {
  if (problemId !== id) throw Error(`No independent LCS reference for ${problemId}`);
  const first = subsequences(input.first), second = subsequences(input.second);
  let longest = 0;
  for (const candidate of first) if (second.has(candidate)) longest = Math.max(longest, candidate.length);
  return longest;
}
module.exports = { cases, maximum, oracle };
