// Checks that every translation file (src/locale/messages.<lang>.json) matches
// the texts extracted from the code (src/locale/messages.json, created by
// `ng extract-i18n`): same keys and same placeholders ({$name}).
// Usage: npm run i18n   (extracts and then checks)
import { readdirSync, readFileSync } from 'node:fs';

const dir = new URL('../src/locale/', import.meta.url);
const read = (file) => JSON.parse(readFileSync(new URL(file, dir), 'utf8')).translations;
const placeholders = (text) => [...text.matchAll(/\{\$[A-Za-z_]+\}/g)].map((m) => m[0]).sort().join(' ');

const source = read('messages.json');
let problems = 0;
for (const file of readdirSync(dir).filter((f) => /^messages\.[\w-]+\.json$/.test(f))) {
  const target = read(file);
  for (const [key, text] of Object.entries(source)) {
    if (!(key in target)) {
      console.error(`${file}: missing "${key}" (${text})`);
      problems++;
    } else if (placeholders(text) !== placeholders(target[key])) {
      console.error(`${file}: different placeholders in "${key}"`);
      problems++;
    }
  }
  for (const key of Object.keys(target).filter((k) => !(k in source))) {
    console.error(`${file}: "${key}" is no longer used`);
    problems++;
  }
}
if (problems) {
  process.exit(1);
}
console.log('Translations OK');
