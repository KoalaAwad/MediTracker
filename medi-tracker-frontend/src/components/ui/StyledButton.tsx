import { Button, ButtonProps } from "@mui/material";

// Thin wrappers so pages share one look; colours and corners come from the theme

interface PrimaryButtonProps extends Omit<ButtonProps, "variant"> {
  children: React.ReactNode;
}

interface SecondaryButtonProps extends Omit<ButtonProps, "variant"> {
  children: React.ReactNode;
}

export function PrimaryButton({ children, ...props }: PrimaryButtonProps) {
  return (
    <Button variant="contained" color="primary" {...props}>
      {children}
    </Button>
  );
}

export function SecondaryButton({ children, ...props }: SecondaryButtonProps) {
  return (
    <Button variant="outlined" color="inherit" {...props} sx={{ borderColor: "divider", ...props.sx }}>
      {children}
    </Button>
  );
}
