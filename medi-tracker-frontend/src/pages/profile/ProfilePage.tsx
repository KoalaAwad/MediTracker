import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Container,
  Typography,
  Paper,
  Alert,
  Snackbar,
} from "@mui/material";
import { profileApi, ProfileDto } from "../../api/profileApi";
import PatientProfileSection from "../../components/profile/PatientProfileSection";
import DeleteAccountSection from "../../components/profile/DeleteAccountSection";
import Loading from "../../components/ui/Loading";
import Navbar from "../../components/ui/Navbar";
import { useAuthStore } from "../../zustand/authStore";

export default function ProfilePage() {
  const token = useAuthStore((s) => s.token);
  const [profile, setProfile] = useState<ProfileDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        if (!token) {
          navigate("/login");
          return;
        }

        const response = await profileApi.getProfile(token);
        setProfile(response.data);
      } catch (err: any) {
        if (err.response?.status === 401) {
          navigate("/login");
        } else if (err.response?.status === 404) {
          setError("Profile not found");
        } else {
          setError(
            err.response?.data?.error || "Failed to load profile"
          );
        }
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [token, navigate]);

  const handlePatientSave = async (updated: any) => {
    try {
      if (!token) return;

      await profileApi.updatePatientProfile(token, updated);
      setSuccessMessage("Health information updated");
      setError(null);

      // Refresh profile
      const response = await profileApi.getProfile(token);
      setProfile(response.data);
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to update health information");
    }
  };

  if (loading) {
    return <Loading label="Loading profile..." />;
  }

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <Navbar />

      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Typography variant="h4" fontWeight="bold" sx={{ mb: 3 }}>
          My Profile
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {profile && (
          <>
            <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
              <Typography variant="h5" gutterBottom>
                Account Information
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Typography variant="subtitle2" color="textSecondary">
                  Email
                </Typography>
                <Typography sx={{ mb: 2 }}>{profile.email}</Typography>

                <Typography variant="subtitle2" color="textSecondary">
                  Name
                </Typography>
                <Typography sx={{ mb: 2 }}>{profile.name}</Typography>

                {profile.roles.includes("ADMIN") && (
                  <>
                    <Typography variant="subtitle2" color="textSecondary">
                      Account type
                    </Typography>
                    <Typography sx={{ mb: 2 }}>Admin</Typography>
                  </>
                )}
              </Box>
            </Paper>

            <PatientProfileSection
              profile={
                profile.patientProfile ?? {
                  id: profile.userId,
                  name: profile.name,
                  active: true,
                } as any
              }
              onSave={handlePatientSave}
            />

            <DeleteAccountSection />
          </>
        )}

        <Snackbar
          open={!!successMessage}
          autoHideDuration={3000}
          onClose={() => setSuccessMessage(null)}
          message={successMessage}
        />
      </Container>
    </Box>
  );
}
