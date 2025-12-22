import { Button, ButtonProps } from "@mui/material";
import { styled } from "@mui/material/styles";

// Modern ShadCN-style primary button (consistent blue across themes)
const StyledPrimaryButton = styled(Button)(({ theme }) => ({
  backgroundColor: "#1565c0", // Same blue in both modes
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
    backgroundColor: "#0d47a1", // Same darker blue on hover
    boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
    transform: "translateY(-1px)",
  },
  "&:active": {
    transform: "translateY(0)",
    boxShadow: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
  },
  "&:disabled": {
    backgroundColor: "#90caf9",
    color: "#ffffff",
    opacity: 0.5,
  },
  "&:focus-visible": {
    outline: "2px solid #1565c0",
    outlineOffset: "2px",
  },
}));

// Modern ShadCN-style secondary button (consistent blue across themes)
const StyledSecondaryButton = styled(Button)(({ theme }) => ({
  backgroundColor: "transparent",
  borderColor: theme.palette.mode === 'dark' ? "rgba(255, 255, 255, 0.23)" : "rgba(0, 0, 0, 0.23)",
  color: "#1565c0", // Same blue in both modes
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
    borderColor: "#1565c0",
    backgroundColor: theme.palette.mode === 'dark'
      ? "rgba(21, 101, 192, 0.08)"
      : "rgba(21, 101, 192, 0.04)",
    boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
  },
  "&:active": {
    transform: "translateY(0)",
  },
  "&:focus-visible": {
    outline: "2px solid #1565c0",
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

