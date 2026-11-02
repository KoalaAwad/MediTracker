import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Alert, Box, Button, TextField, Typography } from "@mui/material";
import AuthLayout from "../../components/auth/AuthLayout";
import { authApi } from "../../api/authApi";

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get("token");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(token ? null : "This link is missing its token.");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (password.length < 8) {
      setError("Password must be at least 8 characters");
      return;
    }
    if (password !== confirm) {
      setError("Passwords don't match");
      return;
    }
    setLoading(true);
    try {
      const res = await authApi.resetPassword(token!, password);
      setDone(res.data.message);
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not reset password");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Choose a new password">
      {done ? (
        <>
          <Alert severity="success" sx={{ mb: 3 }}>{done}</Alert>
          <Button component={Link} to="/login" variant="contained" fullWidth>
            Go to login
          </Button>
        </>
      ) : (
        <Box component="form" onSubmit={handleSubmit} noValidate>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <TextField
            required
            fullWidth
            autoFocus
            label="New password"
            type="password"
            helperText="At least 8 characters"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={loading || !token}
            sx={{ mb: 2 }}
          />
          <TextField
            required
            fullWidth
            label="Confirm new password"
            type="password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            disabled={loading || !token}
          />
          <Button type="submit" fullWidth variant="contained" sx={{ mt: 3, mb: 2 }} disabled={loading || !token}>
            {loading ? "Saving..." : "Set new password"}
          </Button>
          <Typography align="center">
            <Link to="/forgot-password">Request a new link</Link>
          </Typography>
        </Box>
      )}
    </AuthLayout>
  );
}
