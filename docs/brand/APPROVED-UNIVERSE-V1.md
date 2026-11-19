# DeviceCare KI — approved Universe visual identity v1.0

**Owner decision:** 2026-11-19. **Primary repo:** `SvenKulessa/DeviceCare-KI`. **Baseline:** original generated brand-board, logo and four-screen mockup approved in chat. The SVG assets here are **implementation-safe scalable redraws**, *not pixel-identical replacements of the original PNG renders*.

## Versioned design reference (authoritative for all new screens)

- [Brand board](../../assets/branding/approved/brand-board.svg) — seven colors and spatial motif language.
- [Logo](../../assets/branding/approved/devicecare-ki-logo.svg) — shield / smartphone / AI circuitry / orbital ring.
- [UI mockup](../../assets/branding/approved/ui-mockup.svg) — overview, system & Google check, battery, KI assistant. **All values on mocks are illustrative; no observed device data.**
- The Compose theme, launcher icon and Inspection hero are the runtime derivatives. The application ID, Firebase and safety constraints are unchanged.

| Token | Hex | Usage |
|---|---|---|
| Deep Space Navy | `#0B1435` | primary backdrop |
| Starlight Gold | `#F5C24B` | brand / premium emphasis, not a security verdict |
| Plasma Cyan | `#00E6FF` | primary action, focus and orbit |
| Signal Red | `#FF3B4D` | risk / error only |
| Aurora Violet | `#8A3DFF` | AI / secondary visual |
| Steel Silver | `#B8C3D3` | secondary text / metallic |
| Clean White | `#FFFFFF` | foreground |

Semantic green is reserved for actual VERIFIED evidence, and amber for attention/NOT_PROVEN. Do not color synthetic or unmeasured data as successful. Use Material3 role-based colors (accessible light/dark variants); no fixed glows behind small text. Brand visual identity is original cinematic space opera, **no franchise-owned insignia, typography or trademark usage**.

### Source-image provenance

The owner-approved raster renders (including the alternative early board) from the design conversation, with local SHA-256 for integrity, are:

| Reference image | SHA-256 |
|---|---|
| `devicecare_ki_futuristisches_schildlogo.png` | `22194aaaa3922c56b73c536a099212fb910421953e71e3f9d55088eb2533b084` |
| `devicecare_ki_markenboard_im_sci_fi_stil.png` | `111ee9185bdcf06462db6b93934f8529a3f82b56b253ad5fd0837cc3e1754e3b` |
| `devicecare_ki_markenboard_im_weltraum.png` (earlier variant) | `39d57438d4e2ed1aefaf19134d7b2f72d61bee605c9e05418314f39bf9d387b1` |
| `devicecare_ki_kosmische_android_sicherheit.png` | `132dd9484128d36ba72a6f54ad46a5c92cf24d0d66b43dca2da32379d2872cc2` |

**Asset transfer note:** the original high-resolution PNG render bytes are not included in this initial git commit; preserve these SHA hashes when they are uploaded into `assets/branding/approved/source/`. The SVGs are fully versioned and used as normative *implementation sources* immediately. Never claim the PNGs were checked into Git when they were not.

## UX/accessibility & safety

- Scale for 320 / 390 / 1440 CSS-equivalent widths; Android Compose uses dp and font scaling.
- TalkBack labels for meaningful controls; decorative artwork has `contentDescription=null`.
- Tap targets at least 48 dp; WCAG AA contrast for text and essential indicators.
- Honor system typography / large font / reduced motion; never rely on neon glow as sole status.
- Do not import mock numbers (e.g. 96%, patch dates or battery claims) into production.
- Existing stored palette preferences remain readable as *legacy user overrides*, but Universe is the default/new design authority.
- Keep local-only evidence, user initiated scans and Firebase opt-in boundaries in AGENTS.md.

## Acceptance before merge/release

Unit + Robolectric tests, Android lint, Trivy, signed debug build & SHA evidence; 320/390/1440 responsive screenshots and physical Android-15 large-font/TalkBack checks remain separately required for final hardware acceptance. Debug APK is not a production release. Rollback: revert branding PR; DataStore preference values remain backwards compatible.
