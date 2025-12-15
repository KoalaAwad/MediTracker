import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Container,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  IconButton,
  Snackbar,
  Alert,
  TextField,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import { medicineApi, Medicine } from "../../api/medicineApi";
import DeleteMedicineDialog from "./DeleteMedicineDialog";
import Loading from "../ui/Loading";
import { useAuthStore } from "../../zustand/authStore";
import { PrimaryButton } from "../ui/StyledButton";

interface MedicineListProps {
  isAdmin: boolean;
  isDoctor: boolean;
}

export default function MedicineList({ isAdmin, isDoctor }: MedicineListProps) {
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedMedicine, setSelectedMedicine] = useState<Medicine | null>(
    null
  );
  const [search, setSearch] = useState<string>("");
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);
  const role = useAuthStore((s) => s.user?.role || "");
  const isPatient = role.includes("PATIENT");

  const fetchMedicines = async () => {
    try {
      if (!token) {
        navigate("/login");
        return;
      }

      const response = await medicineApi.getAll(token);
      setMedicines(response.data);
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to load medicines");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMedicines();
  }, [token]);

  const handleDeleteClick = (medicine: Medicine) => {
    setSelectedMedicine(medicine);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!selectedMedicine) return;

    try {
      if (!token) return;

      await medicineApi.delete(selectedMedicine.id!, token);
      setSuccessMessage("Medicine deleted successfully");
      setDeleteDialogOpen(false);
      fetchMedicines();
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to delete medicine");
    }
  };

  const filteredMedicines = medicines.filter((m) =>
    m.name.toLowerCase().includes(search.trim().toLowerCase())
  );

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <Box sx={{ bgcolor: "white", borderBottom: 1, borderColor: "divider" }}>
        <Container maxWidth="lg">
          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              py: 2,
            }}
          >
            <Typography variant="h6">
              MediTracker - Medicine Database
            </Typography>
            <Box sx={{ display: "flex", gap: 2 }}>
              {(isAdmin || isDoctor) && (
                <PrimaryButton
                  onClick={() => navigate("/medicine/add")}
                >
                  Add Medicine
                </PrimaryButton>
              )}
              <Button variant="outlined" onClick={() => navigate("/dashboard")}>
                Back to Dashboard
              </Button>
            </Box>
          </Box>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Paper elevation={2} sx={{ p: 3 }}>
          <Typography variant="h4" gutterBottom>
            Medicine List
          </Typography>

          {/* Search input */}
          <Box sx={{ display: "flex", gap: 2, mb: 2 }}>
            <TextField
              label="Search medicines"
              placeholder="Type medicine name..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              size="small"
              fullWidth
            />
          </Box>

          {error && (
            <Alert
              severity="error"
              sx={{ mb: 2 }}
              onClose={() => setError(null)}
            >
              {error}
            </Alert>
          )}

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>
                    <strong>Name</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Generic Name</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Manufacturer</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Dosage Form</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Strength</strong>
                  </TableCell>
                  <TableCell align="right">
                    <strong>Actions</strong>
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredMedicines.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      No medicines found
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredMedicines.map((medicine) => (
                    <TableRow key={medicine.id}>
                      <TableCell>{medicine.name}</TableCell>
                      <TableCell>{medicine.genericName || "N/A"}</TableCell>
                      <TableCell>{medicine.manufacturer || "N/A"}</TableCell>
                      <TableCell>{medicine.dosageForm || "N/A"}</TableCell>
                      <TableCell>{medicine.strength || "N/A"}</TableCell>
                      <TableCell align="right">
                        <Box
                          sx={{
                            display: "flex",
                            gap: 1,
                            justifyContent: "flex-end",
                          }}
                        >
                          {isAdmin && (
                            <IconButton
                              color="primary"
                              onClick={() =>
                                navigate(`/medicine/edit/${medicine.id}`)
                              }
                              title="Edit medicine"
                            >
                              <EditIcon />
                            </IconButton>
                          )}
                          {isAdmin && (
                            <IconButton
                              color="error"
                              onClick={() => handleDeleteClick(medicine)}
                              title="Delete medicine"
                            >
                              <DeleteIcon />
                            </IconButton>
                          )}
                          {/* Add prescription visible only to patients */}
                          {isPatient && (
                            <PrimaryButton
                              size="small"
                              onClick={() =>
                                navigate(`/prescriptions/add/${medicine.id}`)
                              }
                            >
                              + Add prescription
                            </PrimaryButton>
                          )}
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>

          {loading && (
            <Loading fullScreen={false} label="Loading medicines..." />
          )}
        </Paper>
      </Container>

      <DeleteMedicineDialog
        open={deleteDialogOpen}
        medicine={selectedMedicine}
        onClose={() => setDeleteDialogOpen(false)}
        onConfirm={handleDeleteConfirm}
      />

      <Snackbar
        open={!!successMessage}
        autoHideDuration={3000}
        onClose={() => setSuccessMessage(null)}
        message={successMessage}
      />
    </Box>
  );
}
