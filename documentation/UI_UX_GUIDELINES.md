# UI / UX Guidelines

The visual identity is **fresh green, warm cream, natural surroundings and premium 3D illustration**. It is taken from the approved homepage screenshot and applies to the public site, the student dashboard and the admin dashboard alike.

## 1. Design tokens (`static/css/theme.css`)

Every colour, font, radius, shadow and spacing value is a CSS variable in `theme.css`. No other stylesheet defines raw colours for new UI. The admin area has no palette of its own.

| Token | Hex | Use |
|---|---|---|
| `--color-forest` | #153E35 | Headings, dark surfaces (footer, admin sidebar, CTA) |
| `--color-leaf` | #194C36 | Gradients and hover on dark surfaces |
| `--color-action` | #438B25 | Primary buttons, active icons, the accent word in the hero |
| `--color-lime` | #B9D96B | Accent: active admin nav, highlights, focus glow |
| `--color-ivory` | #F7F5ED | Page background |
| `--color-cream` | #FFFDF5 | Cards, header, dialogs |
| `--color-terracotta` | #D77D61 | Supporting accent: counters, saved state, giveaway badge |
| `--color-text` | #202C26 | Body text |
| `--color-muted` | #78827A | Placeholders, icons, large secondary text |
| `--color-border` | #E6E8DF | Hairlines and card borders |

**Accessibility adjustments** (same hues, darker shades, added as derived tokens):
- `--color-muted` reaches 3.6:1 on ivory. That's fine for large text, icons and placeholders, but not for body-size text, so body-size secondary text uses `--color-text-secondary` #5A645C (5.6:1).
- Green text on light backgrounds uses `--color-action-strong` #2F6A18 (≥ 4.5:1). `#438B25` is used for fills and large text.
- White on `#438B25` is 4.2:1. That passes AA for large or bold text, so button labels are bold and at least 16 px.

## 2. Typography

- **Manrope** (variable, self-hosted) for headings, the logo wordmark and numbers in stat cards
- **Inter** (variable, self-hosted) for body text and UI
- **Caveat** only for the tagline "Reuse Today for a Greener Tomorrow"
- Display size: `clamp(2.5rem, 4.6vw, 4.35rem)`, weight 800, tracking −0.035em

## 3. Layout and shape

- Floating rounded header: 80 px tall, 30 px radius, blurred cream background, sticky
- Content width 1340 px plus fluid gutters (16–48 px)
- Radii: 8 / 12 / 16 / 24 / 32 px and pill. Cards 24 px; inputs 12 px; buttons are pills.
- Soft green-tinted shadows (`rgba(21, 62, 53, …)`). There are no hard black shadows.
- Student and admin pages use the same app shell (sidebar plus content). The admin sidebar is forest green so the area is easy to recognise while sharing every other token.

## 4. Components (`components.css`)

Buttons (primary, secondary, dark, lime, danger, ghost; sm/lg/block/icon; built-in loading spinner), status badges (one tone per status value, shared by every list), listing-type badges, cards, form fields with inline errors, choice cards, photo uploader, alerts, empty, error and skeleton states, responsive tables, tabs, pagination, modal dialogs, toasts, avatars, item cards, stat cards, conversation bubbles and FAQ accordion.

## 5. Interaction rules

- Every list has a **loading skeleton**, an **empty state** with a helpful next action, and an **error state** with Retry. This lives in `ui.createListController`.
- Destructive or moderation actions open a **confirmation dialog**. Rejections, suspensions and change requests **require a reason**.
- Successful actions show a **toast**. Form errors appear **next to the field**, mapped from the API's `fieldErrors`.
- No decorative buttons: every button has a handler or a real link. There are no `href="#"` links.
- Motion is limited to short fades and lifts (≤ 0.7 s). `prefers-reduced-motion` disables all of it.

## 6. Responsive behaviour

| Width | Behaviour |
|---|---|
| ≤ 1320 px | Tighter nav spacing; exchange pill shows only the icon and count |
| ≤ 1160 px | Explore filters become a bottom sheet |
| ≤ 1060 px | Nav collapses into a menu. The hero stacks with the art below the copy. Stats show 2×2. The student sidebar is replaced by bottom navigation, and the admin sidebar becomes a horizontal strip. |
| ≤ 760 px | Tables become labelled cards; request rows stack |
| ≤ 640 px | Full-width hero buttons, single-column forms, 2-column item grid, toasts above the bottom nav |

Touch targets are at least 40 px. The category cards on the hero scale with the illustration using container query units, so they never overlap the copy.

## 7. Accessibility checklist

Semantic landmarks (`header`, `nav`, `main`, `aside`, `footer`) and a skip link. Every input has a `<label>`. Errors are linked with `aria-describedby` and `aria-invalid`. Visible `:focus-visible` rings appear everywhere. Dialogs trap focus, close on Esc and return focus afterwards. Toasts use `aria-live`. Images have alt text, and decorative ones have empty alt or `aria-hidden`. Every icon is `aria-hidden` with a text label or `aria-label` beside it.
