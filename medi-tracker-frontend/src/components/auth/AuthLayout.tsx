import { Box, CssBaseline, Paper, ThemeProvider, Typography } from "@mui/material";
import { getTheme } from "../../lib/theme";

// Centered light-theme card used by the email verification and password reset pages
export default function AuthLayout({ title, children }: { title: string; children: React.ReactNode }) {
  const lightTheme = getTheme("light");

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
        <Paper elevation={3} sx={{ width: "100%", maxWidth: 420, p: 4 }}>
          <Typography variant="h5" align="center" sx={{ mb: 2 }}>
            {title}
          </Typography>
          {children}
        </Paper>
      </Box>
    </ThemeProvider>
  );
}
