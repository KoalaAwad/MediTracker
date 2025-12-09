import { Box, CssBaseline } from "@mui/material";
import { useNavigate } from "react-router-dom";
// import { useAuth } from "../../context/AuthContext";
import { useAuthStore } from "../../zustand/authStore";
import LoginForm from "../../components/auth/LoginForm";

export default function Login() {
  const login = useAuthStore((s) => s.login);
  const isLoading = useAuthStore((s) => s.isLoading);
  const navigate = useNavigate();

  const handleLogin = async (email: string, password: string) => {
    await login({ email, password });
    navigate("/dashboard");
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
      <LoginForm onSubmit={handleLogin} />
    </Box>
  );
}
