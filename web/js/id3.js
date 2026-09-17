/**
 * id3.js — Minimal, dependency-free ID3 tag reader.
 *
 * Supports:
 *  - ID3v2.2 (TT2 / TP1 / TAL frames)
 *  - ID3v2.3 / ID3v2.4 (TIT2 / TPE1 / TALB frames)
 *  - ID3v1 fallback (last 128 bytes)
 *
 * Only the text encodings used by the vast majority of files are decoded
 * (ISO-8859-1, UTF-8, UTF-16 with BOM). Everything is defensive: any parse
 * failure simply yields fewer tags, never an exception to the caller.
 */

const ID3V2_MAGIC = 'ID3';

/** Decode a text frame payload given its encoding byte. */
function decodeText(encoding, bytes) {
  try {
    const data = bytes.subarray(1); // strip encoding byte
    switch (encoding) {
      case 0: // ISO-8859-1
        return new TextDecoder('iso-8859-1').decode(data);
      case 1: { // UTF-16 with BOM
        let label = 'utf-16';
        let body = data;
        if (data.length >= 2) {
          if (data[0] === 0xff && data[1] === 0xfe) { label = 'utf-16le'; body = data.subarray(2); }
          else if (data[0] === 0xfe && data[1] === 0xff) { label = 'utf-16be'; body = data.subarray(2); }
        }
        return new TextDecoder(label).decode(body);
      }
      case 2: // UTF-16BE without BOM
        return new TextDecoder('utf-16be').decode(data);
      case 3: // UTF-8
        return new TextDecoder('utf-8').decode(data);
      default:
        return new TextDecoder('iso-8859-1').decode(data);
    }
  } catch {
    return '';
  }
}

/** Strip trailing NUL/space padding that taggers love to add. */
function clean(value) {
  if (!value) return '';
  return value.replace(/\0+$/g, '').replace(/\s+$/g, '').trim();
}

/** Read a big-endian uint32. */
function u32(bytes, off) {
  return ((bytes[off] << 24) | (bytes[off + 1] << 16) | (bytes[off + 2] << 8) | bytes[off + 3]) >>> 0;
}

/** Read a syncsafe uint32 (ID3v2.4 tag & frame sizes). */
function syncsafe(bytes, off) {
  return ((bytes[off] & 0x7f) << 21) | ((bytes[off + 1] & 0x7f) << 14) |
         ((bytes[off + 2] & 0x7f) << 7) | (bytes[off + 3] & 0x7f);
}

const V22_MAP = { TT2: 'title', TP1: 'artist', TAL: 'album' };
const V23_MAP = { TIT2: 'title', TPE1: 'artist', TALB: 'album' };

function parseId3v2(bytes) {
  const tags = {};
  const versionMajor = bytes[3];
  // flags = bytes[5]
  const tagSize = versionMajor === 4 ? syncsafe(bytes, 6) : u32(bytes, 6);
  const end = Math.min(10 + tagSize, bytes.length);
  let off = 10;

  // Skip extended header if present.
  if (bytes[5] & 0x40) {
    if (versionMajor === 4) off += syncsafe(bytes, off);
    else off += u32(bytes, off) + 4;
  }

  const frameMap = versionMajor === 2 ? V22_MAP : V23_MAP;
  const idLen = versionMajor === 2 ? 3 : 4;
  const headerLen = versionMajor === 2 ? 6 : 10;

  while (off + headerLen <= end) {
    // Stop at padding.
    if (bytes[off] === 0) break;

    let id = '';
    for (let i = 0; i < idLen; i++) id += String.fromCharCode(bytes[off + i]);
    if (!/^[A-Z0-9]+$/.test(id)) break;

    let size;
    if (versionMajor === 2) {
      size = (bytes[off + 3] << 16) | (bytes[off + 4] << 8) | bytes[off + 5];
    } else if (versionMajor === 4) {
      size = syncsafe(bytes, off + 4);
    } else {
      size = u32(bytes, off + 4);
    }
    if (size <= 0 || off + headerLen + size > end) break;

    const key = frameMap[id];
    if (key && !tags[key]) {
      const payload = bytes.subarray(off + headerLen, off + headerLen + size);
      const text = clean(decodeText(payload[0] ?? 0, payload));
      if (text) tags[key] = text;
    }
    off += headerLen + size;
  }
  return tags;
}

function parseId3v1(bytes) {
  if (bytes.length < 128) return {};
  const tail = bytes.subarray(bytes.length - 128);
  if (tail[0] !== 0x54 || tail[1] !== 0x41 || tail[2] !== 0x47) return {}; // "TAG"
  const dec = new TextDecoder('iso-8859-1');
  const tags = {};
  const title = clean(dec.decode(tail.subarray(3, 33)));
  const artist = clean(dec.decode(tail.subarray(33, 63)));
  const album = clean(dec.decode(tail.subarray(63, 93)));
  if (title) tags.title = title;
  if (artist) tags.artist = artist;
  if (album) tags.album = album;
  return tags;
}

/**
 * Parse tags from a File/Blob. Reads at most the first 256 KB (for ID3v2)
 * and the last 128 bytes (for ID3v1).
 * @param {Blob} file
 * @returns {Promise<{title?: string, artist?: string, album?: string}>}
 */
