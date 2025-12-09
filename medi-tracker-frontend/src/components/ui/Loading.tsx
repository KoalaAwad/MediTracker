import { Box, CircularProgress, Typography } from "@mui/material";

export default function Loading({ fullScreen = true, label }: { fullScreen?: boolean; label?: string }) {
  return (
    <Box
      sx={{
        minHeight: fullScreen ? "100vh" : undefined,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: 2,
        py: fullScreen ? 0 : 2,
      }}
    >
      <CircularProgress />
      {label && (
        <Typography component="span" variant="body2" color="text.secondary">
          {label}
        </Typography>
      )}
    </Box>
  );
}

