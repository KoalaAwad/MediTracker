import { Button, ButtonProps } from "@mui/material";
import { styled } from "@mui/material/styles";

// Modern ShadCN-style primary button (theme-aware)
const StyledPrimaryButton = styled(Button)(({ theme }) => ({
  backgroundColor: theme.palette.mode === 'dark' ? theme.palette.primary.main : "#1565c0",
  color: "#ffffff",
  padding: "10px 20px",
  fontSize: "0.875rem",
  fontWeight: 500,
  textTransform: "none",
  borderRadius: "6px",
  border: "none",
  boxShadow: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
  transition: "all 0.15s ease",
  "&:hover": {
    backgroundColor: theme.palette.mode === 'dark' ? theme.palette.primary.dark : "#0d47a1",
    boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
    transform: "translateY(-1px)",
  },
  "&:active": {
    transform: "translateY(0)",
    boxShadow: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
  },
  "&:disabled": {
    backgroundColor: theme.palette.mode === 'dark' ? "#42a5f5" : "#90caf9",
    color: "#ffffff",
    opacity: 0.5,
  },
  "&:focus-visible": {
    outline: `2px solid ${theme.palette.primary.main}`,
    outlineOffset: "2px",
  },
}));

// Modern ShadCN-style secondary button (outlined, theme-aware)
const StyledSecondaryButton = styled(Button)(({ theme }) => ({
  backgroundColor: "transparent",
  borderColor: theme.palette.mode === 'dark' ? "rgba(255, 255, 255, 0.23)" : "rgba(0, 0, 0, 0.23)",
  color: theme.palette.mode === 'dark' ? theme.palette.primary.main : "#1565c0",
  padding: "10px 20px",
  fontSize: "0.875rem",
  fontWeight: 500,
  textTransform: "none",
  borderRadius: "6px",
  borderWidth: "1px",
  borderStyle: "solid",
  boxShadow: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
  transition: "all 0.15s ease",
  "&:hover": {
    borderColor: theme.palette.mode === 'dark' ? theme.palette.primary.main : "#0d47a1",
    backgroundColor: theme.palette.mode === 'dark'
      ? "rgba(144, 202, 249, 0.08)"
      : "rgba(21, 101, 192, 0.04)",
    boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
  },
  "&:active": {
    transform: "translateY(0)",
  },
  "&:focus-visible": {
    outline: `2px solid ${theme.palette.primary.main}`,
    outlineOffset: "2px",
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