export async function parseTags(file) {
  try {
    const head = new Uint8Array(await file.slice(0, 262144).arrayBuffer());
    if (head.length > 10 &&
        head[0] === 0x49 && head[1] === 0x44 && head[2] === 0x33) { // "ID3"
      const tags = parseId3v2(head);
      if (tags.title || tags.artist || tags.album) return tags;
    }
    const tailBuf = new Uint8Array(await file.slice(Math.max(0, file.size - 128)).arrayBuffer());
    const v1 = parseId3v1(tailBuf);
    if (Object.keys(v1).length) return v1;
  } catch {
    // fall through
  }
  return {};
}

/** Guess "Artist - Title" from a file name when no tags exist. */
export function tagsFromFilename(name) {
  const base = name.replace(/\.[^.]+$/, '').replace(/_/g, ' ').trim();
  const m = base.match(/^(.{1,80}?)\s*-\s*(.+)$/);
  if (m) return { artist: m[1].trim(), title: m[2].trim() };
  return { title: base };
}

// Allow Node-based unit tests: `node id3.js --self-test`
if (typeof process !== 'undefined' && process.argv?.[1]?.endsWith('id3.js') && process.argv.includes('--self-test')) {
  runSelfTest();
}

export function runSelfTest() {
  let passed = 0;
  let failed = 0;
  const assert = (cond, msg) => {
    if (cond) { passed++; }
    else { failed++; console.error('FAIL:', msg); }
  };

  // --- ID3v2.3 with UTF-8 title ---
  {
    const enc = new TextEncoder();
    const titlePayload = new Uint8Array([3, ...enc.encode('Hello Wörld')]);
    const frame = new Uint8Array(4 + 4 + 2 + titlePayload.length);
    frame.set(enc.encode('TIT2'), 0);
    const size = titlePayload.length;
    frame[4] = (size >>> 24) & 0xff; frame[5] = (size >>> 16) & 0xff;
    frame[6] = (size >>> 8) & 0xff; frame[7] = size & 0xff;
    frame[8] = 0; frame[9] = 0;
    frame.set(titlePayload, 10);

    const tagSize = frame.length;
    const header = new Uint8Array([0x49, 0x44, 0x33, 3, 0, 0,
      (tagSize >>> 21) & 0x7f, (tagSize >>> 14) & 0x7f, (tagSize >>> 7) & 0x7f, tagSize & 0x7f]);
    const full = new Uint8Array(header.length + frame.length);
    full.set(header, 0); full.set(frame, header.length);
    const tags = parseId3v2(full);
    assert(tags.title === 'Hello Wörld', `v2.3 utf8 title, got ${JSON.stringify(tags)}`);
  }

  // --- ID3v2.4 syncsafe sizes ---
  {
    const enc = new TextEncoder();
    const payload = new Uint8Array([0, ...enc.encode('ABC')]);
    const frame = new Uint8Array(10 + payload.length);
    frame.set(enc.encode('TPE1'), 0);
    const size = payload.length; // 4
    frame[4] = 0; frame[5] = 0; frame[6] = 0; frame[7] = size;
    frame.set(payload, 10);
    const tagSize = frame.length;
    const header = new Uint8Array([0x49, 0x44, 0x33, 4, 0, 0,
      (tagSize >>> 21) & 0x7f, (tagSize >>> 14) & 0x7f, (tagSize >>> 7) & 0x7f, tagSize & 0x7f]);
    const full = new Uint8Array(header.length + frame.length);
    full.set(header, 0); full.set(frame, header.length);
    const tags = parseId3v2(full);
    assert(tags.artist === 'ABC', `v2.4 artist, got ${JSON.stringify(tags)}`);
  }

  // --- ID3v2.2 TT2 frame ---
  {
    const enc = new TextEncoder();
    const payload = new Uint8Array([0, ...enc.encode('Old')]);
    const frame = new Uint8Array(6 + payload.length);
    frame.set(enc.encode('TT2'), 0);
    const size = payload.length;
    frame[3] = (size >>> 16) & 0xff; frame[4] = (size >>> 8) & 0xff; frame[5] = size & 0xff;
    frame.set(payload, 6);
    const tagSize = frame.length;
    const header = new Uint8Array([0x49, 0x44, 0x33, 2, 0, 0,
      (tagSize >>> 21) & 0x7f, (tagSize >>> 14) & 0x7f, (tagSize >>> 7) & 0x7f, tagSize & 0x7f]);
    const full = new Uint8Array(header.length + frame.length);
    full.set(header, 0); full.set(frame, header.length);
    const tags = parseId3v2(full);
    assert(tags.title === 'Old', `v2.2 title, got ${JSON.stringify(tags)}`);
  }

  // --- ID3v1 tail ---
  {
    const buf = new Uint8Array(200);
    const off = buf.length - 128;
    buf.set([0x54, 0x41, 0x47], off);
    buf.set(new TextEncoder().encode('SongA'), off + 3);
    buf.set(new TextEncoder().encode('ArtistB'), off + 33);
    const tags = parseId3v1(buf);
    assert(tags.title === 'SongA' && tags.artist === 'ArtistB', `v1 tags, got ${JSON.stringify(tags)}`);
  }

  // --- filename fallback ---
  {
    const t = tagsFromFilename('Daft Punk - Around the World.mp3');
    assert(t.artist === 'Daft Punk' && t.title === 'Around the World', 'filename parse');
    const t2 = tagsFromFilename('plain_song.wav');
    assert(t2.title === 'plain song' && !t2.artist, 'filename plain');
  }

  console.log(`id3 self-test: ${passed} passed, ${failed} failed`);
  if (typeof process !== 'undefined' && failed > 0) process.exitCode = 1;
}
