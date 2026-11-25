# Precision Clock Resizing, Studio Trifecta Switcher, Settings Reorganization & V4 Engines Plan

This revised plan combines clock resizing fixes (proportional digits with optional Free Stretch and clock-aware widget/notification pushdown) with a cohesive 3-way switcher linking Depth Studio, Preview Studio, and a newly live-previewed Lock Screen Mode. It also declutters Studio Settings and Lock Screen Mode via expandable dropdowns in the requested section order, restricts Liquid Glass exclusively to Frosted Glass OS with polished distinct animations, upgrades the Zero-Lag & Preloading Catcher engine to V4, overhauls the Depth Engine to a higher-accuracy single-subject V4 pipeline (removing 2-subject multi-layer controls), and cleans up all cache and test issues.

## User Review & Critical Decisions

> [!IMPORTANT]
> All confirmed choices from earlier clarification plus every revision directive are unified here. No source files have been modified during planning; execution begins immediately upon approval.

- **Confirmed Decision 1 — Clock Resizing & Zero-Overlap Flow**: Corner handle drags and size sliders scale clock numerals proportionally by default, include an optional **Free Stretch** toggle for independent width/height scaling, and dynamically push all active Lock Screen widgets and notification decks downward using the clock's true scaled bottom edge so nothing ever overlaps or covers clock digits.
- **Confirmed Decision 2 — 3-Way Studio Switcher & Live Lock Preview**: Depth Studio, Preview Studio, and Lock Screen Mode share a seamless 3-way switcher (`Depth` | `Preview` | `Lock`) styled as a **Material 3 Expressive segmented header pill** in Material 3 mode or a **Frosted sliding glass capsule** in Frosted Glass OS mode. Lock Screen Mode gains its own dedicated inline live preview stage showing real-time changes (including animated charging overlays, speed/size adjustments, notification stacking, widgets, and unlock bar styling) without having to open a modal blindly.
- **Confirmed Decision 3 — Clutter-Free Dropdowns, Section Order & Open Essentials**:
  - **Studio Settings** orders *Interface Design System (UI Styles)* and *Studio Typography & Fonts* first, followed **immediately** by *Appearance, Theme (Light/Dark/System) & Navigation Layout (Bottom/Side Bar)* before downstream cards; primary toggles remain open while secondary sliders and dense panels compress into expandable dropdowns.
  - **Lock Screen Mode** keeps core activation and privacy toggles visible at the top while folding dense configuration cards (Clock Sets, Notification Styling, Charging Customizer, Unlock Button Theme, Widget Suite, Permissions, Compatibility) into collapsible dropdown drawers alongside its live preview.
  - **Depth Studio** keeps weight, opacity, and primary active toggles immediately visible rather than burying core depth adjustments.
- **Confirmed Decision 4 — Frosted-Exclusive Liquid Glass, Zero-Lag V4 & Depth V4 Overhaul**:
  - **Liquid Glass** (refraction optics, refractive clock finish, glass tiering, sliding capsule physics) is uniquely scoped to **Frosted Glass OS**, leaving Material 3 with its distinct expressive tonal finishes, Pixel corner brackets, and crisp spring motion curves.
  - **Zero-Lag & Zero-Latency Engine V4** adds an active **Preloading Catcher** (scroll lookahead, hover pre-warming, and studio-to-studio warm bitmap handoff) with unified network/disk cache keys.
  - **Depth Engine V4** completely removes the 2-subject multi-layer system and focuses 100% on an upgraded, ultra-accurate classic **single-subject depth cutout** verified by a full 20-case stress test and end-to-end verification suite.

---

## 1. Overview & Core Concept

