// This package deliberately changes paired search snapshots as well as two sources.
// Unlike compare-goldens.cjs, it must NOT claim that every variables map is unchanged.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { isDeepStrictEqual } = require('util');
const root = path.resolve(__dirname, '../../../..');
const baseline = process.argv[2] || '3a83671';
const output = process.argv[3] || '/tmp/parametric-golden-comparison.json';
const sourceDirectory = 'backend/src/main/resources/solutions/core';
const goldenDirectory = 'backend/src/test/resources/golden';
const git = args => execFileSync('git', args, { cwd: root, encoding: 'utf8' });
const normalize = value => value.replace(/\r\n/g, '\n');
const currentSources = fs.readdirSync(path.join(root, sourceDirectory)).filter(name => name.endsWith('.java'));
const baselineSources = git(['ls-tree', '-r', '--name-only', baseline, '--', sourceDirectory]).trim().split('\n').filter(Boolean);
if (baselineSources.some(file => !currentSources.includes(path.basename(file)))) throw Error('Existing source owner removed');
const sourceIds = currentSources.filter(name => {
  const file = `${sourceDirectory}/${name}`;
  return !baselineSources.includes(file) || normalize(git(['show', `${baseline}:${file}`])) !== normalize(fs.readFileSync(path.join(root, file), 'utf8'));
}).map(name => path.basename(name, '.java')).sort();
const readGolden = id => JSON.parse(fs.readFileSync(path.join(root, goldenDirectory, `${id}.json`), 'utf8'));
const snapshotIds = currentSources.map(name => path.basename(name, '.java')).filter(id =>
  readGolden(id).steps.some(step => step.dsType === 'SearchSpace')).sort();
const expected = [...new Set([...sourceIds, ...snapshotIds])].sort();
const changed = git(['diff', '--name-only', baseline, '--', goldenDirectory]).trim().split('\n').filter(Boolean);
if (!sourceIds.length || !isDeepStrictEqual(changed.map(file => path.basename(file, '.json')).sort(), expected)) {
  throw Error('Changed fixtures must exactly match repaired sources and core SEARCH_SPACE owners');
}
function comparable(trace, id) {
  const copy = structuredClone(trace);
  if (sourceIds.includes(id)) {
    delete copy.code;
    delete copy.anchors;
    for (const step of copy.steps) delete step.activeLine;
  }
  if (snapshotIds.includes(id)) for (const step of copy.steps) {
    delete step.variables.low;
    delete step.variables.high;
    delete step.variables.mid;
  }
  return copy;
}
const rows = changed.map(file => {
  const id = path.basename(file, '.json');
  const before = JSON.parse(git(['show', `${baseline}:${file}`]));
  const after = readGolden(id);
  const unchangedOutsideScope = isDeepStrictEqual(comparable(before, id), comparable(after, id));
  let pairedSnapshots = true;
  let unchangedExistingBounds = true;
  if (snapshotIds.includes(id)) for (const [index, step] of after.steps.entries()) {
    const vars = step.variables;
    pairedSnapshots &&= ['low', 'high'].every(key => typeof vars[key] === 'string' && vars[key].trim() !== '' && Number.isFinite(Number(vars[key])));
    for (const key of ['low', 'high', 'mid']) if (before.steps[index]?.variables?.[key] !== undefined) {
      unchangedExistingBounds &&= before.steps[index].variables[key] === vars[key];
    }
  }
  return { id, stepCount: after.stepCount, unchangedOutsideScope, pairedSnapshots, unchangedExistingBounds,
    sourceChanged: sourceIds.includes(id), boundsChanged: snapshotIds.includes(id) };
});
fs.writeFileSync(output, JSON.stringify({ baseline, sourceIds, snapshotIds,
  allowedChanges: { sourceOwners: ['code', 'anchors', 'steps[].activeLine'], searchOwners: ['steps[].variables.low', 'steps[].variables.high', 'steps[].variables.mid'] }, rows }, null, 2));
if (rows.some(row => !row.unchangedOutsideScope || !row.pairedSnapshots || !row.unchangedExistingBounds)) {
  throw Error('Golden changed outside declared scope, omitted a pair, or changed an existing bound');
}
console.log(`PASS ${rows.length} fixtures: intentional source/paired-bound changes only; existing bounds, results, narration and structures unchanged`);
