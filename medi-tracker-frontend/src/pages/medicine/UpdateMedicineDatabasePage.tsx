import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Container,
  Typography,
  Paper,
  Button,
  Alert,
  LinearProgress,
  List,
  ListItem,
  ListItemText,
} from "@mui/material";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import ErrorIcon from "@mui/icons-material/Error";
import { medicineApi } from "../../api/medicineApi";
import { useAuthStore } from "../../zustand/authStore";
import { PrimaryButton } from "../../components/ui/StyledButton";

export default function UpdateMedicineDatabasePage() {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<any | null>(null);
  const navigate = useNavigate();
  const token = useAuthStore((s) => s.token);

  const validateJsonFile = (file: File): string | null => {
    // Check file extension
    if (!file.name.toLowerCase().endsWith(".json")) {
      return "File must be a .json file";
    }

    // Check file size (150MB max)
    const maxSize = 150 * 1024 * 1024; // 150MB
    if (file.size > maxSize) {
      return "File size exceeds 150MB limit";
    }

    // Check file size minimum (must be at least 10 bytes)
    if (file.size < 10) {
      return "File is too small or empty";
    }

    return null;
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    setError(null);
    setResult(null);

    if (!selectedFile) {
      setFile(null);
      return;
    }

    const validationError = validateJsonFile(selectedFile);
    if (validationError) {
      setError(validationError);
      setFile(null);
      return;
    }

    setFile(selectedFile);
  };

  const handleUpload = async () => {
    if (!file || !token) return;

    setUploading(true);
    setError(null);
    setResult(null);

    try {
      // Read file as text
      const text = await file.text();

      // Validate JSON structure
      let jsonData;
      try {
        jsonData = JSON.parse(text);
      } catch (e) {
        throw new Error("Invalid JSON format");
      }

      // Validate structure (must have 'results' array)
      if (!jsonData.results || !Array.isArray(jsonData.results)) {
        throw new Error(
          "Invalid JSON structure: must contain a 'results' array"
        );
      }

      if (jsonData.results.length === 0) {
        throw new Error("JSON file contains no drug records");
      }

      if (jsonData.results.length > 50000) {
        throw new Error(
          `Too many records: ${jsonData.results.length} (max 50,000)`
        );
      }

      // Send to backend
      const response = await medicineApi.importMedicines(jsonData, token);
      setResult(response.data);
    } catch (err: any) {
      if (err.response?.data?.error) {
        setError(err.response.data.error);
      } else if (err.message) {
        setError(err.message);
      } else {
        setError("Failed to upload file");
      }
    } finally {
      setUploading(false);
    }
  };

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
            <Typography variant="h6">
              MediTracker - Update Medicine Database
            </Typography>
            <Box sx={{ display: "flex", gap: 2 }}>
              <Button variant="outlined" onClick={() => navigate("/medicine")}>
                Back to Medicine List
              </Button>
            </Box>
          </Box>
        </Container>
      </Box>

      <Container maxWidth="md" sx={{ py: 4 }}>
        <Paper elevation={2} sx={{ p: 4 }}>
          <Typography variant="h4" gutterBottom>
            Update Medicine Database
          </Typography>

          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            Upload an OpenFDA Drugs@FDA JSON file to update the medicine
            database. Existing medicines will be preserved (no duplicates).
          </Typography>

          <Alert severity="info" sx={{ mb: 3 }}>
            <strong>File Requirements:</strong>
            <List dense>
              <ListItem>
                <ListItemText primary="• File format: .json" />
              </ListItem>
              <ListItem>
                <ListItemText primary="• Maximum size: 150MB" />
              </ListItem>
              <ListItem>
                <ListItemText primary="• Must contain 'results' array with drug records" />
              </ListItem>
              <ListItem>
                <ListItemText primary="• Maximum 50,000 records per file" />
              </ListItem>
            </List>
          </Alert>

          {error && (
            <Alert severity="error" sx={{ mb: 3 }} icon={<ErrorIcon />}>
              {error}
            </Alert>
          )}

          {result && (
            <Alert severity="success" sx={{ mb: 3 }} icon={<CheckCircleIcon />}>
              <Typography variant="h6" gutterBottom>
                Upload Complete!
              </Typography>
              <List dense>
                <ListItem>
                  <ListItemText
                    primary={`✓ Created: ${result.created} medicines`}
                  />
                </ListItem>
                <ListItem>
                  <ListItemText
                    primary={`⊘ Skipped: ${result.skipped} (duplicates or invalid)`}
                  />
                </ListItem>
                {result.skippedNoName > 0 && (
                  <ListItem>
                    <ListItemText
                      primary={`  - ${result.skippedNoName} had no name`}
                    />
                  </ListItem>
                )}
                {result.skippedError > 0 && (
                  <ListItem>
                    <ListItemText
                      primary={`  - ${result.skippedError} had errors`}
                    />
                  </ListItem>
                )}
              </List>
            </Alert>
          )}

          <Box sx={{ mb: 3 }}>
            <input
              accept=".json"
              style={{ display: "none" }}
              id="file-upload"
              type="file"
              onChange={handleFileChange}
              disabled={uploading}
            />
            <label htmlFor="file-upload">
              <Button
                variant="outlined"
                component="span"
                startIcon={<CloudUploadIcon />}
                disabled={uploading}
                fullWidth
                sx={{ py: 2 }}
              >
                {file ? file.name : "Choose JSON File"}
              </Button>
            </label>
          </Box>

          {file && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              File size: {(file.size / 1024 / 1024).toFixed(2)} MB
            </Typography>
          )}

          {uploading && (
            <Box sx={{ mb: 3 }}>
              <LinearProgress />
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ mt: 1 }}
              >
                Uploading and processing... This may take several minutes for
                large files.
              </Typography>
            </Box>
          )}

          <PrimaryButton
            onClick={handleUpload}
            disabled={!file || uploading}
            fullWidth
            sx={{ py: 1.5 }}
          >
            {uploading ? "Uploading..." : "Upload and Update Database"}
          </PrimaryButton>

          <Alert severity="warning" sx={{ mt: 3 }}>
            <strong>Warning:</strong> This operation will add new medicines to
            the database. Existing medicines with the same name will be skipped
            automatically.
          </Alert>
        </Paper>
      </Container>
    </Box>
  );
}

