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
  Alert,
  IconButton,
  Snackbar,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import { adminApi, UserDto } from "../../api/adminApi";
import DeleteUserDialog from "./DeleteUserDialog";
import Loading from "../ui/Loading";
import Navbar from "../ui/Navbar";
import { useAuthStore } from "../../zustand/authStore";

export default function UsersList() {
  const [users, setUsers] = useState<UserDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserDto | null>(null);
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      if (!token) {
        navigate("/unauthorized");
        return;
      }

      const response = await adminApi.getAllUsers(token);
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
    fetchUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [navigate]);

  const handleDeleteClick = (user: UserDto) => {
    setSelectedUser(user);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async (userId: number) => {
    try {
      if (!token) return;

      await adminApi.deleteUser(token, userId);
      setSuccessMessage("User deleted successfully");
      setDeleteDialogOpen(false);
      fetchUsers();
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to delete user");
    }
  };

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <Navbar />

      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Typography variant="h4" fontWeight="bold" sx={{ mb: 3 }}>
          User Management
        </Typography>

        <Paper elevation={2} sx={{ p: 3 }}>
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
                    <strong>User ID</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Name</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Email</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Role</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Created At</strong>
                  </TableCell>
                  <TableCell align="right">
                    <strong>Actions</strong>
                  </TableCell>
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
                      <TableCell>{user.role.includes("ADMIN") ? "Admin" : "User"}</TableCell>
                      <TableCell>
                        {new Date(user.createdAt).toLocaleDateString()}
                      </TableCell>
                      <TableCell align="right">
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

          {loading && <Loading fullScreen={false} label="Loading users..." />}
        </Paper>
      </Container>

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
