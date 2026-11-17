import { useState } from "react";
import {
  Box,
  Button,
  Container,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Typography,
} from "@mui/material";
import { useNavigate, useLocation } from "react-router-dom";
import MenuIcon from "@mui/icons-material/Menu";
import AddIcon from "@mui/icons-material/Add";
import ThemeToggle from "./ThemeToggle";
import { PrimaryButton, SecondaryButton } from "./StyledButton";
import { useAuthStore } from "../../zustand/authStore";

interface NavLink {
  label: string;
  to: string;
  // other paths that count as this section, e.g. a medicine's details page
  match: string[];
}

export default function Navbar() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const [menuOpen, setMenuOpen] = useState(false);

  const isAdmin = user?.role.includes("ADMIN") ?? false;
  const links: NavLink[] = [
    { label: "Home", to: "/dashboard", match: ["/dashboard"] },
    { label: "Medicines", to: "/medicine", match: ["/medicine"] },
    { label: "Prescriptions", to: "/prescriptions", match: ["/prescriptions"] },
    ...(isAdmin ? [{ label: "Users", to: "/admin/users", match: ["/admin"] }] : []),
  ];
  const isActive = (link: NavLink) => link.match.some((m) => pathname === m || pathname.startsWith(m + "/"));

  const go = (to: string) => {
    setMenuOpen(false);
    navigate(to);
  };

  // a prescription is always added for a specific medicine, so this starts at the medicine search
  const addPrescription = () => go("/medicine");

  const signedIn = isAuthenticated && user;

  return (
    <Box component="header" sx={{ bgcolor: "background.paper", borderBottom: 1, borderColor: "divider", position: "sticky", top: 0, zIndex: 1100 }}>
      <Container maxWidth="xl">
        <Box sx={{ display: "flex", alignItems: "center", height: 60, gap: 2 }}>
          {signedIn && (
            <IconButton
              aria-label="Open menu"
              onClick={() => setMenuOpen(true)}
              sx={{ display: { xs: "inline-flex", lg: "none" }, ml: -1 }}
            >
              <MenuIcon />
            </IconButton>
          )}

          <Typography
            variant="h6"
            component="a"
            href={signedIn ? "/dashboard" : "/login"}
            onClick={(e: React.MouseEvent) => {
              e.preventDefault();
              go(signedIn ? "/dashboard" : "/login");
            }}
            sx={{ fontWeight: 700, fontSize: "1.05rem", color: "text.primary", textDecoration: "none", whiteSpace: "nowrap", display: "flex", alignItems: "center", gap: 1 }}
          >
            <Box component="span" sx={{ width: 18, height: 18, bgcolor: "primary.main", display: "inline-block" }} />
            MediTracker
          </Typography>

          {signedIn && (
            <Box component="nav" sx={{ display: { xs: "none", lg: "flex" }, alignItems: "center", gap: 0.5, ml: 2 }}>
              {links.map((link) => (
                <Button
                  key={link.to}
                  onClick={() => go(link.to)}
                  aria-current={isActive(link) ? "page" : undefined}
                  sx={{
                    color: isActive(link) ? "primary.main" : "text.secondary",
                    fontWeight: isActive(link) ? 700 : 500,
                    textTransform: "none",
                    whiteSpace: "nowrap",
                    fontSize: "0.95rem",
                    borderRadius: 0,
                    height: 60,
                    px: 1.5,
                    borderBottom: 2,
                    borderColor: isActive(link) ? "primary.main" : "transparent",
                  }}
                >
                  {link.label}
                </Button>
              ))}
            </Box>
          )}

          <Box sx={{ flex: 1 }} />

          <Box sx={{ display: "flex", gap: 1, alignItems: "center" }}>
            {signedIn ? (
              <>
                <PrimaryButton
                  size="small"
                  startIcon={<AddIcon />}
                  onClick={addPrescription}
                  sx={{ display: { xs: "none", sm: "inline-flex" }, whiteSpace: "nowrap" }}
                >
                  Add prescription
                </PrimaryButton>
                <ThemeToggle />
                <Box sx={{ display: { xs: "none", lg: "flex" }, gap: 1 }}>
                  <SecondaryButton size="small" sx={{ whiteSpace: "nowrap" }} onClick={() => go("/profile")}>
                    My Profile
                  </SecondaryButton>
                  <Button size="small" color="inherit" onClick={logout} sx={{ color: "text.secondary" }}>
                    Logout
                  </Button>
                </Box>
              </>
            ) : (
              <>
                <SecondaryButton size="small" onClick={() => go("/login")}>
                  Login
                </SecondaryButton>
                <PrimaryButton size="small" onClick={() => go("/register")}>
                  Register
                </PrimaryButton>
              </>
            )}
          </Box>
        </Box>
      </Container>

      {/* phone menu */}
      <Drawer anchor="left" open={menuOpen} onClose={() => setMenuOpen(false)}>
        <Box sx={{ width: 260 }} role="navigation">
          <Typography variant="h6" sx={{ fontWeight: 700, px: 2, py: 2 }}>
            MediTracker
          </Typography>
          <Divider />
          <List>
            {links.map((link) => (
              <ListItemButton key={link.to} selected={isActive(link)} onClick={() => go(link.to)}>
                <ListItemText primary={link.label} />
              </ListItemButton>
            ))}
            <ListItemButton onClick={addPrescription}>
              <ListItemText primary="+ Add prescription" primaryTypographyProps={{ color: "primary", fontWeight: 600 }} />
            </ListItemButton>
          </List>
          <Divider />
          <List>
            <ListItemButton selected={pathname === "/profile"} onClick={() => go("/profile")}>
              <ListItemText primary="My Profile" />
            </ListItemButton>
            <ListItemButton
              onClick={() => {
                setMenuOpen(false);
                logout();
              }}
            >
              <ListItemText primary="Logout" primaryTypographyProps={{ color: "error" }} />
            </ListItemButton>
          </List>
        </Box>
      </Drawer>
    </Box>
  );
}