- **What It Does**: Eliminates static/broken clock resizing, adds effortless instant transitions across Depth Studio, Preview Studio, and an inline live-previewed Lock Screen Mode, enforces strict visual separation between Material 3 and Frosted Glass OS (keeping Liquid Glass native to Frosted Glass OS), streamlines settings clutter via progressive disclosure dropdowns, and upgrades both performance preloading and single-subject depth extraction to V4.
- **Target Audience / Persona**: Creators and everyday customizers tailoring clock typography, depth overlap, notifications, widgets, and charging animations across dual Material 3 and Frosted Glass OS visual systems.
- **Key Value**: Instant visual feedback in every studio mode, zero overlap over scaled clocks, zero lag across navigation and studio switches, cleaner single-subject depth cutouts, and uncluttered settings screens.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Clock Resizing & Automatic Widget/Notification Pushdown**:
   - In Depth Studio, tapping the clock opens the corner-bracket bounding frame (`48.dp` bottom-left & bottom-right targets).
   - Dragging either bottom corner scales clock numerals smoothly and proportionally in real time without snapping; toggling **Free Stretch** enables independent X/Y stretching across vector faces and Frosted Liquid Glass glyphs.
   - Regardless of clock height or scale factor, Lock Screen widgets and the iOS stacked notification deck automatically flow below the clock's true bottom boundary with safe margin spacing—never clipping or covering the clock.

2. **3-Way Studio Switcher (`Depth` | `Preview` | `Lock`) & Live Lock Preview**:
   - Depth Studio, Preview Studio, and Lock Screen Mode each host the compact 3-way switcher in their header bar.
   - **Depth Studio**: Full depth stage with Weight, Opacity, and primary depth toggles immediately open for fast editing.
   - **Preview Studio**: Full-bleed Home Screen & Lock Screen live mockups sharing all live clock, zoom/pan/filter, and custom photo edits.
   - **Lock Screen Mode**: Features a dedicated live Lock Screen preview pane directly on screen alongside collapsible customization dropdowns. Tweaking charging animation presets, speed, size, accent color, widget states, or notification layouts updates the live preview stage immediately so users never edit blindly. Expanding to full-screen interactive preview remains one tap away.

3. **Decluttered Studio Settings in Clean Order**:
   - Section sequence from top to bottom:
     1. *Interface Design System* (Material 3 vs Frosted Glass OS selector, mode-specific optical dropdown, Dynamic Theming toggle).
     2. *Studio Typography & Fonts* (bundled font family selector + collapsible type scale dropdown).
     3. *Appearance, Theme & Navigation Layout* (Light/Dark/System mode, Bottom/Side/Auto navigation bar selector, AMOLED Pitch Black, and core toggles right up front).
     4. *Collapsible Secondary Sections* (Hardware Material Tier / Glow tuning, Language & Weather, On-Device Folders, Downloads & Resolution, Offline Packs, Cache & Defaults) neatly tucked into expandable dropdown drawers so scrolling feels airy and organized.

### Visual Identity & Theme

- **Strict Dual-System Identity**:
  - *Material 3*: Uses semantic tonal roles (`surfaceContainerHigh`, `primaryContainer`, `secondaryContainer`), crisp Pixel right-angle corner handles, expressive tonal clock finishes (Solid, Gradient Blend, Outline, Glow), responsive damped springs, and an **M3 Expressive Segmented Header Pill** for studio switching.
  - *Frosted Glass OS*: Exclusively owns **Liquid Glass** (shader/height-map chromatic refraction glyphs, optical glass tiers, specular curved corner arcs, liquid custom sliders with zero Material 3 slider components, luminous dark translucent cards, and a **Frosted Sliding Glass Capsule** studio switcher).
- **Polished Animations**:
  - Distinct choreographer presets for Material 3 (crisp zero-overshoot expressive slides and ink-settle springs) versus Frosted Glass OS (buoyant refractive scale-morph springs, sliding capsule thumb physics, smooth notification deck fan-out, and silk-smooth charging pulse/wave loops).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: True Baseline Measurement + Clock-Bottom Driven Layout Solver**
  - *Chosen Approach*: Decouple unscaled `1.0x` clock dimensions from active scale/stretch transforms so bounding math never compounds on itself, and feed the resolved scaled `clockBoxRect.bottomPx` directly (without artificial premature clamping) into widget strip placement and `LockScreenNotificationLayoutSolver`.
  - *Why*: Fixes both problems together—clocks resize reliably across all 33 faces, and enlarging a clock cleanly pushes widgets and stacked notifications downward so nothing obscures the digits.

