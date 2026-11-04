import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Alert, Box, Button, Chip, Paper, Stack, Typography } from "@mui/material";
import { doseApi, DoseStatus, TodayDose } from "../../api/doseApi";
import { useAuthStore } from "../../zustand/authStore";

const STATUS: Record<DoseStatus, { label: string; color: "success" | "default" | "warning" | "error" | "info" }> = {
  TAKEN: { label: "Taken", color: "success" },
  SKIPPED: { label: "Skipped", color: "default" },
  UPCOMING: { label: "Upcoming", color: "info" },
  DUE: { label: "Due now", color: "warning" },
  MISSED: { label: "Missed", color: "error" },
};

/** Today's doses with one-tap Taken / Skip. */
export default function TodayDoses() {
  const token = useAuthStore((s) => s.token);
  const navigate = useNavigate();
  const [doses, setDoses] = useState<TodayDose[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

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
    // statuses move on by themselves (upcoming -> due -> missed)
    const timer = setInterval(load, 60_000);
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

  const done = doses?.filter((d) => d.status === "TAKEN").length ?? 0;

  return (
    <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", mb: 2, gap: 2 }}>
        <Typography variant="h6" component="h3" sx={{ fontWeight: 600 }}>
          Today's doses
        </Typography>
        {doses && doses.length > 0 && (
          <Typography variant="body2" color="text.secondary">
            {done} of {doses.length} taken
          </Typography>
        )}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {doses === null && !error && <Typography color="text.secondary">Loading...</Typography>}

      {doses?.length === 0 && (
        <Box>
          <Typography color="text.secondary" sx={{ mb: 1 }}>
            Nothing scheduled for today.
          </Typography>
          <Button onClick={() => navigate("/medicine")}>Add a prescription</Button>
        </Box>
      )}

      <Stack divider={<Box sx={{ borderTop: 1, borderColor: "divider" }} />}>
        {doses?.map((dose) => {
          const s = STATUS[dose.status];
          const logged = dose.status === "TAKEN" || dose.status === "SKIPPED";
          const key = dose.prescriptionId + dose.scheduledAt;
          return (
            <Box
              key={key}
              sx={{ display: "flex", alignItems: "center", gap: 2, py: 1.5, flexWrap: { xs: "wrap", sm: "nowrap" } }}
            >
              <Typography sx={{ fontWeight: 700, fontVariantNumeric: "tabular-nums", minWidth: 48 }}>
                {dose.localTime}
              </Typography>
              <Box sx={{ flex: 1, minWidth: 160 }}>
                <Typography sx={{ fontWeight: 600 }}>{dose.medicineName}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {dose.dosage}
                </Typography>
                {dose.notes && (
                  <Typography variant="body2" color="text.secondary" sx={{ fontStyle: "italic", whiteSpace: "pre-line" }}>
                    {dose.notes}
                  </Typography>
                )}
              </Box>
              <Chip label={s.label} color={s.color} size="small" variant={logged ? "filled" : "outlined"} />
              <Box sx={{ display: "flex", gap: 1 }}>
                {logged ? (
                  <Button size="small" disabled={busy === key} onClick={() => mark(dose, "PENDING")}>
                    Undo
                  </Button>
                ) : (
                  <>
                    <Button size="small" variant="contained" disabled={busy === key} onClick={() => mark(dose, "TAKEN")}>
                      Taken
                    </Button>
                    <Button size="small" disabled={busy === key} onClick={() => mark(dose, "SKIPPED")}>
                      Skip
                    </Button>
                  </>
                )}
              </Box>
            </Box>
          );
        })}
      </Stack>
    </Paper>
  );
}
