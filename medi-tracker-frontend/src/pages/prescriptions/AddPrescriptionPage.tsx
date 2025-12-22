import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Container,
  Paper,
  Typography,
  Alert,
  TextField,
  Grid,
  MenuItem,
  Checkbox,
  FormControlLabel,
  Button,
} from "@mui/material";
import { PrimaryButton, SecondaryButton } from "../../components/ui/StyledButton";
import { medicineApi, Medicine } from "../../api/medicineApi";
import {
  prescriptionApi,
  PrescriptionRequestDto,
  ScheduleEntryDto,
} from "../../api/prescriptionApi";
import Loading from "../../components/ui/Loading";
import { useAuthStore } from "../../zustand/authStore";

export default function AddPrescriptionPage() {
  const { id } = useParams();
  const medicineId = Number(id);
  const navigate = useNavigate();

  const [medicine, setMedicine] = useState<Medicine | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [dosageAmount, setDosageAmount] = useState<number>(1);
  const [dosageUnit, setDosageUnit] = useState<string>("TABLET");
  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");
  const [isOngoing, setIsOngoing] = useState<boolean>(false); // Track if prescription is ongoing/indefinite
  const [timeZone, setTimeZone] = useState<string>(
    Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC"
  );
  const [schedule, setSchedule] = useState<ScheduleEntryDto[]>([
    { dayOfWeek: "MONDAY", timeOfDay: "08:00" },
  ]);

  // Track which schedule entries are marked as "daily"
  const [dailyFlags, setDailyFlags] = useState<boolean[]>([false]);

  const role = useAuthStore((s) => s.user?.role || "");
  const isPatient = role.includes("PATIENT");
  const token = useAuthStore((s) => s.token);

  useEffect(() => {
    if (!isPatient) {
      navigate("/unauthorized");
    }
  }, [isPatient, navigate]);

  useEffect(() => {
    const fetchMedicine = async () => {
      try {
        if (!token) {
          navigate("/login");
          return;
        }
        const res = await medicineApi.getById(medicineId, token);
        setMedicine(res.data);
      } catch (err: any) {
        setError(err.response?.data?.error || "Failed to load medicine");
      } finally {
        setLoading(false);
      }
    };

    if (!Number.isFinite(medicineId)) {
      setError("Invalid medicine ID");
      setLoading(false);
      return;
    }

    fetchMedicine();
  }, [medicineId, navigate, token]);

  const addScheduleRow = () => {
    setSchedule([...schedule, { dayOfWeek: "MONDAY", timeOfDay: "08:00" }]);
    setDailyFlags([...dailyFlags, false]);
  };

  const updateScheduleRow = (
    index: number,
    field: keyof ScheduleEntryDto,
    value: string
  ) => {
    const next = [...schedule];
    next[index] = { ...next[index], [field]: value };
    setSchedule(next);
  };

  const removeScheduleRow = (index: number) => {
    const next = schedule.filter((_, i) => i !== index);
    const nextFlags = dailyFlags.filter((_, i) => i !== index);
    setSchedule(next);
    setDailyFlags(nextFlags);
  };

  const toggleDaily = (index: number) => {
    const newFlags = [...dailyFlags];
    newFlags[index] = !newFlags[index];
    setDailyFlags(newFlags);
  };

  const handleSubmit = async () => {
    try {
      if (!token) {
        navigate("/login");
        return;
      }

      // Expand daily entries: if a row is marked "daily", create 7 entries (one per day)
      const expandedSchedule: ScheduleEntryDto[] = [];
      const allDays = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

      schedule.forEach((entry, index) => {
        if (dailyFlags[index]) {
          // This entry is "daily" - add it for all 7 days
          allDays.forEach((day) => {
            expandedSchedule.push({ dayOfWeek: day, timeOfDay: entry.timeOfDay });
          });
        } else {
          // Not daily - add single entry as is
          expandedSchedule.push(entry);
        }
      });

      const dto: PrescriptionRequestDto = {
        medicineId,
        dosage: { amount: dosageAmount, unit: dosageUnit },
        startDate,
        endDate: isOngoing ? undefined : (endDate || undefined), // Ongoing = no end date
        timeZone,
        schedule: expandedSchedule,
      };

      await prescriptionApi.createForMe(token, dto);
      navigate("/prescriptions");
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to create prescription");
    }
  };

  if (loading) return <Loading label="Loading medicine..." />;

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Paper elevation={2} sx={{ p: 3 }}>
          <Typography variant="h5" gutterBottom>
            Add Prescription
          </Typography>

          {error && (
            <Alert
              severity="error"
              sx={{ mb: 2 }}
              onClose={() => setError(null)}
            >
              {error}
            </Alert>
          )}

          {medicine && (
            <Box sx={{ mb: 3 }}>
              <Typography variant="subtitle1">Medicine</Typography>
              <Typography>{medicine.name}</Typography>
            </Box>
          )}

          <Grid container spacing={2}>
            <Grid item xs={6}>
              <TextField
                label="Dosage amount"
                type="number"
                value={dosageAmount}
                onChange={(e) => setDosageAmount(Number(e.target.value))}
                fullWidth
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                select
                label="Dosage unit"
                value={dosageUnit}
                onChange={(e) => setDosageUnit(e.target.value)}
                fullWidth
              >
                {[
                  "TABLET",
                  "CAPSULE",
                  "PUFF",
                  "DROP",
                  "PATCH",
                  "ML",
                  "MG",
                  "MCG",
                ].map((u) => (
                  <MenuItem key={u} value={u}>
                    {u}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={6}>
              <TextField
                label="Start date"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                fullWidth
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={6}>
              <Box sx={{ display: "flex", gap: 1, alignItems: "flex-start" }}>
                <TextField
                  label="End date"
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  fullWidth
                  disabled={isOngoing}
                  InputLabelProps={{ shrink: true }}
                  helperText={isOngoing ? "Ongoing - no end date" : ""}
                />
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={isOngoing}
                      onChange={(e) => {
                        setIsOngoing(e.target.checked);
                        if (e.target.checked) {
                          setEndDate(""); // Clear end date when ongoing is checked
                        }
                      }}
                    />
                  }
                  label="Ongoing"
                  sx={{ mt: 1, whiteSpace: "nowrap" }}
                />
              </Box>
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Time zone"
                value={timeZone}
                onChange={(e) => setTimeZone(e.target.value)}
                fullWidth
              />
            </Grid>

            <Grid item xs={12}>
              <Typography variant="subtitle1" sx={{ mb: 1 }}>
                Schedule entries
              </Typography>
              {schedule.map((row, i) => (
                <Box key={i} sx={{ display: "flex", gap: 2, mb: 2, alignItems: "center" }}>
                  {!dailyFlags[i] && (
                    <TextField
                      select
                      label="Day of week"
                      value={row.dayOfWeek}
                      onChange={(e) =>
                        updateScheduleRow(i, "dayOfWeek", e.target.value)
                      }
                      sx={{ minWidth: 160 }}
                    >
                      {[
                        "MONDAY",
                        "TUESDAY",
                        "WEDNESDAY",
                        "THURSDAY",
                        "FRIDAY",
                        "SATURDAY",
                        "SUNDAY",
                      ].map((d) => (
                        <MenuItem key={d} value={d}>
                          {d}
                        </MenuItem>
                      ))}
                    </TextField>
                  )}
                  <TextField
                    label="Time of day"
                    type="time"
                    value={row.timeOfDay}
                    onChange={(e) =>
                      updateScheduleRow(i, "timeOfDay", e.target.value)
                    }
                    sx={{ minWidth: 160 }}
                    InputLabelProps={{ shrink: true }}
                  />
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={dailyFlags[i]}
                        onChange={() => toggleDaily(i)}
                      />
                    }
                    label="Daily"
                  />
                  <Button color="error" onClick={() => removeScheduleRow(i)}>
                    Remove
                  </Button>
                </Box>
              ))}
              <Button onClick={addScheduleRow}>+ Add schedule time</Button>
            </Grid>
          </Grid>

          <Box sx={{ display: "flex", gap: 2, mt: 3 }}>
            <PrimaryButton onClick={handleSubmit}>
              Save prescription
            </PrimaryButton>
            <SecondaryButton onClick={() => navigate(-1)}>
              Cancel
            </SecondaryButton>
          </Box>
        </Paper>
      </Container>
    </Box>
  );
}