- **Decision 2: Live Inline Preview + Dropdown Compression in Lock Screen Mode & Settings**
  - *Chosen Approach*: Keep high-frequency master switches and Depth Studio Weight/Opacity controls always open, while collapsing multi-slider/multi-option cards in Studio Settings and Lock Screen Mode into clean accordion dropdowns paired with a real-time interactive preview card at the top/side of Lock Screen Mode.
  - *Why*: Eliminates endless vertical wall-of-switches fatigue while giving immediate visual feedback when tuning charging rings, notification blur/transparency, or unlock button corners.

- **Decision 3: Single-Subject Depth Engine V4 (Removing 2-Subject Multi-Layer)**
  - *Chosen Approach*: Completely remove multi-subject layer stratification, multi-layer compositing helpers, and multi-layer chips/sliders. Upgrade the core hybrid segmentation pipeline to **Depth Engine V4**: higher-detail analysis grid (`420px`), bilateral CIE L*a*b* smoothing, multi-sweep geodesic edge-barrier distance transform, single-foreground ML Kit fusion (when available on device), automated small-island cleanup, and luminance/color-guided sub-pixel matte upsampling tuned around the clock zone.
  - *Why*: Multi-layer stratification added UI clutter and compositing complexity that degraded reliability; focusing all compute budget on one razor-sharp foreground mask makes standard depth overlap cleaner, faster, and dramatically more accurate.

- **Decision 4: Frosted-Only Liquid Glass & Zero-Lag Engine V4**
  - *Chosen Approach*: Scope `Liquid Glass` finish and optical tier controls specifically to `Frosted Glass OS` (gracefully mapping to Material Glow/Blend when switched into Material 3 mode), and upgrade the performance subsystem to **V4** with a scroll/navigation/studio **Preloading Catcher**, unified HTTP/disk caching, filter-aware tab cache keys, and strict non-recycled bitmap checks.
  - *Why*: Preserves unmistakable visual identity between the two themes and removes frame hitches, duplicate full-resolution downloads, stale search caches, and bitmap lifecycle crashes.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                    Shared Studio ViewModel & Trifecta Graph               │
│   Depth Studio ◄────────── StudioSwitchBar (M3 Pill / Frost Capsule) ────► Preview Studio
│        ▲                                                                   ▲
│        └──────────────────────► Lock Screen Mode ◄─────────────────────────┘
│                                 (Inline Live Preview + Dropdown Drawers)   │
│                                                                            │
│   Shared Live State: ClockDesignConfig (+FreeStretch) │ CanvasFraming      │
│                      Active Wallpaper / Gallery URI   │ Warm Bitmaps       │
└───────────────────────────────┬───────────────────────┬────────────────────┘
                                │                       │
            ┌───────────────────▼───┐       ┌───────────▼──────────────────┐
            │ Lock Layout & Clocks  │       │ Studio Settings (Reordered)  │
            │ • Baseline Unscaled   │       │ 1. UI Styles (+Frost Optics) │
            │ • Proportional/Stretch│       │ 2. Studio Fonts & Type       │
            │ • Dynamic Y Pushdown  │       │ 3. Theme + Nav Bar (Btm/Side)│
            │   (Widgets + Stack)   │       │ 4+ Collapsible Dropdowns     │
            └───────────┬───────────┘       └──────────────────────────────┘
                        │
        ┌───────────────┴────────────────────────────────────┐
        ▼                                                    ▼
