import { useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  FormControlLabel,
  Checkbox,
  Box,
  Typography,
} from "@mui/material";
import { UserDto } from "../../api/adminApi";

interface EditRoleDialogProps {
  open: boolean;
  user: UserDto | null;
  onClose: () => void;
  onSave: (userId: number, roles: string[]) => void;
}

export default function EditRoleDialog({
  open,
  user,
  onClose,
  onSave,
}: EditRoleDialogProps) {
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);

  const availableRoles = ["PATIENT", "ADMIN", "DOCTOR"];

  const handleOpen = () => {
    if (user) {
      setSelectedRoles(user.role.split(",").map((r) => r.trim()));
    }
  };

  const handleRoleToggle = (role: string) => {
    setSelectedRoles((prev) =>
      prev.includes(role)
        ? prev.filter((r) => r !== role)
        : [...prev, role]
    );
  };

  const handleSave = () => {
    if (user && selectedRoles.length > 0) {
      onSave(user.userId, selectedRoles);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      TransitionProps={{
        onEntered: handleOpen
      }}
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle>Edit User Roles</DialogTitle>
      <DialogContent>
        {user && (
          <Box>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              User: <strong>{user.name}</strong> ({user.email})
            </Typography>
            <Typography variant="body2" sx={{ mb: 2 }}>
              Select roles for this user:
            </Typography>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
              {availableRoles.map((role) => (
                <FormControlLabel
                  key={role}
                  control={
                    <Checkbox
                      checked={selectedRoles.includes(role)}
                      onChange={() => handleRoleToggle(role)}
                    />
                  }
                  label={role}
                />
              ))}
            </Box>
            {selectedRoles.length === 0 && (
              <Typography variant="body2" color="error" sx={{ mt: 2 }}>
                At least one role must be selected
              </Typography>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          onClick={handleSave}
          variant="contained"
          disabled={selectedRoles.length === 0}
        >
          Save
        </Button>
      </DialogActions>
    </Dialog>
  );
}

