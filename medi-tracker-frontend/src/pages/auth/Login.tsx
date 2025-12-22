import { Box, CssBaseline, ThemeProvider } from "@mui/material";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../zustand/authStore";
import { getTheme } from "../../lib/theme";
import LoginForm from "../../components/auth/LoginForm";

export default function Login() {
  const login = useAuthStore((s) => s.login);
  const isLoading = useAuthStore((s) => s.isLoading);
  const navigate = useNavigate();

  const handleLogin = async (email: string, password: string) => {
    await login({ email, password });
    navigate("/dashboard");
  };

  // Force light theme for login page
  const lightTheme = getTheme('light');

  return (
    <ThemeProvider theme={lightTheme}>
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
        <LoginForm onSubmit={handleLogin} />
      </Box>
    </ThemeProvider>
  );
}
