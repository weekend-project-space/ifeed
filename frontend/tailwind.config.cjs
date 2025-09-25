const colorVar = (variable) => `rgb(var(${variable}) / <alpha-value>)`;

/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{vue,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: colorVar('--md-primary'),
          foreground: colorVar('--md-on-primary')
        },
        surface: {
          DEFAULT: colorVar('--md-surface'),
          variant: colorVar('--md-surface-variant'),
          container: colorVar('--md-surface-container')
        },
        outline: colorVar('--md-outline'),
        text: {
          DEFAULT: colorVar('--md-text'),
          secondary: colorVar('--md-text-secondary'),
          muted: colorVar('--md-text-muted')
        },
        inverse: {
          DEFAULT: colorVar('--md-inverse'),
          on: colorVar('--md-on-inverse')
        },
        danger: colorVar('--md-danger')
      },
      boxShadow: {
        'md-elevated': '0 12px 40px -18px rgba(15, 23, 42, 0.4)'
      },
      borderRadius: {
        'xl-2': '1.75rem'
      },
      backdropBlur: {
        xs: '2px'
      }
    }
  },
  plugins: [require('@tailwindcss/forms')]
};
