import { Box, Container, Typography, IconButton, Tooltip } from "@mui/material";
import { useNavigate, useLocation } from "react-router-dom";
import HomeIcon from "@mui/icons-material/Home";
import ThemeToggle from "./ThemeToggle";
import { PrimaryButton } from "./StyledButton";
import { useAuthStore } from "../../zustand/authStore";

interface NavbarProps {
  showHomeIcon?: boolean;
}

export default function Navbar({ showHomeIcon = true }: NavbarProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  // Don't show home icon on dashboard itself
  const isDashboard = location.pathname === "/dashboard";

  return (
    <Box sx={{ bgcolor: "background.paper", borderBottom: 1, borderColor: "divider" }}>
      <Container maxWidth="xl">
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", height: 64 }}>
          {/* Left side - Logo + Home Icon */}
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <Typography
              variant="h6"
              component="h1"
              color="text.primary"
              sx={{ fontWeight: 700, cursor: "pointer" }}
              onClick={() => navigate("/dashboard")}
            >
              MediTracker
            </Typography>
            {showHomeIcon && !isDashboard && isAuthenticated && (
              <Tooltip title="Go to Dashboard">
                <IconButton
                  onClick={() => navigate("/dashboard")}
                  size="small"
                  sx={{ color: "primary.main" }}
                >
                  <HomeIcon />
                </IconButton>
              </Tooltip>
            )}
          </Box>

          {/* Right side - Theme Toggle + Auth Buttons */}
          <Box sx={{ display: "flex", gap: 1, alignItems: "center" }}>
            {isAuthenticated && <ThemeToggle />}

            {isAuthenticated && user ? (
              <>
                <PrimaryButton
                  size="small"
                  variant="outlined"
                  onClick={() => navigate("/profile")}
                >
                  My Profile
                </PrimaryButton>
                <PrimaryButton
                  size="small"
                  onClick={logout}
                  sx={{
                    bgcolor: "error.main",
                    "&:hover": { bgcolor: "error.dark" }
                  }}
                >
                  Logout
                </PrimaryButton>
              </>
            ) : (
              <>
                <PrimaryButton
                  size="small"
                  variant="outlined"
                  onClick={() => navigate("/login")}
                >
                  Login
                </PrimaryButton>
                <PrimaryButton
                  size="small"
                  onClick={() => navigate("/register")}
                >
                  Register
                </PrimaryButton>
              </>
            )}
          </Box>
        </Box>
      </Container>
    </Box>
  );
}

