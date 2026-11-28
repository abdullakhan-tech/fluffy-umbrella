// Firebase Firestore Integration for Zaythar Office Memory & DB Sync
const {
  FIREBASE_PROJECT_ID = 'zaythar-ai',
  FIREBASE_API_KEY,
  FIREBASE_AUTH_TOKEN
} = process.env;

export const firebaseReady = () => !!(FIREBASE_PROJECT_ID && (FIREBASE_API_KEY || FIREBASE_AUTH_TOKEN));

const firestoreBase = () =>
  `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents`;

export async function syncAgentToFirebase(name, memory, db) {
  if (!firebaseReady()) return false;
  try {
    const url = `${firestoreBase()}/agents/${encodeURIComponent(name)}${FIREBASE_API_KEY ? `?key=${FIREBASE_API_KEY}` : ''}`;
    const fields = {
      facts: { arrayValue: { values: (memory?.facts || []).map(f => ({ stringValue: String(f) })) } },
      lessons: { arrayValue: { values: (memory?.lessons || []).map(l => ({ stringValue: String(l) })) } },
      xp: { integerValue: String(memory?.xp || 0) },
      updatedAt: { timestampValue: new Date().toISOString() }
    };
    if (db?.tasks) {
      fields.taskCount = { integerValue: String(db.tasks.length) };
    }
    const res = await fetch(url, {
      method: 'PATCH',
      signal: AbortSignal.timeout(10000),
      headers: {
        'content-type': 'application/json',
        ...(FIREBASE_AUTH_TOKEN ? { authorization: `Bearer ${FIREBASE_AUTH_TOKEN}` } : {})
      },
      body: JSON.stringify({ fields })
    });
    return res.ok;
  } catch {
    return false;
  }
}

export async function loadAgentFromFirebase(name) {
  if (!firebaseReady()) return null;
  try {
    const url = `${firestoreBase()}/agents/${encodeURIComponent(name)}${FIREBASE_API_KEY ? `?key=${FIREBASE_API_KEY}` : ''}`;
    const res = await fetch(url, {
      signal: AbortSignal.timeout(10000),
      headers: {
        ...(FIREBASE_AUTH_TOKEN ? { authorization: `Bearer ${FIREBASE_AUTH_TOKEN}` } : {})
      }
    });
    if (!res.ok) return null;
    const json = await res.json();
    const fields = json.fields || {};
    return {
      facts: (fields.facts?.arrayValue?.values || []).map(v => v.stringValue),
      lessons: (fields.lessons?.arrayValue?.values || []).map(v => v.stringValue),
      xp: parseInt(fields.xp?.integerValue || '0', 10)
    };
  } catch {
    return null;
  }
}
