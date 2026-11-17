import { useAuthStore } from "../../zustand/authStore";
import { useNavigate } from "react-router-dom";
import { PrimaryButton } from "../../components/ui/StyledButton";
import Navbar from "../../components/ui/Navbar";
import TodayDoses from "../../components/doses/TodayDoses";
import RemindersCard from "../../components/doses/RemindersCard";
import { Box, Container, Typography, CircularProgress } from "@mui/material";

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

      {/* today's doses, with reminders beside them (below on phones) */}
      <Container maxWidth="lg" sx={{ py: { xs: 3, sm: 4 } }}>
        <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "1fr 320px" }, gap: 3, alignItems: "start" }}>
          <TodayDoses />
          <Box component="aside" sx={{ mt: { md: 1 } }}>
            <RemindersCard />
          </Box>
        </Box>
      </Container>
    </Box>
  );
}