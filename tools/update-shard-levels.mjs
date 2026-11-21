// Rebuilds the attribute shard level data bundled with the mod:
//   src/main/resources/assets/phantomaddons/shard_levels.json          shard code -> [[level I value, level X value, unit], ...]
//   src/main/resources/assets/phantomaddons/shard_levels_LICENSE.txt   SkyShards' licence, which must stay with the data
//
// The numbers come from SkyShards (MIT, https://github.com/Campionnn/SkyShards), whose desc.json lists every shard's
// effect with the value it has at level 1 and at level 10. Shards are keyed by their code ("C1", "R28", ...), the same
// "shardId" NEU's attribute_shards.json uses.
//
// Run it with:  node tools/update-shard-levels.mjs   (when new shards are added to the game)

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const outDir = path.join(root, "src", "main", "resources", "assets", "phantomaddons");
const base = "https://raw.githubusercontent.com/Campionnn/SkyShards/master/";

async function get(file) {
  const res = await fetch(base + file, { headers: { "User-Agent": "PhantomAddons-update-shard-levels" } });
  if (!res.ok) throw new Error(`${file}: HTTP ${res.status}`);
  return res.text();
}

const desc = JSON.parse(await get("src/desc.json"));
const levels = {};
for (const code of Object.keys(desc).sort()) {
  const ranges = [];
  for (const segment of desc[code].description) {
    if (!Array.isArray(segment.range)) continue;
    const [first, last] = segment.range;
    if (first == null || last == null) continue; // not known, so there is nothing to show
    ranges.push([first, last, segment.unit || ""]);
  }
  if (ranges.length) levels[code] = ranges;
}

fs.mkdirSync(outDir, { recursive: true });
fs.writeFileSync(path.join(outDir, "shard_levels.json"), JSON.stringify(levels));
fs.writeFileSync(
  path.join(outDir, "shard_levels_LICENSE.txt"),
  "The attribute shard level values in shard_levels.json come from SkyShards\n" +
    "(https://github.com/Campionnn/SkyShards), used under this licence:\n\n" + (await get("LICENSE")));

console.log(`${Object.keys(levels).length} shards with level values written to ${outDir}`);
