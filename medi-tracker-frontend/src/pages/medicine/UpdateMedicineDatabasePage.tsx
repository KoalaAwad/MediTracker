import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Alert, Box, Button, Container, LinearProgress, Paper, Typography } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { medicineApi, SyncStatus } from "../../api/medicineApi";
import { useAuthStore } from "../../zustand/authStore";
import Navbar from "../../components/ui/Navbar";
import { PrimaryButton } from "../../components/ui/StyledButton";

// openFDA currently lists about 115k finished products
const EXPECTED_PRODUCTS = 115000;

export default function UpdateMedicineDatabasePage() {
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);
  const isAdmin = useAuthStore((s) => s.user?.role.includes("ADMIN") ?? false);
  const [status, setStatus] = useState<SyncStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!token) return;
    try {
      const res = await medicineApi.getSyncStatus(token);
      setStatus(res.data);
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not load sync status");
    }
  }, [token]);

  useEffect(() => {
    if (!isAdmin) {
      navigate("/unauthorized");
      return;
    }
    refresh();
  }, [isAdmin, navigate, refresh]);

  // poll while a sync runs
  useEffect(() => {
    if (!status?.running) return;
    const timer = setInterval(refresh, 2000);
    return () => clearInterval(timer);
  }, [status?.running, refresh]);

  const start = async () => {
    if (!token) return;
    setError(null);
    try {
      await medicineApi.startSync(token);
      setStatus((s) => (s ? { ...s, running: true, imported: 0, error: undefined } : s));
      refresh();
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not start the sync");
    }
  };

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <Navbar />
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate("/medicine")} sx={{ mb: 2 }}>
          Back to medicines
        </Button>

        <Paper elevation={2} sx={{ p: 4 }}>
          <Typography variant="h5" fontWeight="bold" gutterBottom>
            Medicine database
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 3 }}>
            Medicines come from the FDA's NDC directory: every prescription and over-the-counter
            product sold in the US, with strength, form and manufacturer. The server downloads it
            itself on first start and every Sunday. Use the button to update now.
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {status && (
            <Box sx={{ mb: 3 }}>
              <Typography sx={{ mb: 1 }}>
                <strong>{status.total.toLocaleString()}</strong> medicines available
              </Typography>
              {status.running ? (
                <>
                  <LinearProgress
                    variant={status.imported > 0 ? "determinate" : "indeterminate"}
                    value={Math.min(100, (status.imported / EXPECTED_PRODUCTS) * 100)}
                    sx={{ mb: 1 }}
                  />
                  <Typography variant="body2" color="text.secondary">
                    {status.imported > 0
                      ? `Importing... ${status.imported.toLocaleString()} products so far`
                      : "Downloading from openFDA..."}
                  </Typography>
                </>
              ) : status.error ? (
                <Alert severity="error">{status.error}</Alert>
              ) : status.finishedAt ? (
                <Alert severity="success">
                  Last updated {new Date(status.finishedAt).toLocaleString()}.
                </Alert>
              ) : null}
            </Box>
          )}

          <PrimaryButton onClick={start} disabled={!status || status.running}>
            {status?.running ? "Updating..." : "Update from openFDA"}
          </PrimaryButton>
        </Paper>
      </Container>
    </Box>
  );
}
