import axiosClient from "./axiosClient";

// Browser side of dose reminders: service worker + Push API subscription for this device.

export type ReminderState =
  | "unsupported" // browser has no Push API
  | "needs-install" // iPhone/iPad: push only works from the Home Screen app
  | "denied" // user blocked notifications for the site
  | "off"
  | "on";

const auth = (token: string) => ({ headers: { Authorization: `Bearer ${token}` } });

function isIos() {
  return /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
}

function isStandalone() {
  return window.matchMedia("(display-mode: standalone)").matches || (navigator as any).standalone === true;
}

export function registerServiceWorker() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("/sw.js").catch((err) => console.warn("Service worker failed", err));
  }
}

async function currentSubscription() {
  const reg = await navigator.serviceWorker.ready;
  return reg.pushManager.getSubscription();
}

export async function reminderState(): Promise<ReminderState> {
  if (isIos() && !isStandalone()) return "needs-install";
  if (!("serviceWorker" in navigator) || !("PushManager" in window) || !("Notification" in window)) return "unsupported";
  if (Notification.permission === "denied") return "denied";
  if (Notification.permission !== "granted") return "off";
  return (await currentSubscription()) ? "on" : "off";
}

// the server wants the key as bytes
function keyBytes(base64url: string) {
  const padded = (base64url + "===".slice((base64url.length + 3) % 4)).replace(/-/g, "+").replace(/_/g, "/");
  return Uint8Array.from(atob(padded), (c) => c.charCodeAt(0));
}

/** Asks for permission (must run from a click) and registers this device. */
export async function enableReminders(token: string): Promise<ReminderState> {
  const permission = await Notification.requestPermission();
  if (permission !== "granted") return permission === "denied" ? "denied" : "off";

  const reg = await navigator.serviceWorker.ready;
  let sub = await reg.pushManager.getSubscription();
  if (!sub) {
    const { data } = await axiosClient.get<{ publicKey: string }>("/push/public-key", auth(token));
    sub = await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: keyBytes(data.publicKey) });
  }
  await axiosClient.post("/push/subscriptions", sub.toJSON(), auth(token));
  return "on";
}

export async function disableReminders(token: string) {
  const sub = await currentSubscription();
  if (!sub) return;
  await axiosClient.delete("/push/subscriptions", { ...auth(token), data: { endpoint: sub.endpoint } });
  await sub.unsubscribe();
}

/**
 * Re-sends this device's subscription after login, so reminders follow whoever is signed in
 * on a shared device, and a subscription the browser renewed gets saved.
 */
export async function syncSubscription(token: string) {
  try {
    if ((await reminderState()) !== "on") return;
    const sub = await currentSubscription();
    if (sub) await axiosClient.post("/push/subscriptions", sub.toJSON(), auth(token));
  } catch {
    // reminders settings will show the real state
  }
}
