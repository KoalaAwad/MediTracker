import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
} from "@mui/material";
import { UserDto } from "../../api/adminApi";
import WarningIcon from "@mui/icons-material/Warning";

interface DeleteUserDialogProps {
  open: boolean;
  user: UserDto | null;
  onClose: () => void;
  onConfirm: (userId: number) => void;
}

export default function DeleteUserDialog({
  open,
  user,
  onClose,
  onConfirm,
}: DeleteUserDialogProps) {
  const handleConfirm = () => {
    if (user) {
      onConfirm(user.userId);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <WarningIcon color="error" />
          Confirm Delete User
        </Box>
      </DialogTitle>
      <DialogContent>
        {user && (
          <Box>
            <Typography variant="body1" gutterBottom>
              Are you sure you want to delete this user?
            </Typography>
            <Box
              sx={{
                mt: 2,
                p: 2,
                bgcolor: "grey.100",
                borderRadius: 1,
              }}
            >
              <Typography variant="body2">
                <strong>Name:</strong> {user.name}
              </Typography>
              <Typography variant="body2">
                <strong>Email:</strong> {user.email}
              </Typography>
              <Typography variant="body2">
                <strong>Role:</strong> {user.role}
              </Typography>
            </Box>
            <Typography
              variant="body2"
              color="error"
              sx={{ mt: 2, fontWeight: "bold" }}
            >
              ⚠️ This action cannot be undone!
            </Typography>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleConfirm} variant="contained" color="error">
          Delete User
        </Button>
      </DialogActions>
    </Dialog>
  );
}

