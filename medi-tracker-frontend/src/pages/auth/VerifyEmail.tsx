import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Alert, Button } from "@mui/material";
import AuthLayout from "../../components/auth/AuthLayout";
import Loading from "../../components/ui/Loading";
import { authApi } from "../../api/authApi";

export default function VerifyEmail() {
  const [params] = useSearchParams();
  const token = params.get("token");
  const [status, setStatus] = useState<"loading" | "ok" | "error">(token ? "loading" : "error");
  const [message, setMessage] = useState(token ? "" : "This link is missing its token.");
  // StrictMode runs effects twice in dev; the token is single-use, so only send it once
  const sent = useRef(false);

  useEffect(() => {
    if (!token || sent.current) return;
    sent.current = true;
    authApi
      .verifyEmail(token)
      .then((res) => {
        setStatus("ok");
        setMessage(res.data.message);
      })
      .catch((err) => {
        setStatus("error");
        setMessage(err.response?.data?.error || "Verification failed");
      });
  }, [token]);

  return (
    <AuthLayout title="Verify email">
      {status === "loading" && <Loading label="Verifying..." />}
      {status !== "loading" && (
        <>
          <Alert severity={status === "ok" ? "success" : "error"} sx={{ mb: 3 }}>
            {message}
            {status === "error" && (
              <>
                <br />
                Log in to request a new link.
              </>
            )}
          </Alert>
          <Button component={Link} to="/login" variant="contained" fullWidth>
            Go to login
          </Button>
        </>
      )}
    </AuthLayout>
  );
}
