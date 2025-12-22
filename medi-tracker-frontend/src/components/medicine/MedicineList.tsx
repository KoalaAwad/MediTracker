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
  Pagination,
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
  const [page, setPage] = useState<number>(1);
  const [size, setSize] = useState<number>(20);
  const [totalPages, setTotalPages] = useState<number>(0);
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);
  const role = useAuthStore((s) => s.user?.role || "");
  const isPatient = role.includes("PATIENT");

  const fetchMedicines = async (p = page, s = size, q = search) => {
    try {
      if (!token) {
        navigate("/login");
        return;
      }

      setLoading(true);
      const response = await medicineApi.getPaged(token, p - 1, s, q);
      setMedicines(response.data.content);
      setPage(response.data.page + 1);
      setSize(response.data.size);
      setTotalPages(response.data.totalPages);
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to load medicines");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMedicines(1, size, "");
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

  const handleSearch = () => {
    // Reset to first page when searching
    fetchMedicines(1, size, search);
  };

  const handlePageChange = (_: any, value: number) => {
    fetchMedicines(value, size, search);
  };

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
              {isAdmin && (
                <PrimaryButton
                  onClick={() => navigate("/medicine/update-database")}
                >
                  Update Medicine Database
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
            <PrimaryButton onClick={handleSearch}>Search</PrimaryButton>
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
                {medicines.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      No medicines found
                    </TableCell>
                  </TableRow>
                ) : (
                  medicines.map((medicine) => (
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

          <Box sx={{ display: "flex", justifyContent: "center", mt: 2 }}>
            <Pagination count={totalPages} page={page} onChange={handlePageChange} color="primary" />
          </Box>

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
