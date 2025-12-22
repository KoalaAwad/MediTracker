import { useAuthStore } from "../../zustand/authStore";
import { useNavigate } from "react-router-dom";
import { PrimaryButton, SecondaryButton } from "../../components/ui/StyledButton";
import Navbar from "../../components/ui/Navbar";
import { Box, Container, Paper, Typography, Grid, CircularProgress } from "@mui/material";

export default function Dashboard() {
    const navigate = useNavigate();
    const user = useAuthStore(s => s.user);
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
          <PrimaryButton onClick={() => navigate("/login")}>
            Go to Login
          </PrimaryButton>
        </Box>
      </Box>
    );
  }

  const isAdmin = user.role.includes("ADMIN");
  const isPatient = user.role.includes("PATIENT");

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      {/* Navigation Header */}
      <Navbar showHomeIcon={false} />

      {/* Main Content */}
      <Container maxWidth="xl" sx={{ py: 4 }}>
        {/* Welcome Card */}
        <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
          <Typography variant="h5" component="h2" gutterBottom color="text.primary" sx={{ fontWeight: 700 }}>
            Welcome back, {user.name}!
          </Typography>
          <Grid container spacing={3} justifyContent="center">
            <Grid item xs={12} sm={6} md={4}>
              <Box
                sx={{
                  bgcolor: "background.paper",
                  border: 1,
                  borderColor: "divider",
                  borderRadius: "12px",
                  p: 3,
                  boxShadow: "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)",
                  display: "flex",
                  flexDirection: "column",
                  gap: 1,
                  transition: "all 0.2s ease",
                  "&:hover": {
                    boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
                  }
                }}
              >
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ fontSize: "0.875rem", fontWeight: 500 }}
                >
                  Email
                </Typography>
                <Typography
                  variant="h6"
                  color="text.primary"
                  sx={{
                    fontSize: "1.125rem",
                    fontWeight: 600,
                    wordBreak: "break-all"
                  }}
                >
                  {user.email}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <Box
                sx={{
                  bgcolor: "background.paper",
                  border: 1,
                  borderColor: "divider",
                  borderRadius: "12px",
                  p: 3,
                  boxShadow: "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)",
                  display: "flex",
                  flexDirection: "column",
                  gap: 1,
                  transition: "all 0.2s ease",
                  "&:hover": {
                    boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
                  }
                }}
              >
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ fontSize: "0.875rem", fontWeight: 500 }}
                >
                  Role
                </Typography>
                <Typography
                  variant="h6"
                  color="text.primary"
                  sx={{ fontSize: "1.125rem", fontWeight: 600 }}
                >
                  {user.role}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <Box
                sx={{
                  bgcolor: "background.paper",
                  border: 1,
                  borderColor: "divider",
                  borderRadius: "12px",
                  p: 3,
                  boxShadow: "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)",
                  display: "flex",
                  flexDirection: "column",
                  gap: 1,
                  transition: "all 0.2s ease",
                  "&:hover": {
                    boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
                  }
                }}
              >
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ fontSize: "0.875rem", fontWeight: 500 }}
                >
                  User ID
                </Typography>
                <Typography
                  variant="h6"
                  color="text.primary"
                  sx={{ fontSize: "1.125rem", fontWeight: 600 }}
                >
                  {user.userId}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </Paper>

        {/* Quick Actions */}
        <Paper elevation={2} sx={{ p: 3 }}>
          <Typography variant="h6" component="h3" gutterBottom color="text.primary" sx={{ fontWeight: 600 }}>
            Quick Actions
          </Typography>
          <Grid container spacing={3} justifyContent="center">
            <Grid item xs={12} sm={6} md={4}>
              <PrimaryButton fullWidth onClick={() => navigate("/medicine")}>
                View Medicine
              </PrimaryButton>
            </Grid>
            {isPatient && (
              <Grid item xs={12} sm={6} md={4}>
                <PrimaryButton fullWidth onClick={() => navigate("/prescriptions")}>
                  View Prescriptions
                </PrimaryButton>
              </Grid>
            )}
            {isAdmin && (
              <Grid item xs={12} sm={6} md={4}>
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