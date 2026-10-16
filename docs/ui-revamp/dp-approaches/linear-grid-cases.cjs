// Authored cases and independent references, never expected values copied from traces.
const cases = [
  { id: 'frog-jump-k-distance', input: { heights: [10,30,40,20,50], k: 3 }, answer: '40' },
  { id: 'max-sum-non-adjacent', input: { nums: [2,1,4,9,4,1,8] }, answer: '19' },
  { id: 'grid-unique-paths', input: { rows: 3, cols: 3 }, answer: '6' }
];
function maximum(id, form) {
  if (id === 'grid-unique-paths') return { rows: form === 'recursion' ? 4 : 8, cols: form === 'recursion' ? 4 : 8 };
  const n = id === 'frog-jump-k-distance' ? form === 'recursion' ? 8 : 16 : form === 'recursion' ? 10 : 20;
  const values = Array.from({ length: n }, (_, i) => i % 2 ? 999 : 0);
  return id === 'frog-jump-k-distance' ? { heights: values, k: 8 } : { nums: values };
}
function oracle(id, input) {
  if (id === 'grid-unique-paths') {
    let answer = 1;
    for (let i = 1; i < input.rows; i++) answer = answer * (input.cols - 1 + i) / i;
    return Math.round(answer);
  }
  if (id === 'max-sum-non-adjacent') {
    let best = 0;
    for (let mask = 0; mask < 2 ** input.nums.length; mask++) {
      if ((mask & (mask << 1)) !== 0) continue;
      let sum = 0;
      input.nums.forEach((n, i) => { if (mask & (1 << i)) sum += n; });
      best = Math.max(best, sum);
    }
    return best;
  }
  function walk(at, cost) {
    if (at === input.heights.length - 1) return cost;
    let best = Infinity;
    for (let d = 1; d <= input.k && at + d < input.heights.length; d++) {
      best = Math.min(best, walk(at + d, cost + Math.abs(input.heights[at + d] - input.heights[at])));
    }
    return best;
  }
  return walk(0, 0);
}
module.exports = { cases, maximum, oracle };
