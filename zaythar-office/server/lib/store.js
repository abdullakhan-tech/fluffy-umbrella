// Har agent ka apna folder: data/<agent>/memory.json (yaaddasht) aur db.json (apna database).
// Optional cloud sync: Firebase Firestore par automatic async sync jab credentials hon.
import fs from 'node:fs';
import { pathToFileURL } from 'node:url';
import { syncAgentToFirebase } from './firebase.js';

const root = process.env.DATA_DIR ? pathToFileURL(process.env.DATA_DIR + '/') : new URL('../data/', import.meta.url);

export function agentStore(name) {
  const dir = new URL(name + '/', root);
  fs.mkdirSync(dir, { recursive: true });
  const read = (n, d) => { try { return JSON.parse(fs.readFileSync(new URL(n, dir), 'utf8')); } catch { return d; } };
  const write = (n, v) => fs.writeFileSync(new URL(n, dir), JSON.stringify(v));
  return {
    memory: () => read('memory.json', { facts: [], lessons: [], xp: 0 }),
    saveMemory: m => {
      write('memory.json', m);
      syncAgentToFirebase(name, m, null).catch(() => {});
    },
    db: () => read('db.json', { tasks: [] }),
    saveDb: v => {
      write('db.json', v);
      syncAgentToFirebase(name, null, v).catch(() => {});
    },
  };
}
