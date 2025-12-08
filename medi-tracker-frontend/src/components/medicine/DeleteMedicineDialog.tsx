import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
} from "@mui/material";
import WarningIcon from "@mui/icons-material/Warning";
import { Medicine } from "../../api/medicineApi";

interface DeleteMedicineDialogProps {
  open: boolean;
  medicine: Medicine | null;
  onClose: () => void;
  onConfirm: () => void;
}

export default function DeleteMedicineDialog({
  open,
  medicine,
  onClose,
  onConfirm,
}: DeleteMedicineDialogProps) {
  const handleConfirm = () => {
    if (medicine) {
      onConfirm();
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <WarningIcon color="error" />
          Confirm Delete Medicine
        </Box>
      </DialogTitle>
      <DialogContent>
        {medicine && (
          <Box>
            <Typography variant="body1" gutterBottom>
              Are you sure you want to delete this medicine?
            </Typography>
            <Box sx={{ mt: 2, p: 2, bgcolor: "grey.100", borderRadius: 1 }}>
              <Typography variant="body2">
                <strong>Name:</strong> {medicine.name}
              </Typography>
              {medicine.genericName && (
                <Typography variant="body2">
                  <strong>Generic Name:</strong> {medicine.genericName}
                </Typography>
              )}
              {medicine.manufacturer && (
                <Typography variant="body2">
                  <strong>Manufacturer:</strong> {medicine.manufacturer}
                </Typography>
              )}
            </Box>
            <Typography variant="body2" color="error" sx={{ mt: 2, fontWeight: "bold" }}>
              ⚠️ This action cannot be undone!
            </Typography>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleConfirm} variant="contained" color="error">
          Delete Medicine
        </Button>
      </DialogActions>
    </Dialog>
  );
}

