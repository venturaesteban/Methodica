# Scholarly Sanctuary Design System

This module applies the editorial design language for Methodica.

## Included

- Theme tokens in `theme/`:
  - `Color.kt`: Scholarly palette and layered surfaces.
  - `Type.kt`: editorial typography scale.
  - `Theme.kt`: fixed visual identity (dynamic color disabled by default).
- Reusable components in `components/`:
  - `ScholarlyButton.kt`
  - `ScholarlyTextField.kt`
  - `ScholarlyChip.kt`
  - `ScholarlySurface.kt`

## Adoption Rules

- No 1px section dividers; use surface shifts and spacing.
- Prefer `surfaceContainer*` tones to define hierarchy.
- Use gradient primary CTA and tertiary for completion states.
- Keep body text in `onSurfaceVariant` when possible to reduce strain.

## Notes

- Typeface mapping currently uses system fallback families.
- When font files are available, map Manrope/Inter in `Type.kt`.

