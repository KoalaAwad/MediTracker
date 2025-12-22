import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Container,
  Typography,
  Paper,
  Button,
  Chip,
  Divider,
  Alert,
  List,
  ListItem,
  ListItemText,
} from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { medicineApi, Medicine } from "../../api/medicineApi";
import { useAuthStore } from "../../zustand/authStore";
import Loading from "../../components/ui/Loading";

export default function MedicineDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);
  const [medicine, setMedicine] = useState<Medicine | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id || !token) return;

    const fetchMedicine = async () => {
      try {
        setLoading(true);
        const response = await medicineApi.getById(parseInt(id), token);
        setMedicine(response.data);
      } catch (err: any) {
        setError(err.response?.data?.error || "Failed to load medicine details");
      } finally {
        setLoading(false);
      }
    };

    fetchMedicine();
  }, [id, token]);

  if (loading) return <Loading label="Loading medicine details..." />;
  if (error) return <Alert severity="error">{error}</Alert>;
  if (!medicine) return <Alert severity="warning">Medicine not found</Alert>;

  const { openfda } = medicine;

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
            <Typography variant="h6">Medicine Details</Typography>
            <Button
              startIcon={<ArrowBackIcon />}
              onClick={() => navigate("/medicine")}
            >
              Back to List
            </Button>
          </Box>
        </Container>
      </Box>

      <Container maxWidth="md" sx={{ py: 4 }}>
        <Paper elevation={2} sx={{ p: 4 }}>
          {/* Brand Name */}
          <Typography variant="h4" gutterBottom>
            {medicine.name}
          </Typography>

          {/* Generic Name */}
          {medicine.genericName && (
            <Typography variant="h6" color="text.secondary" gutterBottom>
              Generic: {medicine.genericName}
            </Typography>
          )}

          {/* Manufacturer */}
          {medicine.manufacturer && (
            <Typography variant="body1" color="text.secondary" sx={{ mb: 2 }}>
              Manufacturer: {medicine.manufacturer}
            </Typography>
          )}

          <Divider sx={{ my: 3 }} />

          {/* OpenFDA Data */}
          {openfda && (
            <>
              <Typography variant="h6" gutterBottom>
                FDA Information
              </Typography>

              {/* Brand Names */}
              {openfda.brand_name && openfda.brand_name.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Brand Names:
                  </Typography>
                  <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                    {openfda.brand_name.map((name, idx) => (
                      <Chip key={idx} label={name} size="small" />
                    ))}
                  </Box>
                </Box>
              )}

              {/* Generic Names */}
              {openfda.generic_name && openfda.generic_name.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Generic Names:
                  </Typography>
                  <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                    {openfda.generic_name.map((name, idx) => (
                      <Chip key={idx} label={name} size="small" color="primary" />
                    ))}
                  </Box>
                </Box>
              )}

              {/* Substance Names */}
              {openfda.substance_name && openfda.substance_name.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Active Substances:
                  </Typography>
                  <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                    {openfda.substance_name.map((name, idx) => (
                      <Chip key={idx} label={name} size="small" variant="outlined" />
                    ))}
                  </Box>
                </Box>
              )}

              <Divider sx={{ my: 2 }} />

              {/* Pharmaceutical Classes */}
              <Typography variant="h6" gutterBottom>
                Pharmaceutical Classification
              </Typography>

              {/* EPC - Established Pharmacologic Class */}
              {openfda.pharm_class_epc && openfda.pharm_class_epc.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Pharmacologic Class:
                  </Typography>
                  <List dense>
                    {openfda.pharm_class_epc.map((pc, idx) => (
                      <ListItem key={idx}>
                        <ListItemText primary={`• ${pc}`} />
                      </ListItem>
                    ))}
                  </List>
                </Box>
              )}

              {/* MOA - Mechanism of Action */}
              {openfda.pharm_class_moa && openfda.pharm_class_moa.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Mechanism of Action:
                  </Typography>
                  <List dense>
                    {openfda.pharm_class_moa.map((moa, idx) => (
                      <ListItem key={idx}>
                        <ListItemText primary={`• ${moa}`} />
                      </ListItem>
                    ))}
                  </List>
                </Box>
              )}

              {/* PE - Physiologic Effect */}
              {openfda.pharm_class_pe && openfda.pharm_class_pe.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Physiologic Effect:
                  </Typography>
                  <List dense>
                    {openfda.pharm_class_pe.map((pe, idx) => (
                      <ListItem key={idx}>
                        <ListItemText primary={`• ${pe}`} />
                      </ListItem>
                    ))}
                  </List>
                </Box>
              )}

              {/* CS - Chemical Structure */}
              {openfda.pharm_class_cs && openfda.pharm_class_cs.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Chemical Structure:
                  </Typography>
                  <List dense>
                    {openfda.pharm_class_cs.map((cs, idx) => (
                      <ListItem key={idx}>
                        <ListItemText primary={`• ${cs}`} />
                      </ListItem>
                    ))}
                  </List>
                </Box>
              )}

              {/* RxCUI */}
              {openfda.rxcui && openfda.rxcui.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    RxNorm Concept Unique Identifiers (RxCUI):
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {openfda.rxcui.join(", ")}
                  </Typography>
                </Box>
              )}
            </>
          )}

          {!openfda && (
            <Alert severity="info">
              No detailed FDA information available for this medicine.
            </Alert>
          )}
        </Paper>
      </Container>
    </Box>
  );
}

