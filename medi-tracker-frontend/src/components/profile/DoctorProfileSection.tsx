import { useState } from "react";
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Grid,
} from "@mui/material";
import { DoctorProfileDto } from "../../api/profileApi";

interface DoctorProfileSectionProps {
  profile: DoctorProfileDto | undefined;
  onSave?: (updatedProfile: DoctorProfileDto) => void;
}

export default function DoctorProfileSection({
  profile,
  onSave,
}: DoctorProfileSectionProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState<DoctorProfileDto>(
    profile || {
      id: 0,
      active: true,
    }
  );

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSave = () => {
    if (onSave) {
      onSave(formData);
      setIsEditing(false);
    }
  };

  return (
    <Card sx={{ mb: 3 }}>
      <CardContent>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
          <Typography variant="h6">Doctor Information</Typography>
          {!isEditing ? (
            <Button variant="outlined" onClick={() => setIsEditing(true)}>
              Edit
            </Button>
          ) : (
            <Box>
              <Button
                variant="contained"
                color="success"
                onClick={handleSave}
                sx={{ mr: 1 }}
              >
                Save
              </Button>
              <Button
                variant="outlined"
                onClick={() => {
                  setIsEditing(false);
                  setFormData(profile || { id: 0, active: true });
                }}
              >
                Cancel
              </Button>
            </Box>
          )}
        </Box>

        <Grid container spacing={2}>
          {isEditing ? (
            <>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="First Name"
                  name="firstName"
                  value={formData.firstName || ""}
                  onChange={handleInputChange}
                  size="small"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Last Name"
                  name="lastName"
                  value={formData.lastName || ""}
                  onChange={handleInputChange}
                  size="small"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Specialization"
                  name="specialization"
                  value={formData.specialization || ""}
                  onChange={handleInputChange}
                  size="small"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="License Number"
                  name="licenseNumber"
                  value={formData.licenseNumber || ""}
                  onChange={handleInputChange}
                  size="small"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Phone"
                  name="phone"
                  value={formData.phone || ""}
                  onChange={handleInputChange}
                  size="small"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Clinic Address"
                  name="clinicAddress"
                  value={formData.clinicAddress || ""}
                  onChange={handleInputChange}
                  size="small"
                />
              </Grid>
            </>
          ) : (
            <>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="textSecondary">
                  First Name
                </Typography>
                <Typography>{formData.firstName || "N/A"}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="textSecondary">
                  Last Name
                </Typography>
                <Typography>{formData.lastName || "N/A"}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="textSecondary">
                  Specialization
                </Typography>
                <Typography>{formData.specialization || "N/A"}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="textSecondary">
                  License Number
                </Typography>
                <Typography>{formData.licenseNumber || "N/A"}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="textSecondary">
                  Phone
                </Typography>
                <Typography>{formData.phone || "N/A"}</Typography>
              </Grid>
              <Grid item xs={12}>
                <Typography variant="subtitle2" color="textSecondary">
                  Clinic Address
                </Typography>
                <Typography>{formData.clinicAddress || "N/A"}</Typography>
              </Grid>
            </>
          )}
        </Grid>
      </CardContent>
    </Card>
  );
}

