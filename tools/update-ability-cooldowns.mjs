// Rebuilds the ability list bundled with the mod:
//   src/main/resources/assets/phantomaddons/ability_cooldowns.json
//
// Every weapon, armor and pet ability that has a cooldown in the NotEnoughUpdates item repo is written out with the
// item it belongs to, the cooldown, how long it stays active (when the description says so) and how the mod can tell
// it was used. Abilities the mod has no way of noticing are left out:
//   use        a right or left click with the item in hand (its tooltip says RIGHT CLICK or LEFT CLICK)
//   chat       the ability saves your life and says so in chat ("Your Bonzo's Mask saved your life!")
//   color      the armor set changes colour when it fires (Reaper armor turns red)
//
// Run it with:  node tools/update-ability-cooldowns.mjs <path to a NotEnoughUpdates-REPO checkout>
// (a checkout or an extracted https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO archive)

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const repo = process.argv[2];
if (!repo || !fs.existsSync(path.join(repo, "items"))) {
  console.error("usage: node tools/update-ability-cooldowns.mjs <NotEnoughUpdates-REPO folder>");
  process.exit(1);
}
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const out = path.join(root, "src", "main", "resources", "assets", "phantomaddons", "ability_cooldowns.json");

const strip = (s) => s.replace(/§./g, "");
const RARITY = /^(?:VERY SPECIAL|SPECIAL|COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SUPREME|ULTIMATE|ADMIN)\s+/;
const HEADER = /^(Ability|Spirit Ability|Item Ability|Full Set Bonus|Piece Bonus|Tier Bonus):\s*(.+)$/;
const TRIGGER = /\s+(?:RIGHT CLICK|LEFT CLICK|SNEAK|SHIFT|RIGHT|LEFT)(?:\s.*)?$/;

const ARMOR = /(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS|CLOAK)$/;
const WEAPON = /(?:SWORD|BOW|LONGSWORD|WAND|AXE|GAUNTLET|KATANA|DAGGER|SCYTHE|STAFF|FISHING WEAPON|ABILITY SCROLL)$/;
const ITEM = /^DUNGEON ITEM$/;

// pet perks that announce themselves in chat: "<pet>|<perk>" -> the line, and how long it protects you
const PET_CHAT = {
  "Phoenix|Rekindle": { chat: "Your Phoenix Pet saved you from certain death!", active: 4 },
};
// armor sets that change colour when their ability fires
const COLOR_SETS = /^REAPER_(?:CHESTPLATE|LEGGINGS|BOOTS)$/;
// what the item repo has out of date
const FIXES = {
  "armor|clownin_around": { cooldown: 180 },
  // the veil burns for 5s; the item repo says 1s cooldown and the live tooltip has no cooldown line at all
  "weapon|fire_veil": { active: 5 },
  // Ragnarock channels for 3s ("CASTING IN 3s" on the action bar), then "CASTING" appears and the 10s buff starts;
  // taking damage first shows "CANCELLED". The active time only begins at "CASTING".
  "weapon|ragnarock": { active: 10, activeStart: "CASTING(?! IN)", activeCancel: "CANCELLED" },
};

function classify(internal, lastLine) {
  if (/;\d+$/.test(internal) && /PET$/.test(lastLine)) return "PET";
  const kind = lastLine.replace(RARITY, "").trim();
  if (ARMOR.test(kind)) return "ARMOR";
  if (WEAPON.test(kind)) return "WEAPON";
  if (ITEM.test(kind)) return "ITEM";
  return null;
}

function seconds(text) {
  const m = text.match(/^([\d.,]+)\s*(minutes?|min|m)?(?=s|\b|$)/i);
  if (!m) return 0;
  const n = parseFloat(m[1].replace(",", "."));
  return m[2] ? n * 60 : n;
}

function activeSeconds(description, cooldown) {
  let found = 0;
  for (const m of description.matchAll(/(?:for|Lasts)\s+(\d+(?:\.\d+)?)\s*(?:s|sec|seconds?)\b/gi)) found = parseFloat(m[1]);
  return found > 0 && found < cooldown ? found : 0;
}

// "Reaper Boots", "Reaper Chestplate", "Reaper Leggings" -> "Reaper": the words every piece starts with
function setName(names) {
  const words = names.map((n) => n.split(/\s+/));
  const common = [];
  for (let i = 0; ; i++) {
    const w = words[0][i];
    if (!w || !words.every((x) => x[i] === w)) break;
    common.push(w);
  }
  return common.join(" ");
}

function title(internal) {
  return internal.split(";")[0].toLowerCase().split("_").map((w) => w[0].toUpperCase() + w.slice(1)).join(" ");
}

