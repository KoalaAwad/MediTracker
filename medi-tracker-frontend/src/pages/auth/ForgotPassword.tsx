import { useState } from "react";
import { Link } from "react-router-dom";
import { Alert, Box, Button, TextField, Typography } from "@mui/material";
import AuthLayout from "../../components/auth/AuthLayout";
import { authApi } from "../../api/authApi";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await authApi.forgotPassword(email);
      setSent(res.data.message);
    } catch (err: any) {
      setError(err.response?.data?.error || "Something went wrong, try again");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Forgot password">
      {sent ? (
        <Alert severity="success" sx={{ mb: 3 }}>
          {sent}. The link expires in 30 minutes.
        </Alert>
      ) : (
        <Box component="form" onSubmit={handleSubmit} noValidate>
          <Typography color="text.secondary" sx={{ mb: 2 }}>
            Enter your account email and we'll send you a link to choose a new password.
          </Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <TextField
            required
            fullWidth
            autoFocus
            label="Email Address"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={loading}
          />
          <Button type="submit" fullWidth variant="contained" sx={{ mt: 3, mb: 2 }} disabled={loading || !email}>
            {loading ? "Sending..." : "Send reset link"}
          </Button>
        </Box>
      )}
      <Typography align="center">
        <Link to="/login">Back to login</Link>
      </Typography>
    </AuthLayout>
  );
}
