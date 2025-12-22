import { useAuthStore } from "../../zustand/authStore";
import { Link } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { PrimaryButton } from "../../components/ui/StyledButton";
import ThemeToggle from "../../components/ui/ThemeToggle";
import { Box, Container, Paper, Typography, Grid, Button, CircularProgress } from "@mui/material";

export default function Dashboard() {
    const navigate = useNavigate();
    const user = useAuthStore(s => s.user);
    const logout = useAuthStore(s => s.logout);
    const isLoading = useAuthStore(s => s.isLoading);


  if (isLoading) {
    return (
      <Box sx={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", bgcolor: "background.default" }}>
        <Box sx={{ textAlign: "center" }}>
          <CircularProgress sx={{ mb: 2 }} />
          <Typography color="text.secondary">Loading your dashboard...</Typography>
        </Box>
      </Box>
    );
  }

  if (!user) {
    return (
      <Box sx={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", bgcolor: "background.default" }}>
        <Box sx={{ textAlign: "center" }}>
          <Typography color="text.secondary" sx={{ mb: 2 }}>You need to log in first</Typography>
          <Link to="/login" style={{ textDecoration: "none" }}>
            <Button color="primary">Go to Login</Button>
          </Link>
        </Box>
      </Box>
    );
  }

  const isAdmin = user.role.includes("ADMIN");
  const isPatient = user.role.includes("PATIENT");

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      {/* Navigation Header */}
      <Box sx={{ bgcolor: "background.paper", borderBottom: 1, borderColor: "divider" }}>
        <Container maxWidth="xl">
          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", height: 64 }}>
            <Typography variant="h6" component="h1" color="text.primary" sx={{ fontWeight: 700 }}>
              MediTracker
            </Typography>
            <Box sx={{ display: "flex", gap: 1, alignItems: "center" }}>
              <ThemeToggle />
              <Button variant="outlined" onClick={() => navigate("/profile")}>
                My Profile
              </Button>
              <Button variant="contained" color="error" onClick={logout}>
                Logout
              </Button>
            </Box>
          </Box>
        </Container>
      </Box>

      {/* Main Content */}
      <Container maxWidth="xl" sx={{ py: 4 }}>
        {/* Welcome Card */}
        <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
          <Typography variant="h5" component="h2" gutterBottom color="text.primary" sx={{ fontWeight: 700 }}>
            Welcome back, {user.name}!
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <Paper elevation={1} sx={{ p: 2, bgcolor: "primary.light", color: "primary.contrastText" }}>
                <Typography variant="body2" sx={{ fontWeight: 600, mb: 0.5 }}>Email</Typography>
                <Typography variant="body1">{user.email}</Typography>
              </Paper>
            </Grid>
            <Grid item xs={12} md={4}>
              <Paper elevation={1} sx={{ p: 2, bgcolor: "success.light", color: "success.contrastText" }}>
                <Typography variant="body2" sx={{ fontWeight: 600, mb: 0.5 }}>Role</Typography>
                <Typography variant="body1">{user.role}</Typography>
              </Paper>
            </Grid>
            <Grid item xs={12} md={4}>
              <Paper elevation={1} sx={{ p: 2, bgcolor: "secondary.light", color: "secondary.contrastText" }}>
                <Typography variant="body2" sx={{ fontWeight: 600, mb: 0.5 }}>User ID</Typography>
                <Typography variant="body1">{user.userId}</Typography>
              </Paper>
            </Grid>
          </Grid>
        </Paper>

        {/* Quick Actions */}
        <Paper elevation={2} sx={{ p: 3 }}>
          <Typography variant="h6" component="h3" gutterBottom color="text.primary" sx={{ fontWeight: 600 }}>
            Quick Actions
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <PrimaryButton fullWidth onClick={() => navigate("/medicine")}>
                View Medicine
              </PrimaryButton>
            </Grid>
            {isPatient && (
              <Grid item xs={12} md={4}>
                <PrimaryButton fullWidth onClick={() => navigate("/prescriptions")}>
                  View Prescriptions
                </PrimaryButton>
              </Grid>
            )}
            {isAdmin && (
              <Grid item xs={12} md={4}>
                <PrimaryButton fullWidth onClick={() => navigate("/admin/users")}>
                  View Users
                </PrimaryButton>
              </Grid>
            )}
          </Grid>
        </Paper>
      </Container>
    </Box>
  );
}