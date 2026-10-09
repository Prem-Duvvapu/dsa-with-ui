/** Record the bundle the browser will actually load, not a filesystem build assumption. */
module.exports = async function servedBuildIdentity(base) {
  const response = await fetch(base);
  if (!response.ok) throw Error(`Evidence frontend HTTP ${response.status}`);
  const html = await response.text();
  const origin = new URL(base).origin;
  const assets = [...new Set([...html.matchAll(/(?:src|href)=["']([^"']+)["']/g)]
    .map(match => new URL(match[1], base)).filter(url => url.origin === origin
      && url.pathname.startsWith('/assets/') && /\.(js|css)$/.test(url.pathname))
    .map(url => url.pathname))].sort();
  if (!assets.some(asset => asset.endsWith('.js')) || !assets.some(asset => asset.endsWith('.css')))
    throw Error('Expected a served production JS/CSS bundle for this evidence');
  return { assets };
};
