import { IconButton, Tooltip } from '@mui/material';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';
import { useThemeStore } from '../../zustand/themeStore';

export default function ThemeToggle() {
  const mode = useThemeStore((s) => s.mode);
  const toggleTheme = useThemeStore((s) => s.toggleTheme);

  return (
    <Tooltip title={mode === 'light' ? 'Switch to Dark Mode' : 'Switch to Light Mode'}>
      <IconButton onClick={toggleTheme} color="inherit">
        {mode === 'light' ? <Brightness4Icon /> : <Brightness7Icon />}
      </IconButton>
    </Tooltip>
  );
}

