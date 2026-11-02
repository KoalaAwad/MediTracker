import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Paper,
  TextField,
  Typography,
} from "@mui/material";
import { authApi } from "../../api/authApi";
import { useAuthStore } from "../../zustand/authStore";

export default function DeleteAccountSection() {
  const token = useAuthStore((s) => s.token);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const close = () => {
    if (deleting) return;
    setOpen(false);
    setPassword("");
    setError(null);
  };

  const handleDelete = async () => {
    if (!token) return;
    setDeleting(true);
    setError(null);
    try {
      await authApi.deleteAccount(token, password);
      logout();
      navigate("/", { replace: true });
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not delete account");
      setDeleting(false);
    }
  };

  return (
    <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
      <Typography variant="h5" gutterBottom>
        Delete account
      </Typography>
      <Typography color="textSecondary" sx={{ mb: 2 }}>
        Permanently deletes your account, profile and all your prescriptions. This can't be undone.
      </Typography>
      <Button variant="outlined" color="error" onClick={() => setOpen(true)}>
        Delete my account
      </Button>

      <Dialog open={open} onClose={close} maxWidth="xs" fullWidth>
        <DialogTitle>Delete account?</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Enter your password to confirm. All your data will be removed.
          </DialogContentText>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <TextField
            label="Password"
            type="password"
            fullWidth
            autoFocus
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && password) handleDelete();
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={close} disabled={deleting}>
            Cancel
          </Button>
          <Button color="error" variant="contained" onClick={handleDelete} disabled={!password || deleting}>
            {deleting ? "Deleting..." : "Delete account"}
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
}