┌────────────────────────────────────┐   ┌──────────────────────────────────┐
│ Depth Engine V4 (Single-Subject)   │   │ Zero-Lag Engine V4               │
│ • 420px Bilateral L*a*b* Grid      │   │ • Preloading Catcher (Scroll +   │
│ • Geodesic Edge-Barrier + Portrait │   │   Detail-to-Studio + Pill Hover) │
│ • Single-FG ML Kit Fusion          │   │ • Unified HTTP/Disk Keys         │
│ • Clean Sub-Pixel Cutout Upsampler │   │ • !isRecycled & Filter Keys      │
│   (Multi-Layer System Removed)     │   │ • Liquid Glass Shader V4 (Frost) │
└────────────────────────────────────┘   └──────────────────────────────────┘
```

### Data Model & State

- **`ClockDesignConfig`**: Retains `scale`, `widthScale`, `heightScale`, plus `freeStretchEnabled` (default `false` so proportional digit scaling works immediately out of the box, with independent X/Y stretch toggled cleanly when desired).
- **`CanvasFramingState` & Shared ViewModel**: Hoisted across Depth Studio, Preview Studio, and Lock Screen Mode so wallpaper selection, custom gallery photos, clock geometry, dimming, and look filters transfer instantaneously with zero reload or stack bloat.
- **`DepthDetectionResult` (V4)**: Streamlined back to crisp single-subject foreground cutout outputs (`cutoutBitmap`, `depthQualityScore`, `subjectTopFraction`, `recommendedClockTopFraction`, `recommendedClockOverlapFraction`, `engineSource = "CIE L*a*b* Depth V4"` / Hybrid ML Kit V4), removing multi-layer list models completely.
- **Lock Screen Rendering Layout State**: Computes exact scaled/stretched `clockBottomDp = resolvedClockBox.bottomPx / density` and shifts the `StudioWidgetsLockStrip` + `LockScreenNotificationsSection` container strictly below that clearance threshold while adapting notification visible group height smoothly within remaining vertical space above the unlock bar.

### Interactive Component & State Mapping

- **Clock Corner Drag & Free Stretch Toggle**:
  - Bottom-left/right handles update `widthScale` and `heightScale` continuously from stable unscaled baseline dimensions.
  - When `freeStretchEnabled == false`, visual scaling applies balanced proportional magnification/reduction across digits and subtitle pills; when `freeStretchEnabled == true`, X and Y axes stretch independently and pass updated aspect ratios through to Liquid Glass rasterization and hit-test bounds.
- **3-Way Studio Switcher**:
  - *Material 3*: Tonal segmented pill (`Depth` | `Preview` | `Lock`) with `primaryContainer` active chip and clean ink ripples.
  - *Frosted Glass OS*: Animated spring-driven translucent sliding capsule over frosted glass border highlighting the active studio destination.
  - Triggers single-swap route transitions without wiping unsaved ViewModel edits.
- **Lock Screen Mode Live Inline Preview**:
  - Hosts a responsive live Lock Screen frame pinned cleanly in view (side-by-side on wide screens, collapsible/compact interactive preview stage on narrow viewports) wired directly to `chargingAnimation`, `notificationStyle`, `unlockButtonTheme`, and `studioWidgets` draft state—auto-showing live charging rings/waves/beams whenever charging parameters or presets are adjusted.
- **Preloading Catcher V4 & Cache Safety**:
  - Catcher monitors upcoming grid rows, category switches, detail entry, and studio navigation hovers to pre-decode thumbnails and full cutouts into bounded memory caches without duplicate OkHttp requests, invalidates tab metadata by filter fingerprint, and guards all warm/cached bitmaps against recycling.

### Key Sequences & Integrations

- **Liquid Glass Scoping**: Setting or activating **Frosted Glass OS** unlocks Liquid Glass clock finishes, specular optical tuning, and refractive capsule physics; selecting **Material 3** showcases expressive Material finishes and automatically normalizes frosted-only controls cleanly without visual leakage.
- **Verification & 20-Case Stress Plan**:
  - Fix the compact viewport widget strip layout/clickability failure (`studio_widget_music_expand_area`), stabilize offline Robolectric execution so the full test suite passes cleanly, and run an updated **20-test Depth Engine V4 & Depth Studio Stress Suite** alongside dedicated checks for proportional clock resizing, Free Stretch, clock-to-widget/notification non-overlap pushdown, 3-way studio switching, live inline Lock Screen charging preview, Settings section ordering, Frosted-only Liquid Glass scoping, and V4 Preloading Catcher safety.
