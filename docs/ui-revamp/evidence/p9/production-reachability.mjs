import { existsSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, extname, isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const REPO = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');
const SOURCE_EXTENSIONS = new Set(['.js', '.jsx', '.css']);
const slash = value => value.replaceAll('\\', '/');

/** Test roots/fixtures do not make a module reachable by a learner. */
export function isProductionSource(file) {
  file = slash(file);
  return SOURCE_EXTENSIONS.has(extname(file)) && !/\.(?:test|spec)\.[^/]+$/.test(file)
    && !/(?:^|\/)test(?:\/|$)/.test(file) && !/(?:^|\/)setupTests\.js$/.test(file);
}

function sourceFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const file = join(directory, entry.name);
    return entry.isDirectory() ? sourceFiles(file) : isProductionSource(slash(file)) ? [file] : [];
  });
}

/** The actual Vite/Rollup loader handles lazy imports, re-exports and CSS imports. */
export async function inspectProductionReachability(repo = REPO) {
  const frontend = join(repo, 'frontend');
  const require = createRequire(join(frontend, 'package.json'));
  const vitePackage = require.resolve('vite/package.json');
  const { build } = await import(pathToFileURL(join(dirname(vitePackage), 'dist/node/index.js')));
  const localFile = id => {
    if (!isAbsolute(id) || id.includes('\0')) return null;
    const file = id.split('?')[0];
    const name = slash(relative(repo, file));
    return !name.startsWith('../') && existsSync(file) ? name : null;
  };
  let modules = [];
  const built = await build({
    root: frontend, configFile: join(frontend, 'vite.config.js'), logLevel: 'silent',
    build: { write: false, sourcemap: false },
    plugins: [{
      name: 'record-production-reachability',
      generateBundle() {
        modules = [...this.getModuleIds()].flatMap(id => {
          const file = localFile(id);
          if (!file || !file.startsWith('frontend/src/')) return [];
          const info = this.getModuleInfo(id);
          return [{ file, included: info.isIncluded,
            imports: [...new Set(info.importedIds.map(localFile).filter(Boolean))].sort(),
            lazyImports: [...new Set(info.dynamicallyImportedIds.map(localFile).filter(Boolean))].sort() }];
        }).sort((a, b) => a.file.localeCompare(b.file));
      }
    }]
  });
  const loaded = new Set(modules.map(module => module.file));
  const candidates = sourceFiles(join(frontend, 'src')).map(file => slash(relative(repo, file))).sort();
  const outputs = (Array.isArray(built) ? built : [built]).flatMap(bundle => bundle.output);
  return {
    baseCommit: null,
    viteVersion: JSON.parse(readFileSync(vitePackage, 'utf8')).version,
    scope: 'Production-loaded JS/JSX/CSS under frontend/src; test roots and fixtures excluded. No unused-export or CSS-selector claim.',
    sourceFiles: candidates,
    productionModules: modules,
    unreachableFiles: candidates.filter(file => !loaded.has(file)),
    assets: outputs.filter(output => /\.(?:js|css)$/.test(output.fileName)).map(output => output.fileName).sort()
  };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const report = await inspectProductionReachability();
  // Supplying an output is deliberate: ordinary imports/tests never write an artifact.
  const out = process.argv.indexOf('--out');
  if (out >= 0) {
    if (!process.argv[out + 1]) throw Error('--out requires a path');
    report.baseCommit = process.env.AUDIT_BASE_COMMIT ?? null;
    writeFileSync(resolve(process.argv[out + 1]), `${JSON.stringify(report, null, 2)}\n`);
  }
  console.log(JSON.stringify({ sourceFiles: report.sourceFiles.length,
    productionModules: report.productionModules.length,
    unreachableFiles: report.unreachableFiles, assets: report.assets }, null, 2));
}