const found = [];
for (const file of fs.readdirSync(path.join(repo, "items"))) {
  const item = JSON.parse(fs.readFileSync(path.join(repo, "items", file), "utf8"));
  const raw = item.lore || [];
  const lore = raw.map(strip);
  const last = [...lore].reverse().find((l) => l.trim()) || "";
  const type = classify(item.internalname, last);
  if (!type) continue;
  const itemName = type === "PET" ? title(item.internalname) : strip(item.displayname || "").replace(/[✪⚚-]/g, "").trim();
  if (!itemName) continue;
  const icon = type === "PET" ? "minecraft:bone" : item.itemid;

  if (type === "PET") {
    // pet perks: a gold title line, then its description, which says "(5s cooldown)" or "(Lasts 12s, 60s cooldown)"
    for (let i = 0; i < raw.length; i++) {
      const cd = lore[i].match(/(\d+(?:\.\d+)?)\s*s(?:ec|econds?)? cooldown/i);
      if (!cd) continue;
      let t = i;
      while (t > 0 && !/^§6[^§]+$/.test(raw[t])) t--;
      const name = strip(raw[t]).trim();
      if (!name || t === i) continue;
      const cooldown = parseFloat(cd[1]);
      const lasts = lore.slice(t, i + 1).join(" ").match(/Lasts\s+(\d+(?:\.\d+)?)s/i);
      const said = PET_CHAT[itemName + "|" + name];
      if (!said) continue; // nothing the mod can see
      found.push({ type, name, item: item.internalname, itemName, icon, cooldown, active: said.active ?? (lasts ? parseFloat(lasts[1]) : 0), detect: "chat", chat: said.chat });
    }
    continue;
  }

  let name = null, trigger = "", desc = [];
  const flush = (cooldown) => {
    if (!name || !(cooldown > 0)) return;
    const text = desc.join(" ");
    let detect = null;
    let chat = null;
    if (/Instead of dying/i.test(text)) {
      detect = "chat";
      chat = `Your (?:.+ )?${itemName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")} saved your life!`;
    } else if (type === "ARMOR" && COLOR_SETS.test(item.internalname)) detect = "color";
    else if (type !== "ARMOR" && /(?:RIGHT|LEFT) CLICK/.test(trigger)) detect = "use";
    if (!detect) return; // nothing the mod can see
    found.push({ type, name, item: item.internalname, itemName, icon, cooldown, active: activeSeconds(text, cooldown), detect, chat });
  };
  for (let i = 0; i < lore.length; i++) {
    const line = lore[i].trim();
    const header = line.match(HEADER);
    if (header) {
      name = header[2].replace(TRIGGER, "").replace(/\s*\(\d+\/\d+\)\s*$/, "").trim();
      trigger = header[2].slice(header[2].replace(TRIGGER, "").length);
      if (!/^[A-Za-z]/.test(name)) name = null;
      desc = [];
      continue;
    }
    if (!name) continue;
    const cd = line.match(/^Cooldown:\s*(.+)$/i);
    if (cd) { flush(seconds(cd[1])); name = null; continue; }
    if (line) desc.push(line);
  }
}

// one entry per ability (name + type): the same ability on several items is listed once, with the most common cooldown
const groups = new Map();
for (const f of found) {
  const key = f.type + "|" + f.name.toLowerCase();
  if (!groups.has(key)) groups.set(key, []);
  groups.get(key).push(f);
}
const abilities = [];
for (const [key, list] of groups) {
  const counts = new Map();
  for (const f of list) counts.set(f.cooldown, (counts.get(f.cooldown) || 0) + 1);
  const cooldown = [...counts.entries()].sort((a, b) => b[1] - a[1] || a[0] - b[0])[0][0];
  const same = list.filter((f) => f.cooldown === cooldown);
  // the plain version of an item (no star in its id) represents the ability
  same.sort((a, b) => (a.item.startsWith("STARRED_") ? 1 : 0) - (b.item.startsWith("STARRED_") ? 1 : 0) || a.item.localeCompare(b.item));
  const first = same[0];
  const items = [...new Set(list.map((f) => f.itemName))];
  const id = key.replace(/[^a-z0-9|]+/gi, "_").toLowerCase();
  const entry = {
    id,
    name: first.name,
    type: first.type,
    item: first.item,
    // an armor ability shared by several pieces is shown by the set's name, not by whichever piece came first
    itemName: first.type === "ARMOR" && items.length > 1 && setName(items) ? setName(items) : first.itemName,
    icon: first.icon,
    cooldown: FIXES[id]?.cooldown ?? cooldown,
    detect: first.detect,
  };
  const active = FIXES[id]?.active ?? first.active;
  if (active) entry.active = active;
  if (FIXES[id]?.activeStart) { entry.activeStart = FIXES[id].activeStart; entry.activeCancel = FIXES[id].activeCancel; }
  if (items.length > 1) entry.items = items.slice(0, 8);
  const chats = [...new Set(list.map((f) => f.chat).filter(Boolean))];
  if (chats.length) entry.chat = chats.length === 1 ? chats[0] : chats.join("|");
  abilities.push(entry);
}
const order = { WEAPON: 0, ARMOR: 1, PET: 2, ITEM: 3 };
abilities.sort((a, b) => order[a.type] - order[b.type] || a.name.localeCompare(b.name));

fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify({ abilities }));
const byType = {};
for (const a of abilities) byType[a.type] = (byType[a.type] || 0) + 1;
const byDetect = {};
for (const a of abilities) byDetect[a.detect] = (byDetect[a.detect] || 0) + 1;
console.log(`wrote ${abilities.length} abilities`, byType, byDetect);
