/**
 * bus.js — tiny app-wide event bus so modules stay decoupled.
 * Events used:
 *   'state'          player state snapshot changed
 *   'tracksupdated'  library track list / metadata changed
 *   'settings'       settings changed (detail: {settings, patch})
 *   'navigate'       screen navigation requested (detail: screen id)
 */
export const bus = new EventTarget();

export function emit(type, detail) {
  bus.dispatchEvent(new CustomEvent(type, { detail }));
}

export function on(type, handler) {
  bus.addEventListener(type, (e) => handler(e.detail));
  return () => bus.removeEventListener(type, handler);
}
