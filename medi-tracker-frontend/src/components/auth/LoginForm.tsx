import { useState } from "react";
    import { Link } from "react-router-dom";
    import {
      Avatar,
      Box,
      Button,
      TextField,
      Typography,
      Paper,
      Alert,
      Grid,
      styled,
    } from "@mui/material";
    import { LockOutlined } from "@mui/icons-material";

    const Item = styled(Paper)(({ theme }) => ({
      backgroundColor: "#fff",
      ...theme.typography.body2,
      padding: theme.spacing(1),
      textAlign: "center",
      color: theme.palette.text.secondary,
    }));

    interface LoginFormProps {
      onSubmit: (email: string, password: string) => Promise<void>;
    }

    export default function LoginForm({ onSubmit }: LoginFormProps) {
      const [email, setEmail] = useState("");
      const [password, setPassword] = useState("");
      const [error, setError] = useState<string | null>(null);
      const [loading, setLoading] = useState(false);

      const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
          await onSubmit(email, password);
        } catch (err: any) {
          setError(err.response?.data?.error || err.message || "Incorrect username or password");
        } finally {
          setLoading(false);
        }
      };

      return (
        <Paper
          elevation={3}
          sx={{
            width: "100%",
            maxWidth: 420,
            p: 4,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
          }}
        >
          <Avatar sx={{ m: 1, bgcolor: "primary.light" }}>
            <LockOutlined />
          </Avatar>

          <Typography variant="h5">Login</Typography>

          {error && (
            <Alert severity="error" sx={{ width: "100%", mt: 2 }}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 3, width: "100%" }} noValidate>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  id="email"
                  label="Email Address"
                  name="email"
                  type="email"
                  autoFocus
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={loading}
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  name="password"
                  label="Password"
                  type="password"
                  id="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={loading}
                />
              </Grid>
            </Grid>

            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={loading}
            >
              {loading ? "Logging in..." : "Login"}
            </Button>

            <Grid container justifyContent="flex-end">
              <Item>
                <Link to="/register">Don't have an account? Register</Link>
              </Item>
            </Grid>
          </Box>
        </Paper>
      );
    }