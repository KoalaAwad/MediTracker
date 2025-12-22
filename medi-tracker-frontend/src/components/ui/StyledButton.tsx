import { Button, ButtonProps } from "@mui/material";
import { styled } from "@mui/material/styles";

// Reusable primary button with darker, more noticeable blue (theme-aware)
const StyledPrimaryButton = styled(Button)(({ theme }) => ({
  backgroundColor: theme.palette.mode === 'dark' ? theme.palette.primary.main : "#1565c0",
  color: "#ffffff",
  padding: "10px 24px",
  fontSize: "0.95rem",
  fontWeight: 600,
  textTransform: "none",
  borderRadius: "8px",
  boxShadow: theme.palette.mode === 'dark'
    ? "0 2px 4px rgba(144, 202, 249, 0.3)"
    : "0 2px 4px rgba(21, 101, 192, 0.3)",
  transition: "all 0.2s ease-in-out",
  "&:hover": {
    backgroundColor: theme.palette.mode === 'dark' ? theme.palette.primary.dark : "#0d47a1",
    boxShadow: theme.palette.mode === 'dark'
      ? "0 4px 8px rgba(144, 202, 249, 0.4)"
      : "0 4px 8px rgba(21, 101, 192, 0.4)",
    transform: "translateY(-1px)",
  },
  "&:active": {
    transform: "translateY(0)",
    boxShadow: theme.palette.mode === 'dark'
      ? "0 1px 2px rgba(144, 202, 249, 0.3)"
      : "0 1px 2px rgba(21, 101, 192, 0.3)",
  },
  "&:disabled": {
    backgroundColor: theme.palette.mode === 'dark' ? "#42a5f5" : "#90caf9",
    color: "#ffffff",
    opacity: 0.6,
  },
}));

// Reusable secondary button (outlined style, theme-aware)
const StyledSecondaryButton = styled(Button)(({ theme }) => ({
  borderColor: theme.palette.mode === 'dark' ? theme.palette.primary.main : "#1565c0",
  color: theme.palette.mode === 'dark' ? theme.palette.primary.main : "#1565c0",
  padding: "10px 24px",
  fontSize: "0.95rem",
  fontWeight: 600,
  textTransform: "none",
  borderRadius: "8px",
  borderWidth: "2px",
  "&:hover": {
    borderColor: theme.palette.mode === 'dark' ? theme.palette.primary.dark : "#0d47a1",
    backgroundColor: theme.palette.mode === 'dark'
      ? "rgba(144, 202, 249, 0.08)"
      : "rgba(21, 101, 192, 0.04)",
    borderWidth: "2px",
  },
}));

interface PrimaryButtonProps extends Omit<ButtonProps, "variant"> {
  children: React.ReactNode;
}

interface SecondaryButtonProps extends Omit<ButtonProps, "variant"> {
  children: React.ReactNode;
}

export function PrimaryButton({ children, ...props }: PrimaryButtonProps) {
  return (
    <StyledPrimaryButton variant="contained" {...props}>
      {children}
    </StyledPrimaryButton>
  );
}

export function SecondaryButton({ children, ...props }: SecondaryButtonProps) {
  return (
    <StyledSecondaryButton variant="outlined" {...props}>
      {children}
    </StyledSecondaryButton>
  );
}

