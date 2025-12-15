import { Button, ButtonProps } from "@mui/material";
import { styled } from "@mui/material/styles";

// Reusable primary button with darker, more noticeable blue
const StyledPrimaryButton = styled(Button)(({ theme }) => ({
  backgroundColor: "#1565c0", // Darker blue (Material-UI blue[800])
  color: "#ffffff",
  padding: "10px 24px",
  fontSize: "0.95rem",
  fontWeight: 600,
  textTransform: "none",
  borderRadius: "8px",
  boxShadow: "0 2px 4px rgba(21, 101, 192, 0.3)",
  transition: "all 0.2s ease-in-out",
  "&:hover": {
    backgroundColor: "#0d47a1", // Even darker on hover (Material-UI blue[900])
    boxShadow: "0 4px 8px rgba(21, 101, 192, 0.4)",
    transform: "translateY(-1px)",
  },
  "&:active": {
    transform: "translateY(0)",
    boxShadow: "0 1px 2px rgba(21, 101, 192, 0.3)",
  },
  "&:disabled": {
    backgroundColor: "#90caf9",
    color: "#ffffff",
    opacity: 0.6,
  },
}));

// Reusable secondary button (outlined style)
const StyledSecondaryButton = styled(Button)(({ theme }) => ({
  borderColor: "#1565c0",
  color: "#1565c0",
  padding: "10px 24px",
  fontSize: "0.95rem",
  fontWeight: 600,
  textTransform: "none",
  borderRadius: "8px",
  borderWidth: "2px",
  "&:hover": {
    borderColor: "#0d47a1",
    backgroundColor: "rgba(21, 101, 192, 0.04)",
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

