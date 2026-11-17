// 12-hour clock helpers; the app never shows 24-hour times

/** "08:00" or "20:30:00" -> "8:00 AM" / "8:30 PM" */
export function to12h(hhmm: string): string {
  const [h, m] = hhmm.split(":").map(Number);
  if (Number.isNaN(h) || Number.isNaN(m)) return hhmm;
  const suffix = h < 12 ? "AM" : "PM";
  const hour = h % 12 === 0 ? 12 : h % 12;
  return `${hour}:${String(m).padStart(2, "0")} ${suffix}`;
}

/** "20 minutes ago", "1 hour 40 minutes ago", "5 hours ago" */
export function timeAgo(iso: string, now: Date = new Date()): string {
  const minutes = Math.max(0, Math.floor((now.getTime() - new Date(iso).getTime()) / 60000));
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes} minute${minutes === 1 ? "" : "s"} ago`;
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  const h = `${hours} hour${hours === 1 ? "" : "s"}`;
  // minutes matter for the first couple of hours, after that round to hours
  if (hours < 3 && rest > 0) return `${h} ${rest} minute${rest === 1 ? "" : "s"} ago`;
  return `${h} ago`;
}
