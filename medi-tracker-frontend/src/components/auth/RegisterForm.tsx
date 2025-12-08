import {
    Avatar,
    Box,
    Button,
    Container,
    CssBaseline,
    Grid,
    TextField,
    Typography,
    Paper,
    styled,
    Alert,
  } from "@mui/material";
  import { LockOutlined } from "@mui/icons-material";
  import { useState } from "react";
  import { Link, useNavigate } from "react-router-dom";
  import { authApi } from "../../api/authApi";

  const Item = styled(Paper)(({ theme }) => ({
    backgroundColor: "#fff",
    ...theme.typography.body2,
    padding: theme.spacing(1),
    textAlign: "center",
    color: theme.palette.text.secondary,
  }));

  const Register = () => {
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleRegister = async (e: React.FormEvent) => {
      e.preventDefault();
      setError("");
      setLoading(true);

      try {
        await authApi.register({ name, email, password });
        navigate('/login');
      } catch (err: any) {
        setError(err.response?.data?.error || err.message || 'Registration failed');
      } finally {
        setLoading(false);
      }
    };

    return (
      <Box
        sx={{
          width: "100%",
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          bgcolor: "background.default",
          px: 2,
        }}
      >
        <CssBaseline />
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

          <Typography variant="h5">Register</Typography>

          {error && (
            <Alert severity="error" sx={{ width: '100%', mt: 2 }}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleRegister} sx={{ mt: 3, width: "100%" }} noValidate>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  name="name"
                  required
                  fullWidth
                  id="name"
                  label="Name"
                  autoFocus
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  disabled={loading}
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  id="email"
                  label="Email Address"
                  name="email"
                  type="email"
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
              {loading ? 'Registering...' : 'Register'}
            </Button>

            <Grid container justifyContent="flex-end">
              <Item>
                <Link to="/login">Already have an account? Login</Link>
              </Item>
            </Grid>
          </Box>
        </Paper>
      </Box>
    );
  };

  export default Register;