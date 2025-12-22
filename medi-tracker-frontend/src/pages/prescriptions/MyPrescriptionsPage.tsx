import { useEffect, useState } from "react";
import {
  Box,
  Container,
  Paper,
  Typography,
  Alert,
  Card,
  CardContent,
  Chip,
  Stack,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Divider
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import { prescriptionApi, PrescriptionDto } from "../../api/prescriptionApi";
import Loading from "../../components/ui/Loading";
import Navbar from "../../components/ui/Navbar";
import { useNavigate } from "react-router-dom";
import { PrimaryButton } from "../../components/ui/StyledButton";
import { useAuthStore } from "../../zustand/authStore";

export default function MyPrescriptionsPage() {
  const [items, setItems] = useState<PrescriptionDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [prescriptionToDelete, setPrescriptionToDelete] = useState<PrescriptionDto | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);

  const loadPrescriptions = async () => {
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

  useEffect(() => {
    loadPrescriptions();
  }, [navigate, token]);

  // Helper to check if prescription is expired
  const isExpired = (prescription: PrescriptionDto): boolean => {
    if (!prescription.endDate) return false; // Ongoing prescriptions never expire

    const today = new Date();
    today.setHours(0, 0, 0, 0); // Reset time to midnight for date-only comparison

    const endDate = new Date(prescription.endDate);
    endDate.setHours(0, 0, 0, 0);

    return endDate < today;
  };

  // Separate prescriptions into active and past
  const activePrescriptions = items.filter(p => !isExpired(p));
  const pastPrescriptions = items.filter(p => isExpired(p));

  // Helper function to format schedule in smart way
  const formatSchedule = (schedule: { dayOfWeek: string; timeOfDay: string }[]) => {
    const byTime = new Map<string, string[]>();

    schedule.forEach((entry) => {
      const time = entry.timeOfDay;
      if (!byTime.has(time)) {
        byTime.set(time, []);
      }
      byTime.get(time)!.push(entry.dayOfWeek);
    });

    const formatted: string[] = [];
    byTime.forEach((days, time) => {
      const dayOrder = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
      const sortedDays = days.sort((a, b) => dayOrder.indexOf(a) - dayOrder.indexOf(b));

      if (sortedDays.length === 7) {
        formatted.push(`Daily at ${time}`);
      } else {
        const shortDays = sortedDays.map(d => d.substring(0, 3));
        formatted.push(`${shortDays.join(", ")} at ${time}`);
      }
    });

    return formatted;
  };

  const handleDeleteClick = (prescription: PrescriptionDto) => {
    setPrescriptionToDelete(prescription);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!prescriptionToDelete || !token) return;

    setDeleting(true);
    try {
      await prescriptionApi.delete(token, prescriptionToDelete.id);
      setItems(items.filter(p => p.id !== prescriptionToDelete.id));
      setDeleteDialogOpen(false);
      setPrescriptionToDelete(null);
    } catch (e: any) {
      setError(e.response?.data?.error || "Failed to delete prescription");
    } finally {
      setDeleting(false);
    }
  };

  const handleEditClick = (prescription: PrescriptionDto) => {
    // Navigate to edit page (we'll create this route)
    navigate(`/prescriptions/edit/${prescription.id}`, { state: { prescription } });
  };

  const renderPrescriptionCard = (prescription: PrescriptionDto, isPast: boolean = false) => {
    const scheduleLines = formatSchedule(prescription.schedule);
    const expired = isExpired(prescription);

    return (
      <Card key={prescription.id} elevation={2} sx={{ opacity: isPast ? 0.7 : 1 }}>
        <CardContent>
          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", mb: 2 }}>
            <Box sx={{ flex: 1 }}>
              <Typography variant="h6" fontWeight="600" gutterBottom>
                {prescription.medicineName}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {prescription.dosageAmount} {prescription.dosageUnit}
              </Typography>
            </Box>
            <Box sx={{ display: "flex", gap: 1, alignItems: "center" }}>
              {expired ? (
                <Chip label="No Longer Taking" color="error" size="small" />
              ) : prescription.endDate ? (
                <Chip label="Active" color="primary" size="small" />
              ) : (
                <Chip label="Ongoing" color="success" size="small" />
              )}
              {!isPast && (
                <>
                  <IconButton
                    size="small"
                    color="primary"
                    onClick={() => handleEditClick(prescription)}
                    title="Edit prescription"
                  >
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => handleDeleteClick(prescription)}
                    title="Delete prescription"
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </>
              )}
            </Box>
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
  };

  if (loading) return <Loading label="Loading prescriptions..." />;

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <Navbar />

      <Container maxWidth="md" sx={{ py: 4 }}>
        <Box sx={{ mb: 3 }}>
          <Typography variant="h4" fontWeight="bold">
            My Prescriptions
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {items.length === 0 ? (
          <Paper elevation={2} sx={{ p: 4, textAlign: "center" }}>
            <Typography color="text.secondary" sx={{ mb: 2 }}>
              No prescriptions yet. Add one from the Medicine Database!
            </Typography>
            <PrimaryButton onClick={() => navigate("/medicine")}>
              View Medicine
            </PrimaryButton>
          </Paper>
        ) : (
          <>
            {/* Active Prescriptions Section */}
            {activePrescriptions.length > 0 && (
              <Box sx={{ mb: 4 }}>
                <Typography variant="h5" fontWeight="600" gutterBottom sx={{ mb: 2 }}>
                  Active Prescriptions
                </Typography>
                <Stack spacing={2}>
                  {activePrescriptions.map((prescription) => renderPrescriptionCard(prescription, false))}
                </Stack>
              </Box>
            )}

            {/* Past Prescriptions Section */}
            {pastPrescriptions.length > 0 && (
              <Box>
                <Divider sx={{ my: 3 }} />
                <Typography variant="h5" fontWeight="600" gutterBottom sx={{ mb: 2 }} color="text.secondary">
                  Past Prescriptions
                </Typography>
                <Stack spacing={2}>
                  {pastPrescriptions.map((prescription) => renderPrescriptionCard(prescription, true))}
                </Stack>
              </Box>
            )}
          </>
        )}

        {/* Delete Confirmation Dialog */}
        <Dialog open={deleteDialogOpen} onClose={() => !deleting && setDeleteDialogOpen(false)}>
          <DialogTitle>Delete Prescription</DialogTitle>
          <DialogContent>
            <Typography>
              Are you sure you want to delete the prescription for <strong>{prescriptionToDelete?.medicineName}</strong>?
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              This action cannot be undone.
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDeleteDialogOpen(false)} disabled={deleting}>
              Cancel
            </Button>
            <Button onClick={handleDeleteConfirm} color="error" disabled={deleting}>
              {deleting ? "Deleting..." : "Delete"}
            </Button>
          </DialogActions>
        </Dialog>
      </Container>
    </Box>
  );
}
