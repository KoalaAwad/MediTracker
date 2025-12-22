import { useEffect, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
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
  PrescriptionDto,
} from "../../api/prescriptionApi";
import Loading from "../../components/ui/Loading";
import { useAuthStore } from "../../zustand/authStore";

export default function EditPrescriptionPage() {
  const { id } = useParams();
  const prescriptionId = Number(id);
  const navigate = useNavigate();
  const location = useLocation();

  // Get prescription from navigation state
  const prescriptionFromState = location.state?.prescription as PrescriptionDto | undefined;

  const [medicine, setMedicine] = useState<Medicine | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [dosageAmount, setDosageAmount] = useState<number>(1);
  const [dosageUnit, setDosageUnit] = useState<string>("TABLET");
  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");
  const [isOngoing, setIsOngoing] = useState<boolean>(false);
  const [timeZone, setTimeZone] = useState<string>(
    Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC"
  );
  const [schedule, setSchedule] = useState<ScheduleEntryDto[]>([
    { dayOfWeek: "MONDAY", timeOfDay: "08:00" },
  ]);
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
    const fetchData = async () => {
      try {
        if (!token || !prescriptionFromState) {
          navigate("/prescriptions");
          return;
        }

        // Load the medicine
        const res = await medicineApi.getById(prescriptionFromState.medicineId, token);
        setMedicine(res.data);

        // Pre-fill form with prescription data
        setDosageAmount(prescriptionFromState.dosageAmount);
        setDosageUnit(prescriptionFromState.dosageUnit);
        setStartDate(prescriptionFromState.startDate);
        setEndDate(prescriptionFromState.endDate || "");
        setIsOngoing(!prescriptionFromState.endDate);
        setTimeZone(prescriptionFromState.timeZone);

        // Set schedule (collapse daily entries)
        const uniqueSchedule = collapseDailyEntries(prescriptionFromState.schedule);
        setSchedule(uniqueSchedule.schedule);
        setDailyFlags(uniqueSchedule.flags);

      } catch (err: any) {
        setError(err.response?.data?.error || "Failed to load prescription");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [prescriptionFromState, navigate, token]);

  // Helper to collapse daily entries into single entry with daily flag
  const collapseDailyEntries = (entries: ScheduleEntryDto[]) => {
    const byTime = new Map<string, Set<string>>();

    entries.forEach(entry => {
      if (!byTime.has(entry.timeOfDay)) {
        byTime.set(entry.timeOfDay, new Set());
      }
      byTime.get(entry.timeOfDay)!.add(entry.dayOfWeek);
    });

    const schedule: ScheduleEntryDto[] = [];
    const flags: boolean[] = [];

    byTime.forEach((days, time) => {
      if (days.size === 7) {
        // All 7 days - mark as daily
        schedule.push({ dayOfWeek: "MONDAY", timeOfDay: time });
        flags.push(true);
      } else {
        // Individual days
        days.forEach(day => {
          schedule.push({ dayOfWeek: day, timeOfDay: time });
          flags.push(false);
        });
      }
    });

    return { schedule, flags };
  };

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
      if (!token || !prescriptionFromState) {
        navigate("/login");
        return;
      }

      // Expand daily entries
      const expandedSchedule: ScheduleEntryDto[] = [];
      const allDays = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

      schedule.forEach((entry, index) => {
        if (dailyFlags[index]) {
          allDays.forEach((day) => {
            expandedSchedule.push({ dayOfWeek: day, timeOfDay: entry.timeOfDay });
          });
        } else {
          expandedSchedule.push(entry);
        }
      });

      const dto: PrescriptionRequestDto = {
        medicineId: prescriptionFromState.medicineId,
        dosage: { amount: dosageAmount, unit: dosageUnit },
        startDate,
        endDate: isOngoing ? undefined : (endDate || undefined),
        timeZone,
        schedule: expandedSchedule,
      };

      await prescriptionApi.update(token, prescriptionId, dto);
      navigate("/prescriptions");
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to update prescription");
    }
  };

  if (loading) return <Loading label="Loading prescription..." />;

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Paper elevation={2} sx={{ p: 3 }}>
          <Typography variant="h5" gutterBottom>
            Edit Prescription
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
                          setEndDate("");
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
              Update prescription
            </PrimaryButton>
            <SecondaryButton onClick={() => navigate("/prescriptions")}>
              Cancel
            </SecondaryButton>
          </Box>
        </Paper>
      </Container>
    </Box>
  );
}

