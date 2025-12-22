import { Box, CssBaseline, ThemeProvider } from "@mui/material";
import { getTheme } from "../../lib/theme";
import RegisterForm from "../../components/auth/RegisterForm";

export default function RegisterPage() {
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
        <RegisterForm />
      </Box>
    </ThemeProvider>
  );
}
