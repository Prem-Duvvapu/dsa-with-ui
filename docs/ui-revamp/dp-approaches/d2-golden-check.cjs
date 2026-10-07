const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { isDeepStrictEqual } = require('util');
const root = path.resolve(__dirname, '../../..');
const baseline = process.argv[2] || 'b111b72';
const file = 'backend/src/test/resources/golden/climbing-stairs.json';
const changed = execFileSync('git', ['diff', '--name-only', baseline, '--', 'backend/src/test/resources/golden'],
  { cwd: root, encoding: 'utf8' }).trim().split('\n').filter(Boolean);
if (!isDeepStrictEqual(changed, [file])) throw Error('Only the canonical Climbing Stairs golden may change');
const before = JSON.parse(execFileSync('git', ['show', `${baseline}:${file}`], { cwd: root, encoding: 'utf8' }));
const after = JSON.parse(fs.readFileSync(path.join(root, file), 'utf8'));
function data(trace) {
  const copy = structuredClone(trace);
  delete copy.code;
  delete copy.anchors;
  for (const step of copy.steps) delete step.activeLine;
  return copy;
}
if (!isDeepStrictEqual(data(before), data(after))) throw Error('Canonical algorithm data or narration changed');
fs.writeFileSync(path.join(__dirname, 'd2-golden-results.json'), JSON.stringify({
  baseline, file, unchangedAlgorithmData: true, allowedChanges: ['code', 'anchors', 'steps[].activeLine'],
  oldSourceLines: before.code.split('\n').length, newSourceLines: after.code.split('\n').length
}, null, 2) + '\n');
console.log('PASS: only source, anchors and highlighted lines changed in the single canonical golden');
