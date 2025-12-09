import { Button } from "./button";
import { ComponentProps } from "react";

interface PrimaryButtonProps extends ComponentProps<typeof Button> {
  children: React.ReactNode;
}

interface SecondaryButtonProps extends ComponentProps<typeof Button> {
  children: React.ReactNode;
}

export function PrimaryButton({ children, className, ...props }: PrimaryButtonProps) {
  return (
    <Button
      variant="default"
      className={className}
      {...props}
    >
      {children}
    </Button>
  );
}

export function SecondaryButton({ children, className, ...props }: SecondaryButtonProps) {
  return (
    <Button
      variant="outline"
      className={className}
      {...props}
    >
      {children}
    </Button>
  );
}

