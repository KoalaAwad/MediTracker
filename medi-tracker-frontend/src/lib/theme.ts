import { createTheme } from '@mui/material/styles';

// "Calm" design: one teal accent for actions and "take now", red only for missed doses,
// everything else neutral. Sharp corners everywhere except info cards (see INFO_CARD).

const palettes = {
  light: {
    bg: '#f4f6f8',
    card: '#ffffff',
    line: '#dfe4ea',
    text: '#14202b',
    muted: '#5d6b78',
    accent: '#0f766e',
    accentHover: '#0b5f58',
    onAccent: '#ffffff',
    accentSoft: '#e3f3f1',
    danger: '#dc2626',
    dangerSoft: '#fde4e4',
    doneBg: '#f0f2f5',
  },
  dark: {
    bg: '#0e1418',
    card: '#151d23',
    line: '#2a353f',
    text: '#e6edf2',
    muted: '#8fa0ad',
    accent: '#2dd4bf',
    accentHover: '#5eead4',
    onAccent: '#052e2b',
    accentSoft: '#11302d',
    danger: '#f87171',
    dangerSoft: '#3a1717',
    doneBg: '#11171c',
  },
};

export type CalmColors = (typeof palettes)['light'];

declare module '@mui/material/styles' {
  interface Theme {
    calm: CalmColors;
  }
  interface ThemeOptions {
    calm?: CalmColors;
  }
}

/** The one place corners are rounded: panels that show information about something. */
export const INFO_CARD = { borderRadius: '14px' } as const;

export const getTheme = (mode: 'light' | 'dark') => {
  const c = palettes[mode];
  return createTheme({
    calm: c,
    palette: {
      mode,
      primary: { main: c.accent, dark: c.accentHover, contrastText: c.onAccent },
      error: { main: c.danger },
      background: { default: c.bg, paper: c.card },
      text: { primary: c.text, secondary: c.muted },
      divider: c.line,
    },
    shape: { borderRadius: 0 },
    typography: {
      fontFamily: '"Inter Variable", Inter, system-ui, -apple-system, "Segoe UI", sans-serif',
      h4: { fontWeight: 700, letterSpacing: '-0.01em' },
      h5: { fontWeight: 700, letterSpacing: '-0.01em' },
      h6: { fontWeight: 600 },
      button: { textTransform: 'none', fontWeight: 600 },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: { body: { backgroundColor: c.bg } },
      },
      // flat boxes with a hairline border instead of shadows
      MuiPaper: {
        defaultProps: { elevation: 0 },
        styleOverrides: {
          root: { backgroundImage: 'none' },
          outlined: { borderColor: c.line },
          elevation0: { border: `1px solid ${c.line}` },
          elevation1: { border: `1px solid ${c.line}`, boxShadow: 'none' },
          elevation2: { border: `1px solid ${c.line}`, boxShadow: 'none' },
          elevation3: { border: `1px solid ${c.line}`, boxShadow: 'none' },
        },
      },
      MuiCard: { defaultProps: { elevation: 0 } },
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: { root: { borderRadius: 0 } },
      },
      MuiChip: { styleOverrides: { root: { borderRadius: 0 } } },
      MuiAlert: { styleOverrides: { root: { borderRadius: 0 } } },
      MuiDialog: { styleOverrides: { paper: { borderRadius: 0 } } },
      MuiMenu: { styleOverrides: { paper: { border: `1px solid ${c.line}` } } },
      MuiTableCell: { styleOverrides: { root: { borderColor: c.line } } },
    },
  });
};
