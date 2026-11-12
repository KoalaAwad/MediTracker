import { useAuthStore } from "../../zustand/authStore";
import { useNavigate } from "react-router-dom";
import { PrimaryButton } from "../../components/ui/StyledButton";
import Navbar from "../../components/ui/Navbar";
import TodayDoses from "../../components/doses/TodayDoses";
import RemindersCard from "../../components/doses/RemindersCard";
import { Box, Container, Paper, Typography, CircularProgress } from "@mui/material";

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


  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      {/* Navigation Header */}
      <Navbar />

      {/* Main Content */}
      <Container maxWidth="xl" sx={{ py: 4 }}>
        {/* Welcome Card */}
        <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
          <Typography variant="h5" component="h2" color="text.primary" sx={{ fontWeight: 700 }}>
            Welcome back, {user.name}!
          </Typography>
        </Paper>

        <TodayDoses />
        <RemindersCard />

      </Container>
    </Box>
  );
}