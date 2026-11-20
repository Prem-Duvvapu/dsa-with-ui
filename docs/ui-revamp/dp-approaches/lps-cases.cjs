// The oracle tests palindromic subsequences directly; it never solves LCS/reversal.
const id = 'longest-palindromic-subsequence';
const cases = [{
  id, input: { text: 'bbbab' }, answer: '4', roots: 1,
  stringLabels: { text: 'Text' }, draftStringField: 'text', draftStringValue: 'zzzzz',
  defaults: { recursion: { text: 'bbbab' }, memoization: { text: 'bbbab' }, canonical: { text: 'bbbab' } },
  rejected: { text: 'abcdef' },
  memoExtra: {
    'dense-known': { text: 'abbbbbbc' },
    'all-distinct-eight': { text: 'abcdefgh' },
    'all-same-eight': { text: 'aaaaaaaa' }
  },
  extra: {
    'one-character': { text: 'a' },
    'all-same': { text: 'aaaaa' },
    'all-distinct': { text: 'abcde' },
    palindrome: { text: 'abcba' },
    repeated: { text: 'aabab' }
  }
}];
function maximum(problemId, form) {
  if (problemId !== id) throw Error(`No authored LPS maximum for ${problemId}`);
  if (!['recursion', 'memoization', 'canonical'].includes(form)) throw Error(`Unknown form ${form}`);
  return { text: form === 'recursion' ? 'abcde' : form === 'memoization' ? 'abbccdde' : 'abcdefghijkl' };
}
function oracle(problemId, input) {
  if (problemId !== id) throw Error(`No independent LPS reference for ${problemId}`);
  const text = input.text; let longest = 0;
  for (let mask = 0; mask < 2 ** text.length; mask++) {
    let candidate = '';
    for (let index = 0; index < text.length; index++) if (mask & (1 << index)) candidate += text[index];
    if (candidate.length <= longest) continue;
    let palindrome = true;
    for (let left = 0, right = candidate.length - 1; left < right; left++, right--)
      if (candidate[left] !== candidate[right]) { palindrome = false; break; }
    if (palindrome) longest = candidate.length;
  }
  return longest;
}
module.exports = { cases, maximum, oracle };
