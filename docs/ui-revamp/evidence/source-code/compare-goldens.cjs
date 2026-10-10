// Prove this source repair did not change any recorded algorithm data or narration.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { isDeepStrictEqual } = require('util');
const root = path.resolve(__dirname, '../../../..');
const baseline = process.argv[2] || '9d0aa10';
const family = process.argv[3] || 'dp';
if (!['dp', 'core'].includes(family)) throw Error('Unknown source resource family');
const output = process.argv[4] || path.join(__dirname, 'golden-comparison.json');
const directory = 'backend/src/test/resources/golden';
const files = execFileSync('git', ['diff', '--name-only', baseline, '--', directory], { cwd: root, encoding: 'utf8' })
  .trim().split('\n').filter(Boolean);
const ids = fs.readdirSync(path.join(root, `backend/src/main/resources/solutions/${family}`))
  .filter(name => name.endsWith('.java')).map(name => name.slice(0, -5)).sort();
const sourceDirectory = `backend/src/main/resources/solutions/${family}`;
const baselineSources = execFileSync('git', ['ls-tree', '-r', '--name-only', baseline, '--', sourceDirectory],
  { cwd: root, encoding: 'utf8' }).trim().split('\n').filter(Boolean);
const currentSources = ids.map(id => `${sourceDirectory}/${id}.java`);
if (baselineSources.some(file => !currentSources.includes(file))) throw Error('Source repair cannot remove an existing owner');
const normalized = source => source.replace(/\r\n/g, '\n');
// New resources may still be untracked: git diff alone cannot discover them.
const repairedIds = currentSources.filter(file => !baselineSources.includes(file) ||
  normalized(execFileSync('git', ['show', `${baseline}:${file}`], { cwd: root, encoding: 'utf8' })) !==
    normalized(fs.readFileSync(path.join(root, file), 'utf8'))).map(file => path.basename(file, '.java')).sort();
if (!repairedIds.length || !isDeepStrictEqual(files.map(file => path.basename(file, '.json')).sort(), repairedIds)) {
  throw Error('Changed goldens must exactly match source resources repaired since the baseline');
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
fs.writeFileSync(output, JSON.stringify({
  baseline, family, completeFamily: ids, repairedIds,
  allowedChanges: ['code', 'anchors', 'steps[].activeLine'], rows
}, null, 2));
if (rows.some(row => !row.unchangedData)) throw Error('A golden changed non-source data');
console.log(`PASS ${rows.length} goldens: only source, anchors and highlighted lines changed`);
