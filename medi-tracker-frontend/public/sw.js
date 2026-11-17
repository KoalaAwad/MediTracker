// MediTracker service worker: shows dose reminders and handles their Taken / Skip buttons.
// Kept as plain JS in public/ so it's served at /sw.js and controls the whole site.

self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (event) => event.waitUntil(self.clients.claim()));

self.addEventListener("push", (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch {
    data = { title: "MediTracker", body: event.data ? event.data.text() : "" };
  }
  const isDose = Boolean(data.doseId);
  const title = data.title || "MediTracker";
  event.waitUntil(
    self.registration
      .showNotification(title, {
        body: data.body || "",
        tag: data.tag,
        // a follow-up replaces the first reminder but should still buzz
        renotify: Boolean(data.tag),
        requireInteraction: isDose,
        icon: "/icon-192.png",
        badge: "/badge-96.png",
        data,
        actions: isDose
          ? [
              { action: "taken", title: "Taken" },
              { action: "skip", title: "Skip" },
            ]
          : [],
      })
      // a browser that rejects one of the options above still gets a plain reminder
      .catch(() => self.registration.showNotification(title, { body: data.body || "", data }))
  );
});

self.addEventListener("notificationclick", (event) => {
  const data = event.notification.data || {};
  event.notification.close();

  if ((event.action === "taken" || event.action === "skip") && data.doseId) {
    event.waitUntil(
      fetch("/api/doses/quick", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          doseId: data.doseId,
          token: data.token,
          status: event.action === "taken" ? "TAKEN" : "SKIPPED",
        }),
      }).then((res) => {
        if (!res.ok) {
          return self.registration.showNotification("Couldn't log that dose", {
            body: "Open MediTracker to mark it.",
            icon: "/icon-192.png",
            data: { url: "/dashboard" },
          });
        }
        // tell open tabs to refresh today's list
        return self.clients.matchAll({ type: "window" }).then((tabs) =>
          tabs.forEach((tab) => tab.postMessage({ type: "dose-logged" }))
        );
      })
    );
    return;
  }

  const url = data.url || "/dashboard";
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((tabs) => {
      const existing = tabs.find((t) => new URL(t.url).origin === self.location.origin);
      if (existing) {
        existing.postMessage({ type: "dose-logged" });
        return existing.focus();
      }
      return self.clients.openWindow(url);
    })
  );
});
