// Prove this source repair did not change any recorded algorithm data or narration.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { isDeepStrictEqual } = require('util');
const root = path.resolve(__dirname, '../../../..');
const baseline = process.argv[2] || '9d0aa10';
const directory = 'backend/src/test/resources/golden';
const files = execFileSync('git', ['diff', '--name-only', baseline, '--', directory], { cwd: root, encoding: 'utf8' })
  .trim().split('\n').filter(Boolean);
const ids = fs.readdirSync(path.join(root, 'backend/src/main/resources/solutions/dp'))
  .filter(name => name.endsWith('.java')).map(name => name.slice(0, -5)).sort();
if (!isDeepStrictEqual(files.map(file => path.basename(file, '.json')).sort(), ids)) {
  throw Error('Changed goldens must exactly match repaired source resources');
}
function withoutSource(trace) {
  const copy = structuredClone(trace);
  delete copy.code;
  delete copy.anchors;
  for (const step of copy.steps) delete step.activeLine;
  return copy;
}
const rows = files.map(file => {
  const before = JSON.parse(execFileSync('git', ['show', `${baseline}:${file}`], { cwd: root, encoding: 'utf8' }));
  const after = JSON.parse(fs.readFileSync(path.join(root, file), 'utf8'));
  const unchangedData = isDeepStrictEqual(withoutSource(before), withoutSource(after));
  return { id: path.basename(file, '.json'), unchangedData,
    oldLines: before.code.split('\n').length, newLines: after.code.split('\n').length };
});
fs.writeFileSync(path.join(__dirname, 'golden-comparison.json'), JSON.stringify({
  baseline, allowedChanges: ['code', 'anchors', 'steps[].activeLine'], rows
}, null, 2));
if (rows.some(row => !row.unchangedData)) throw Error('A golden changed non-source data');
console.log(`PASS ${rows.length} goldens: only source, anchors and highlighted lines changed`);
