import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  Divider,
  Grid,
  Link,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableRow,
  Typography,
} from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import { medicineApi, Medicine, MedicineLabel } from "../../api/medicineApi";
import { useAuthStore } from "../../zustand/authStore";
import Loading from "../../components/ui/Loading";
import Navbar from "../../components/ui/Navbar";
import { PrimaryButton } from "../../components/ui/StyledButton";

// "CAPSULE, GELATIN COATED" -> "Capsule, gelatin coated"
function sentenceCase(s?: string) {
  if (!s) return undefined;
  const lower = s.toLowerCase();
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}

function Fact({ label, value }: { label: string; value?: string | null }) {
  if (!value) return null;
  return (
    <TableRow>
      <TableCell sx={{ color: "text.secondary", width: 170, pl: 0, verticalAlign: "top" }}>{label}</TableCell>
      <TableCell sx={{ pr: 0 }}>{value}</TableCell>
    </TableRow>
  );
}

export default function MedicineDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);
  const isUser = useAuthStore((s) => s.user?.role.includes("USER") ?? false);
  const [medicine, setMedicine] = useState<Medicine | null>(null);
  const [label, setLabel] = useState<MedicineLabel | null>(null);
  const [labelLoading, setLabelLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id || !token) return;
    const medicineId = parseInt(id);

    medicineApi
      .getById(medicineId, token)
      .then((res) => setMedicine(res.data))
      .catch((err) => setError(err.response?.data?.error || "Failed to load medicine details"))
      .finally(() => setLoading(false));

    // the label comes from DailyMed and can be slow, so it loads separately
    medicineApi
      .getLabel(medicineId, token)
      .then((res) => setLabel(res.data))
      .catch(() => setLabel({ available: false }))
      .finally(() => setLabelLoading(false));
  }, [id, token]);

  if (loading) return <Loading label="Loading medicine details..." />;

  const images = label?.images ?? [];
  const appearance = label?.appearance;
  const pharmClass = medicine?.openfda?.pharm_class_epc?.join(", ");

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <Navbar />

      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)} sx={{ mb: 2 }}>
          Back
        </Button>

        {error && <Alert severity="error">{error}</Alert>}

        {medicine && (
          <Paper elevation={2} sx={{ p: { xs: 2, sm: 4 } }}>
            {!medicine.active && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                The FDA no longer lists this product. Existing prescriptions still work.
              </Alert>
            )}

            <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: 2, mb: 1 }}>
              <Typography variant="h4" fontWeight="bold">
                {medicine.name}
              </Typography>
              {medicine.strength && <Chip label={medicine.strength} color="primary" />}
            </Box>
            <Typography variant="h6" color="text.secondary" sx={{ mb: 3 }}>
              {[sentenceCase(medicine.dosageForm), medicine.manufacturer].filter(Boolean).join(" · ")}
            </Typography>

            <Grid container spacing={4}>
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="h6" gutterBottom>
                  Package label
                </Typography>
                {labelLoading ? (
                  <Box sx={{ display: "flex", alignItems: "center", gap: 2, py: 4 }}>
                    <CircularProgress size={24} />
                    <Typography color="text.secondary">Loading the official label...</Typography>
                  </Box>
                ) : images.length > 0 ? (
                  <>
                    <Box
                      component="img"
                      src={images[selectedImage]}
                      alt={`${medicine.name} package label`}
                      sx={{
                        width: "100%",
                        maxHeight: 420,
                        objectFit: "contain",
                        bgcolor: "#fff",
                        border: 1,
                        borderColor: "divider",
                        borderRadius: 1,
                      }}
                    />
                    {images.length > 1 && (
                      <Box sx={{ display: "flex", gap: 1, mt: 1, overflowX: "auto" }}>
                        {images.map((src, i) => (
                          <Box
                            key={src}
                            component="img"
                            src={src}
                            alt={`Label image ${i + 1}`}
                            onClick={() => setSelectedImage(i)}
                            sx={{
                              width: 72,
                              height: 72,
                              objectFit: "cover",
                              bgcolor: "#fff",
                              cursor: "pointer",
                              borderRadius: 1,
                              border: 2,
                              borderColor: i === selectedImage ? "primary.main" : "divider",
                              flexShrink: 0,
                            }}
                          />
                        ))}
                      </Box>
                    )}
                  </>
                ) : (
                  <Alert severity="info">No package label image is available for this product.</Alert>
                )}

                {appearance && (
                  <Box sx={{ mt: 3 }}>
                    <Typography variant="h6" gutterBottom>
                      What the pill looks like
                    </Typography>
                    <Table size="small">
                      <TableBody>
                        <Fact label="Colour" value={appearance.color} />
                        <Fact label="Shape" value={appearance.shape} />
                        <Fact label="Size" value={appearance.size} />
                        <Fact label="Imprint" value={appearance.imprint} />
                        <Fact label="Score lines" value={appearance.score && appearance.score !== "1" ? appearance.score : undefined} />
                      </TableBody>
                    </Table>
                    <Typography variant="caption" color="text.secondary">
                      From the manufacturer's FDA label. Compare the imprint on your pill.
                    </Typography>
                  </Box>
                )}
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="h6" gutterBottom>
                  Details
                </Typography>
                <Table size="small">
                  <TableBody>
                    <Fact label="Strength" value={medicine.strength} />
                    <Fact label="Form" value={sentenceCase(medicine.dosageForm)} />
                    <Fact label="Taken" value={sentenceCase(medicine.route)} />
                    <Fact label="Generic name" value={medicine.genericName} />
                    <Fact label="Manufacturer" value={medicine.manufacturer} />
                    <Fact label="Drug class" value={pharmClass} />
                    <Fact label="Type" value={sentenceCase(medicine.productType)} />
                    <Fact label="NDC" value={medicine.productNdc} />
                  </TableBody>
                </Table>

                <Box sx={{ display: "flex", flexWrap: "wrap", gap: 2, mt: 3 }}>
                  {isUser && medicine.active && (
                    <PrimaryButton onClick={() => navigate(`/prescriptions/add/${medicine.id}`)}>
                      + Add prescription
                    </PrimaryButton>
                  )}
                  {label?.dailyMedUrl && (
                    <Button
                      variant="outlined"
                      endIcon={<OpenInNewIcon />}
                      component={Link}
                      href={label.dailyMedUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      Full label on DailyMed
                    </Button>
                  )}
                </Box>
              </Grid>
            </Grid>

            <Divider sx={{ my: 3 }} />
            <Typography variant="caption" color="text.secondary">
              Data from the US FDA (openFDA and DailyMed). It covers products sold in the US and may not
              match local packaging elsewhere. Check with your pharmacist if anything looks different.
            </Typography>
          </Paper>
        )}
      </Container>
    </Box>
  );
}
