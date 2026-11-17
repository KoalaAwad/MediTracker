import { Box } from "@mui/material";
import RegisterForm from "../../components/auth/RegisterForm";

export default function RegisterPage() {

  return (
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
        <RegisterForm />
      </Box>
  );
}
