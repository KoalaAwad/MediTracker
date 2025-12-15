import { useEffect, useState } from "react";
import { Box, Container, Paper, Typography, Alert, Card, CardContent, Chip, Stack } from "@mui/material";
import { prescriptionApi, PrescriptionDto } from "../../api/prescriptionApi";
import Loading from "../../components/ui/Loading";
import { useNavigate } from "react-router-dom";
import { SecondaryButton } from "../../components/ui/StyledButton";
import { useAuthStore } from "../../zustand/authStore";

export default function MyPrescriptionsPage() {
  const [items, setItems] = useState<PrescriptionDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);

  useEffect(() => {
    const run = async () => {
      try {
        if (!token) {
          navigate("/login");
          return;
        }
        const res = await prescriptionApi.listMine(token);
        setItems(res.data);
      } catch (e: any) {
        setError(e.response?.data?.error || "Failed to load prescriptions");
      } finally {
        setLoading(false);
      }
    };
    run();
  }, [navigate, token]);

  // Helper function to format schedule in smart way
  const formatSchedule = (schedule: { dayOfWeek: string; timeOfDay: string }[]) => {
    // Group by time
    const byTime = new Map<string, string[]>();

    schedule.forEach((entry) => {
      const time = entry.timeOfDay;
      if (!byTime.has(time)) {
        byTime.set(time, []);
      }
      byTime.get(time)!.push(entry.dayOfWeek);
    });

    // Format each time group
    const formatted: string[] = [];
    byTime.forEach((days, time) => {
      // Sort days in week order
      const dayOrder = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
      const sortedDays = days.sort((a, b) => dayOrder.indexOf(a) - dayOrder.indexOf(b));

      // Check if it's all 7 days (daily)
      if (sortedDays.length === 7) {
        formatted.push(`Daily at ${time}`);
      } else {
        // Shorten day names
        const shortDays = sortedDays.map(d => d.substring(0, 3)); // MON, TUE, etc.
        formatted.push(`${shortDays.join(", ")} at ${time}`);
      }
    });

    return formatted;
  };

  if (loading) return <Loading label="Loading prescriptions..." />;

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default", py: 4 }}>
      <Container maxWidth="md">
        <Box sx={{ mb: 3, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Typography variant="h4" fontWeight="bold">
            My Prescriptions
          </Typography>
          <SecondaryButton onClick={() => navigate("/dashboard")}>
            Back to Dashboard
          </SecondaryButton>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {items.length === 0 ? (
          <Paper elevation={2} sx={{ p: 4, textAlign: "center" }}>
            <Typography color="text.secondary">
              No prescriptions yet. Add one from the Medicine Database!
            </Typography>
          </Paper>
        ) : (
          <Stack spacing={2}>
            {items.map((prescription) => {
              const scheduleLines = formatSchedule(prescription.schedule);

              return (
                <Card key={prescription.id} elevation={2}>
                  <CardContent>
                    <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", mb: 2 }}>
                      <Box>
                        <Typography variant="h6" fontWeight="600" gutterBottom>
                          {prescription.medicineName}
                        </Typography>
                        <Typography variant="body2" color="text.secondary" gutterBottom>
                          {prescription.dosageAmount} {prescription.dosageUnit}
                        </Typography>
                      </Box>
                      <Chip
                        label={prescription.endDate ? "Active" : "Ongoing"}
                        color={prescription.endDate ? "primary" : "success"}
                        size="small"
                      />
                    </Box>

                    <Box sx={{ mb: 2 }}>
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        <strong>Schedule:</strong>
                      </Typography>
                      {scheduleLines.map((line, idx) => (
                        <Typography key={idx} variant="body1" sx={{ ml: 2 }}>
                          • {line}
                        </Typography>
                      ))}
                    </Box>

                    <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap" }}>
                      <Typography variant="caption" color="text.secondary">
                        <strong>Start:</strong> {prescription.startDate}
                      </Typography>
                      {prescription.endDate && (
                        <Typography variant="caption" color="text.secondary">
                          <strong>End:</strong> {prescription.endDate}
                        </Typography>
                      )}
                      <Typography variant="caption" color="text.secondary">
                        <strong>Timezone:</strong> {prescription.timeZone}
                      </Typography>
                    </Box>
                  </CardContent>
                </Card>
              );
            })}
          </Stack>
        )}
      </Container>
    </Box>
  );
}
