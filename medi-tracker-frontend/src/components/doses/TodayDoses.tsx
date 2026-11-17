import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Alert, Box, Button, Typography, useTheme } from "@mui/material";
import { doseApi, TodayDose } from "../../api/doseApi";
import { useAuthStore } from "../../zustand/authStore";
import { timeAgo } from "../../lib/time";

// the server sorts now -> upcoming -> past; logged doses keep their place by time
function phaseOf(d: TodayDose): "now" | "upcoming" | "past" {
  if (d.status === "NOW") return "now";
  if (d.status === "UPCOMING") return "upcoming";
  if (d.status === "PAST") return "past";
  const minutes = (Date.now() - new Date(d.scheduledAt).getTime()) / 60000;
  if (minutes < -30) return "upcoming";
  if (minutes <= 60) return "now";
  return "past";
}

const GROUPS = [
  { title: "Now", phase: "now" },
  { title: "Upcoming", phase: "upcoming" },
  { title: "Past", phase: "past" },
] as const;

const STATE_LABEL: Record<TodayDose["status"], string> = {
  NOW: "Now",
  UPCOMING: "Upcoming",
  PAST: "Past",
  TAKEN: "Taken",
  SKIPPED: "Skipped",
};

/** Home: today's doses. Teal = take it now, red = missed, everything else neutral. */
export default function TodayDoses() {
  const token = useAuthStore((s) => s.token);
  const navigate = useNavigate();
  const { calm } = useTheme();
  const [doses, setDoses] = useState<TodayDose[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [, setTick] = useState(0);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      const res = await doseApi.today(token);
      setDoses(res.data);
      setError(null);
    } catch {
      setError("Couldn't load today's doses");
    }
  }, [token]);

  useEffect(() => {
    load();
    // statuses and "x minutes ago" move on by themselves
    const timer = setInterval(() => {
      load();
      setTick((t) => t + 1);
    }, 60_000);
    // a Taken/Skip tapped on a notification
    const onMessage = (e: MessageEvent) => e.data?.type === "dose-logged" && load();
    navigator.serviceWorker?.addEventListener("message", onMessage);
    return () => {
      clearInterval(timer);
      navigator.serviceWorker?.removeEventListener("message", onMessage);
    };
  }, [load]);

  const mark = async (dose: TodayDose, status: "TAKEN" | "SKIPPED" | "PENDING") => {
    if (!token) return;
    const key = dose.prescriptionId + dose.scheduledAt;
    setBusy(key);
    try {
      const res = await doseApi.log(token, dose.prescriptionId, dose.scheduledAt, status);
      setDoses((list) => list?.map((d) => (d.prescriptionId + d.scheduledAt === key ? { ...d, status: res.data.status } : d)) ?? null);
    } catch (err: any) {
      setError(err.response?.data?.error || "Couldn't save that");
    } finally {
      setBusy(null);
    }
  };

  const total = doses?.length ?? 0;
  const taken = doses?.filter((d) => d.status === "TAKEN").length ?? 0;
  const today = new Date().toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "long" });

  const rowColors = (d: TodayDose) => {
    if (d.status === "NOW") return { bg: calm.accentSoft, edge: calm.accent, thick: true, text: calm.text };
    if (d.status === "PAST") return { bg: calm.dangerSoft, edge: calm.danger, thick: true, text: calm.text };
    if (d.status === "TAKEN" || d.status === "SKIPPED") return { bg: calm.doneBg, edge: calm.line, thick: false, text: calm.muted };
    return { bg: calm.card, edge: calm.line, thick: false, text: calm.text };
  };

  return (
    <Box>
      <Typography variant="h4" component="h1" sx={{ fontSize: { xs: 24, sm: 28 } }}>
        Home
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 0.5, fontSize: 14 }}>
        {today}
        {total > 0 && ` · ${taken} of ${total} taken`}
      </Typography>
      {total > 0 && (
        <Box sx={{ height: 6, bgcolor: "divider", mt: 2, mb: 1 }}>
          <Box sx={{ height: "100%", width: `${(taken / total) * 100}%`, bgcolor: "primary.main", transition: "width .3s" }} />
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ my: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {doses === null && !error && (
        <Typography color="text.secondary" sx={{ mt: 3 }}>
          Loading...
        </Typography>
      )}

      {doses?.length === 0 && (
        <Box sx={{ mt: 3 }}>
          <Typography color="text.secondary" sx={{ mb: 1 }}>
            Nothing scheduled for today.
          </Typography>
          <Button variant="contained" onClick={() => navigate("/medicine")}>
            + Add prescription
          </Button>
        </Box>
      )}

      {doses &&
        GROUPS.map((group) => {
          const items = doses.filter((d) => phaseOf(d) === group.phase);
          if (items.length === 0) return null;
          return (
            <Box key={group.title} component="section">
              <Typography
                component="h2"
                sx={{ fontSize: 12, textTransform: "uppercase", letterSpacing: "0.08em", color: "text.secondary", fontWeight: 600, mt: 3, mb: 1 }}
              >
                {group.title}
              </Typography>
              {items.map((dose) => {
                const key = dose.prescriptionId + dose.scheduledAt;
                const logged = dose.status === "TAKEN" || dose.status === "SKIPPED";
                const c = rowColors(dose);
                return (
                  <Box
                    key={key}
                    sx={{
                      display: "flex",
                      alignItems: "center",
                      flexWrap: { xs: "wrap", sm: "nowrap" },
                      gap: { xs: 1, sm: 2 },
                      py: 1.75,
                      pr: 2,
                      // keep text aligned whether the left edge is thick or thin
                      pl: c.thick ? 2 : 2.375,
                      mb: 0.75,
                      bgcolor: c.bg,
                      color: c.text,
                      border: `1px solid ${c.edge}`,
                      borderLeft: `${c.thick ? 4 : 1}px solid ${c.edge}`,
                    }}
                  >
                    <Typography sx={{ fontWeight: 700, fontVariantNumeric: "tabular-nums", width: 76, flexShrink: 0, color: "inherit" }}>
                      {dose.localTime}
                    </Typography>
                    <Box sx={{ flex: 1, minWidth: 150 }}>
                      <Typography sx={{ fontWeight: 600, color: "inherit" }}>{dose.medicineName}</Typography>
                      <Typography sx={{ fontSize: 13, opacity: 0.8, color: "inherit" }}>{dose.dosage}</Typography>
                      {dose.notes && (
                        <Typography sx={{ fontSize: 13, opacity: 0.8, mt: 0.5, whiteSpace: "pre-line", color: "inherit" }}>
                          {dose.notes}
                        </Typography>
                      )}
                    </Box>
                    <Typography component="div" sx={{ fontSize: 13, fontWeight: 600, textAlign: { sm: "right" }, minWidth: 80, color: "inherit" }}>
                      {STATE_LABEL[dose.status]}
                      {dose.status === "PAST" && (
                        <Box component="span" sx={{ display: "block", fontWeight: 400, opacity: 0.85 }}>
                          {timeAgo(dose.scheduledAt)}
                        </Box>
                      )}
                    </Typography>
                    <Box sx={{ display: "flex", gap: 0.75, justifyContent: "flex-end", width: { sm: 150 }, flexShrink: 0 }}>
                      {logged ? (
                        <Button size="small" variant="outlined" color="inherit" disabled={busy === key} onClick={() => mark(dose, "PENDING")}>
                          Undo
                        </Button>
                      ) : (
                        <>
                          <Button size="small" variant="contained" disabled={busy === key} onClick={() => mark(dose, "TAKEN")}>
                            Taken
                          </Button>
                          <Button size="small" variant="outlined" color="inherit" disabled={busy === key} onClick={() => mark(dose, "SKIPPED")}>
                            Skip
                          </Button>
                        </>
                      )}
                    </Box>
                  </Box>
                );
              })}
            </Box>
          );
        })}
    </Box>
  );
}
