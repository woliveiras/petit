# Petit Design System

Visual reference for Petit UI tokens, components, and interaction rules.

## Theme Direction

Theme name: Modern Cozy Editorial.

Design goals:

- Warm, welcoming tone instead of clinical style
- Clear hierarchy and strong readability
- Subtle depth with tonal surfaces

## Color Foundations

Primary palette:

- `primary`: `#3A1444`
- `primaryContainer`: `#522B5B`
- `surface`: `#FCF9F4`
- `surfaceContainer`: `#F0EDE9`
- `onSurfaceVariant`: `#4D444D`
- `outlineVariant`: `#D0C3CD`

Guidelines:

- Prefer tonal separation over hard 1px borders
- Use spacing rhythm (`8dp`, `12dp`, `16dp`) for section structure
- Reserve gradients for primary CTAs and hero regions

## Typography

Font families:

- Headings: Plus Jakarta Sans
- Body/UI labels: Be Vietnam Pro

Hierarchy guidance:

- Headlines: primary tone
- Body copy: on-surface variant
- Use Material 3 typography roles; avoid ad-hoc `fontSize`

## Shape and Elevation

- Main cards: rounded corners around `24dp` to `32dp`
- Small controls: around `16dp` to `24dp`
- Keep shadows soft and tinted to palette, not pure black

## Components

### Buttons

- Primary: filled or gradient, high prominence
- Secondary: lower emphasis, no heavy hard border
- Destructive: use semantic error color

### Cards and Lists

- Prefer spacing-based grouping over divider-heavy layouts
- Use avatar + title + support text for list rows
- Keep touch targets at least `48dp`

### Inputs

- Filled surfaces with clear labels
- Focus state must be highly visible
- Validation states use semantic color + readable text

### Navigation

- Bottom navigation can use a floating style with clear active indicator
- FAB should communicate main creation action

## Accessibility Baselines

- Touch target >= `48dp`
- Sufficient contrast in light and dark themes
- Explicit content descriptions for icon-only actions
- Logical focus order and grouped semantics

## Layout and Spacing

Recommended spacing scale:

- `4dp`, `8dp`, `12dp`, `16dp`, `24dp`, `32dp`

Guideline:

- Avoid dense packing in health/task screens
- Keep vertical rhythm consistent across sections

## Dark Theme

- Keep same semantic mapping as light theme
- Avoid pure black backgrounds
- Verify all status colors for contrast and readability

## Material 3 Mapping

Use Material 3 tokens whenever possible:

- `primary`, `secondary`, `tertiary`
- `surface` + container levels
- `outline` and `outlineVariant`
- semantic `error`

## Implementation Notes

- Centralize colors in theme files.
- Reuse component styles instead of one-off variants.
- Add visual regression checks for key screens when changing tokens.
