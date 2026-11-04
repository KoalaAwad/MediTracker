import { useEffect, useState } from "react";
import { Alert, Box, Button, Paper, Typography } from "@mui/material";
import { useAuthStore } from "../../zustand/authStore";
import { disableReminders, enableReminders, reminderState, ReminderState, sendTestReminder } from "../../lib/push";

/** Turn dose reminders on or off for the device you're using. */
export default function RemindersCard() {
  const token = useAuthStore((s) => s.token);
  const [state, setState] = useState<ReminderState | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  useEffect(() => {
    reminderState().then(setState);
  }, []);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setMessage(null);
    try {
      await action();
    } catch (err: any) {
      setMessage({ type: "error", text: err.response?.data?.error || "Something went wrong. Try again." });
    } finally {
      setBusy(false);
    }
  };

  const turnOn = () =>
    run(async () => {
      const next = await enableReminders(token!);
      setState(next);
      if (next === "on") setMessage({ type: "success", text: "Reminders are on for this device." });
    });

  const turnOff = () =>
    run(async () => {
      await disableReminders(token!);
      setState("off");
    });

  const test = () =>
    run(async () => {
      await sendTestReminder(token!);
      setMessage({ type: "success", text: "Test sent. It should appear in a few seconds." });
    });

  return (
    <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
      <Typography variant="h6" component="h3" sx={{ fontWeight: 600, mb: 1 }}>
        Reminders
      </Typography>

      {state === null && <Typography color="text.secondary">Checking...</Typography>}

      {state === "on" && (
        <>
          <Typography color="text.secondary" sx={{ mb: 2 }}>
            This device gets a notification when a dose is due, and again 30 minutes later if it isn't marked.
            You can tap Taken or Skip right on the notification.
          </Typography>
          <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
            <Button variant="outlined" onClick={test} disabled={busy}>
              Send a test
            </Button>
            <Button onClick={turnOff} disabled={busy}>
              Turn off on this device
            </Button>
          </Box>
        </>
      )}

      {state === "off" && (
        <>
          <Typography color="text.secondary" sx={{ mb: 2 }}>
            Get a notification on this device when a dose is due, even with MediTracker closed.
          </Typography>
          <Button variant="contained" onClick={turnOn} disabled={busy}>
            Turn on reminders
          </Button>
        </>
      )}

      {state === "needs-install" && (
        <Alert severity="info">
          On iPhone and iPad, reminders only work from the Home Screen app. Tap the Share button, choose
          <strong> Add to Home Screen</strong>, then open MediTracker from your Home Screen and turn reminders on
          here.
        </Alert>
      )}

      {state === "denied" && (
        <Alert severity="warning">
          Notifications are blocked for this site. Allow them in your browser's site settings, then reload this
          page.
        </Alert>
      )}

      {state === "unsupported" && (
        <Alert severity="info">This browser doesn't support notifications. Try Chrome, Edge, Firefox or Safari.</Alert>
      )}

      {message && (
        <Alert severity={message.type} sx={{ mt: 2 }} onClose={() => setMessage(null)}>
          {message.text}
        </Alert>
      )}
    </Paper>
  );
}
