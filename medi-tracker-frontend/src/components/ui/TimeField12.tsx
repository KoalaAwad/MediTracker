import { Box, MenuItem, TextField } from "@mui/material";

// A 12-hour time picker (hour, minute, AM/PM). The browser's own time input follows the
// computer's clock setting and can show 24-hour time, which this app never does.
// value/onChange use "HH:mm" (24-hour), which is what the server stores.

const HOURS = Array.from({ length: 12 }, (_, i) => i + 1);
const MINUTES = Array.from({ length: 12 }, (_, i) => i * 5);

function split(value: string) {
  const [h, m] = value.split(":").map(Number);
  const hour24 = Number.isNaN(h) ? 8 : h;
  const minute = Number.isNaN(m) ? 0 : m;
  return { hour: hour24 % 12 === 0 ? 12 : hour24 % 12, minute, pm: hour24 >= 12 };
}

function join(hour: number, minute: number, pm: boolean) {
  const hour24 = (hour % 12) + (pm ? 12 : 0);
  return `${String(hour24).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
}

interface Props {
  label?: string;
  value: string;
  onChange: (value: string) => void;
}

export default function TimeField12({ label = "Time", value, onChange }: Props) {
  const { hour, minute, pm } = split(value);
  // a saved time that isn't on the 5-minute grid (e.g. 08:07) still shows correctly
  const minutes = MINUTES.includes(minute) ? MINUTES : [...MINUTES, minute].sort((a, b) => a - b);

  return (
    <Box role="group" aria-label={label} sx={{ display: "flex", gap: 0.5 }}>
      <TextField
        select
        label={label}
        value={hour}
        onChange={(e) => onChange(join(Number(e.target.value), minute, pm))}
        sx={{ width: 82 }}
      >
        {HOURS.map((h) => (
          <MenuItem key={h} value={h}>
            {h}
          </MenuItem>
        ))}
      </TextField>
      <TextField
        select
        aria-label="Minutes"
        value={minute}
        onChange={(e) => onChange(join(hour, Number(e.target.value), pm))}
        sx={{ width: 76 }}
      >
        {minutes.map((m) => (
          <MenuItem key={m} value={m}>
            {String(m).padStart(2, "0")}
          </MenuItem>
        ))}
      </TextField>
      <TextField
        select
        aria-label="AM or PM"
        value={pm ? "PM" : "AM"}
        onChange={(e) => onChange(join(hour, minute, e.target.value === "PM"))}
        sx={{ width: 80 }}
      >
        <MenuItem value="AM">AM</MenuItem>
        <MenuItem value="PM">PM</MenuItem>
      </TextField>
    </Box>
  );
}
