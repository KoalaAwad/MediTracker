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
  CircularProgress,
  Alert,
  IconButton,
  Snackbar,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Checkbox,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import { adminApi, UserDto } from "../../api/adminApi";
import EditRoleDialog from "./EditRoleDialog";
import DeleteUserDialog from "./DeleteUserDialog";

export default function UsersList() {
  const [users, setUsers] = useState<UserDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserDto | null>(null);
  const [roles, setRoles] = useState<string[]>([]);
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [onlyRole, setOnlyRole] = useState(false);
  const navigate = useNavigate();

  const fetchRoles = async () => {
    try {
      const token = localStorage.getItem("token");
      if (!token) return;
      const res = await adminApi.getAvailableRoles(token);
      setRoles(res.data.roles);
    } catch (err: any) {
      // ignore silently or setError
      console.error("Failed to load roles", err);
    }
  };

  const fetchUsers = async (role?: string | null, only?: boolean) => {
    try {
      setLoading(true);
      const token = localStorage.getItem("token");
      if (!token) {
        navigate("/unauthorized");
        return;
      }

      console.debug("fetchUsers called with", { role, only });
      const response = await adminApi.getAllUsers(token, role, only);
      console.debug("adminApi.getAllUsers response", { params: response.config.params, url: response.request?.responseURL, count: response.data?.users?.length });
      setUsers(response.data.users);
    } catch (err: any) {
      if (err.response?.status === 403) {
        navigate("/unauthorized");
      } else if (err.response?.status === 401) {
        navigate("/login");
      } else {
        setError(err.response?.data?.error || "Failed to load users");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRoles();
    fetchUsers(selectedRole, onlyRole);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [navigate]);

  useEffect(() => {
    // when filters change, refetch
    fetchUsers(selectedRole, onlyRole);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedRole, onlyRole]);

  const handleEditClick = (user: UserDto) => {
    setSelectedUser(user);
    setEditDialogOpen(true);
  };

  const handleDeleteClick = (user: UserDto) => {
    setSelectedUser(user);
    setDeleteDialogOpen(true);
  };

  const handleEditSave = async (userId: number, roles: string[]) => {
    try {
      const token = localStorage.getItem("token");
      if (!token) return;

      await adminApi.updateUserRoles(token, { userId, roles });
      setSuccessMessage("User roles updated successfully");
      setEditDialogOpen(false);
      fetchUsers(selectedRole, onlyRole);
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to update user roles");
    }
  };

  const handleDeleteConfirm = async (userId: number) => {
    try {
      const token = localStorage.getItem("token");
      if (!token) return;

      await adminApi.deleteUser(token, userId);
      setSuccessMessage("User deleted successfully");
      setDeleteDialogOpen(false);
      fetchUsers(selectedRole, onlyRole);
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to delete user");
    }
  };

  if (loading) {
    return (
      <Box
        sx={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

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
            <Typography variant="h6">MediTracker - Admin Panel</Typography>
            <Button variant="outlined" onClick={() => navigate("/dashboard")}>
              Back to Dashboard
            </Button>
          </Box>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Paper elevation={2} sx={{ p: 3 }}>
          <Typography variant="h4" gutterBottom>
            User Management
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
              {error}
            </Alert>
          )}

          {/* Filters: role dropdown + only checkbox */}
          <Box sx={{ display: "flex", gap: 2, mb: 2, alignItems: "center" }}>
            <FormControl sx={{ minWidth: 200 }}>
              <InputLabel id="role-select-label">Filter by role</InputLabel>
              <Select
                labelId="role-select-label"
                value={selectedRole || ""}
                label="Filter by role"
                onChange={(e) => setSelectedRole(e.target.value || null)}
                size="small"
              >
                <MenuItem value="">All</MenuItem>
                {roles.map((r) => (
                  <MenuItem key={r} value={r}>
                    {r}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControlLabel
              control={
                <Checkbox
                  checked={onlyRole}
                  onChange={(e) => setOnlyRole(e.target.checked)}
                />
              }
              label="Only users with this role"
            />

          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell><strong>User ID</strong></TableCell>
                  <TableCell><strong>Name</strong></TableCell>
                  <TableCell><strong>Email</strong></TableCell>
                  <TableCell><strong>Role</strong></TableCell>
                  <TableCell><strong>Created At</strong></TableCell>
                  <TableCell align="right"><strong>Actions</strong></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      No users found
                    </TableCell>
                  </TableRow>
                ) : (
                  users.map((user) => (
                    <TableRow key={user.userId}>
                      <TableCell>{user.userId}</TableCell>
                      <TableCell>{user.name}</TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>{user.role}</TableCell>
                      <TableCell>
                        {new Date(user.createdAt).toLocaleDateString()}
                      </TableCell>
                      <TableCell align="right">
                        <IconButton
                          color="primary"
                          onClick={() => handleEditClick(user)}
                          title="Edit roles"
                        >
                          <EditIcon />
                        </IconButton>
                        <IconButton
                          color="error"
                          onClick={() => handleDeleteClick(user)}
                          title="Delete user"
                        >
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      </Container>

      <EditRoleDialog
        open={editDialogOpen}
        user={selectedUser}
        onClose={() => setEditDialogOpen(false)}
        onSave={handleEditSave}
      />

      <DeleteUserDialog
        open={deleteDialogOpen}
        user={selectedUser}
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
