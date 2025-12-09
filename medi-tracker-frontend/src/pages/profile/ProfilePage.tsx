import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Container,
  Typography,
  Paper,
  Alert,
  Button,
  Snackbar,
} from "@mui/material";
import { profileApi, ProfileDto } from "../../api/profileApi";
import PatientProfileSection from "../../components/profile/PatientProfileSection";
import DoctorProfileSection from "../../components/profile/DoctorProfileSection";
import Loading from "../../components/ui/Loading";

export default function ProfilePage() {
  const [profile, setProfile] = useState<ProfileDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const token = localStorage.getItem("token");
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
  }, [navigate]);

  const handlePatientSave = async (updated: any) => {
    try {
      const token = localStorage.getItem("token");
      if (!token) return;

      await profileApi.updatePatientProfile(token, updated);
      setSuccessMessage("Patient profile updated successfully");
      setError(null);

      // Refresh profile
      const response = await profileApi.getProfile(token);
      setProfile(response.data);
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to update patient profile");
    }
  };

  const handleDoctorSave = async (updated: any) => {
    try {
      const token = localStorage.getItem("token");
      if (!token) return;

      await profileApi.updateDoctorProfile(token, updated);
      setSuccessMessage("Doctor profile updated successfully");
      setError(null);

      // Refresh profile
      const response = await profileApi.getProfile(token);
      setProfile(response.data);
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to update doctor profile");
    }
  };

  // Helper flags based on roles
  const hasPatientRole = profile?.roles.includes("PATIENT");
  const hasDoctorRole = profile?.roles.includes("DOCTOR");
  const isAdminOnly = profile?.roles.length === 1 && profile.roles.includes("ADMIN");

  if (loading) {
    return <Loading label="Loading profile..." />;
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
            <Typography variant="h6">MediTracker - My Profile</Typography>
            <Button variant="outlined" onClick={() => navigate("/dashboard")}>
              Back to Dashboard
            </Button>
          </Box>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: 4 }}>
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

                <Typography variant="subtitle2" color="textSecondary">
                  Roles
                </Typography>
                <Typography sx={{ mb: 2 }}>
                  {profile.roles.join(", ")}
                </Typography>
              </Box>
            </Paper>

            {/* Editable Sections: show based on roles only. Admin has no dedicated editable section. */}
            {!isAdminOnly && (
              <>
                {/* Patient Profile Section */}
                {hasPatientRole && (
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
                )}

                {/* Doctor Profile Section */}
                {hasDoctorRole && (
                  <DoctorProfileSection
                    profile={
                      profile.doctorProfile ?? {
                        id: profile.userId,
                        active: true,
                      } as any
                    }
                    onSave={handleDoctorSave}
                  />
                )}
              </>
            )}
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
