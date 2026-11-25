# Backdrop V4: Zero Crash Engine V1, Zero-Lag V4, Infinix XOS Support & Studio Trifecta Plan

This revised plan introduces the brand-new **Zero Crash Engine V1**—a dedicated system-wide crash prevention engine built to eliminate application crashes across all devices, memory profiles, and runtime environments. It refines the **Zero-Lag & Zero-Latency Engines V4** for ultra-smooth performance on any Android device from 2GB RAM budget phones to 120Hz flagships, adds dedicated native support and diagnostics for **Infinix XOS Launcher** (Transsion ecosystem), overhauls the **Depth Engine to pure on-device CIE L*a*b* V4** with a 3-second watchdog and single razor-sharp cutout, provides proportional clock resizing with optional **Free Stretch** and dynamic widget/notification pushdown, delivers a 3-way **Depth | Preview | Lock** studio switcher with an inline live-previewed Lock Screen Mode, reorders settings cleanly into collapsible drawers, and scopes Liquid Glass exclusively to Frosted Glass OS.

## User Review & Critical Decisions

> [!IMPORTANT]
> All user requirements from earlier turns and the latest revision instructions are synthesized below. Execution will begin as soon as this plan is approved.

- **Confirmed Decision 1 — Brand-New Zero Crash Engine V1**: A dedicated, ultra-resilient engine initialized at application startup that intercepts uncaught exceptions across all threads, protects coroutine jobs with fault-tolerant exception handlers, and traps OutOfMemoryError (OOM) before native bitmap allocations crash the VM. Every sub-engine (Depth, Liquid Glass, Widgets, Overlay, Notifications, Weather) is wrapped in safe execution boundaries that gracefully degrade to clean fallbacks without crashing the app process or showing system error dialogs.
- **Confirmed Decision 2 — Ultra-Refined Zero Lag and Zero Latency Engines V4**: Eliminates frame drops, memory spikes, and lag across all devices (Android 7 through 16, 2GB low-RAM to 16GB flagship devices). Features hardware-adaptive tiering, pre-allocated zero-allocation drawing matrices, non-blocking asynchronous coroutine pipelines with eager cancellation on rapid slider drags, and an active **Preloading Catcher V4** that pre-warms catalog bitmaps and studio transitions on background IO threads.
- **Confirmed Decision 3 — Native Support & Diagnostics for Infinix XOS Launcher**: Deep integration for Infinix and Transsion devices (Infinix XOS, Tecno HiOS, itelOS) added to `LockScreenCompatibilityManager`. Detects Transsion XOS launchers (`com.transsion.XOSLauncher`, `com.transsion.hilauncher`, etc.), provides step-by-step diagnostic guidance and one-tap deep links into Infinix Phone Master (`com.transsion.phonemanager`), Auto-start permissions, "Display over other apps", and Transsion Power Master auto-freeze battery whitelist.
- **Confirmed Decision 4 — Pure On-Device CIE L*a*b* V4 Depth Engine**: Fully removes Google Play Services ML Kit Subject Segmentation client and external dependencies to prevent native library and model download failures. Operates 100% on-device using a 420px coarse grid, bilateral CIE L*a*b* color filtering, and geodesic edge-barrier propagation backed by an animated multi-stage scanning pill (*"Sampling Color Space"* -> *"Tracing Geodesic Barriers"* -> *"Synthesizing Edge Matte"*) and a strict 3-second safety watchdog timer.
- **Confirmed Decision 5 — Single Razor-Sharp Primary Subject Cutout**: Focuses all memory and compute power on a single razor-sharp foreground cutout, completely eliminating the unstable 2-subject multi-layer system, duplicate bitmap allocations, and UI stratification clutter.
- **Confirmed Decision 6 — Proportional Clock Resizing, Free Stretch & Zero-Overlap Pushdown**: Corner handles (`48.dp` bottom-left & bottom-right touch targets) resize clock digits proportionally by default. An optional **Free Stretch** toggle enables independent X/Y aspect scaling. Lock Screen widgets and notification decks dynamically measure the resolved bottom coordinate of the clock (`clockBottomDp`) and push downward with safe spacing so no widget or notification ever covers clock numerals.
- **Confirmed Decision 7 — 3-Way Studio Switcher (`Depth` | `Preview` | `Lock`) & Live Lock Preview**: Unifies Depth Studio, Preview Studio, and Lock Screen Mode with an instant header switcher (Material 3 Segmented Pill or Frosted Sliding Capsule). Lock Screen Mode gains a dedicated inline live preview stage reflecting charging animations (rings, waves, beams, speed, size, color), widget states, and notification stacks in real time without blind editing.
- **Confirmed Decision 8 — Reordered Collapsible Settings & Frosted-Only Liquid Glass**:
  - Studio Settings section sequence: (1) Interface Design System, (2) Studio Typography & Fonts, (3) Appearance, Theme & Navigation Layout, followed by collapsible accordion drawers for secondary settings.
  - Liquid Glass finish and optical refraction controls are strictly exclusive to **Frosted Glass OS**, while Material 3 utilizes expressive tonal finishes, Glow, and Pixel corner brackets.

---

## 1. Overview & Core Concept

- **What It Does**: Provides absolute crash-free stability via the new Zero Crash Engine V1, buttery-smooth zero-latency interactions on every Android device, native lock screen compatibility for Infinix XOS Launcher, deterministic on-device single-subject depth cutouts, precision proportional clock resizing with zero widget overlap, a shared 3-way studio switcher with live Lock Screen preview, and decluttered progressive settings.
- **Target Audience / Persona**: Users across all Android OEMs (including Infinix, Samsung, Xiaomi, Pixel, OnePlus, OPPO, and tablets) who demand high-fidelity clock customization, depth layering, charging effects, and lock screen personalization with zero crashes and zero lag.
- **Key Value**: 100% crash immunity, zero stutter on any device tier, instant visual feedback across all three studio modes, flawless Infinix XOS overlay coexistence, and an uncluttered modern UI.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Zero Crash & Graceful Recovery Experience**:
   - If an unexpected error, memory pressure spike, or corrupted image occurs at any point, **Zero Crash Engine V1** silently intercepts the issue.
   - The UI never terminates or freezes; it gracefully displays a clean fallback canvas or non-blocking notification, allowing the user to continue customizing uninterrupted.

2. **Infinix XOS Lock Screen Setup & Coexistence**:
   - When an Infinix or Transsion device is detected, Lock Screen Mode displays a dedicated **Infinix XOS Diagnostics Card** with tailored guidance.
   - One-tap quick action buttons link directly to XOS settings (Phone Master Auto-Start, Display Over Other Apps, and Power Master Battery Exemption).
   - Once granted, the lock screen overlay coexists cleanly with XOS Launcher without system UI flicker or keyguard lockups.

3. **Crash-Free Single-Subject Depth Studio Scan**:
   - Opening Depth Studio initiates pure on-device CIE L*a*b* scanning with zero external network or Play Services calls.
   - The status indicator displays animated multi-stage progress: *"Sampling Color Space"* -> *"Tracing Geodesic Barriers"* -> *"Synthesizing Edge Matte"*.
   - A single high-precision foreground cutout emerges smoothly behind the clock within milliseconds. If 3 seconds elapse, the safety watchdog gracefully completes with a fallback horizon.

4. **Proportional Clock Resizing & Zero-Overlap Widget Pushdown**:
   - In Depth Studio, tapping the clock activates corner-bracket handles.
   - Dragging resizes clock digits proportionally in real time without jitter.
   - Users can toggle **Free Stretch** in the Clock panel for independent width/height scaling across vector and Liquid Glass faces.
   - In Lock Screen Mode and live overlay, widgets and notifications automatically clear the true bottom edge of the clock (`clockBottomDp + 16.dp`).

5. **3-Way Studio Switcher (`Depth` | `Preview` | `Lock`)**:
   - Positioned in the header of each studio mode:
     - *Material 3*: Semantic tonal segmented pill with ink ripple feedback.
     - *Frosted Glass OS*: Translucent sliding glass capsule with spring motion.
   - Active wallpaper, photo URI, clock configuration, and canvas framing stay synced in memory across all three modes without reloads.

6. **Lock Screen Mode with Live Inline Preview**:
   - Features an inline live preview stage showcasing real-time charging animations (Pulse Ring, Hyper Beam, Sonic Wave), speeds, sizes, accent colors, widget configurations, and notification card fans.
   - High-frequency master switches remain visible at the top, while dense configuration cards fold into clean collapsible dropdown drawers.

7. **Decluttered Studio Settings in Strict Order**:
   - Ordered strictly from top to bottom:
     1. *Interface Design System* (M3 vs Frosted Glass OS, dynamic theming).
     2. *Studio Typography & Fonts* (bundled font families & scales).
     3. *Appearance, Theme & Navigation Layout* (Light/Dark/System, Bottom/Side/Auto nav bar, AMOLED pitch black).
     4. *Collapsible Secondary Drawers* (Hardware tiers, Weather & Locale, Cache & Defaults).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dedicated Zero Crash Engine V1 vs. Standard Try/Catch Blocks**
  - *Chosen Approach*: Build a centralized, proactive crash interception engine with:
    1. Global uncaught exception handler catching unhandled thread exceptions and keeping the main loop alive.
    2. Bitmap OOM allocation guard pre-checking JVM and native heap headroom before allocating large textures.
    3. CoroutineExceptionHandler wrappers for all background jobs.
    4. Compose-level Error Boundary restoring stable screen states on unexpected rendering exceptions.
  - *Why*: Standard localized try/catch blocks miss native memory crashes, asynchronous thread failures, and unhandled coroutine cancellations. A dedicated engine guarantees universal crash immunity across all device types.

- **Decision 2: Refined Zero-Lag V4 Engine on Every Device**
  - *Chosen Approach*: Implement dynamic 3-tier hardware profiling (Low RAM <= 3GB, Balanced, High-End), pre-allocated zero-allocation drawing matrices, non-blocking asynchronous coroutine pipelines with eager cancellation on rapid slider drags, and an active **Preloading Catcher V4** that pre-warms catalog bitmaps and studio transitions on background IO threads.
  - *Why*: Eliminates frame drops, memory spikes, and lag across all devices from 2GB RAM budget phones to 120Hz flagships.

- **Decision 3: Dedicated Support for Infinix XOS Launcher**
  - *Chosen Approach*: Add `OemProfile.INFINIX_XOS` to `LockScreenCompatibilityManager` with custom manufacturer/brand detection (`infinix`, `transsion`, `tecno`), XOS-specific intent targets for Transsion Phone Master (`com.transsion.phonemanager`), Auto-start permissions, and Power Master background battery whitelist.
  - *Why*: Transsion devices (Infinix/Tecno) employ aggressive proprietary background killers and overlay permission gates; dedicated diagnostics and deep-links ensure 100% reliable overlay execution without user frustration.

- **Decision 4: Pure On-Device CIE L*a*b* V4 vs. Play Services ML Kit**
  - *Chosen Approach*: Remove all Google Play Services ML Kit Subject Segmentation dependencies from code and Gradle. Rely solely on our deterministic, pure Kotlin CIE L*a*b* color-space conversion, bilateral 3x3 smoothing, and geodesic edge-barrier distance transform with a 3-second watchdog timer.
  - *Why*: Guarantees zero crashes on all devices and emulators regardless of Play Services presence or model availability.

- **Decision 5: Single-Subject Cutout vs. Multi-Layer Stratification**
  - *Chosen Approach*: Completely remove 2-subject multi-layer stratification and layer chips. Focus 100% on a single crisp foreground cutout.
  - *Why*: Eliminates native OOM spikes from allocating multiple 1280x2800 bitmasks simultaneously and delivers a significantly sharper, more reliable depth cutout.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                          Zero Crash Engine V1                             │
│  • Global UncaughtExceptionHandler       • OOM Allocation Guard           │
│  • Coroutine Exception Boundary          • Compose UI Safe Boundary       │
│  • Safe Canvas & Bitmap Lifecycle        • Hardware Health Monitor        │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │ Protects All Subsystems
┌─────────────────────────────────────▼─────────────────────────────────────┐
│                    Shared Studio ViewModel & Trifecta Graph               │
│   Depth Studio ◄────────── StudioSwitchBar (M3 / Frosted) ────────► Lock Screen Mode
│        ▲                                                            (Live Preview)
│        └─────────────────────► Preview Studio ◄───────────────────────────┘
│   Shared State: ClockDesignConfig (+FreeStretch) │ CanvasFramingState      │
│                 Active Wallpaper / Custom Photo  │ Warm Bitmaps Cache     │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │
            ┌─────────────────────────┴─────────────────────────┐
            ▼                                                   ▼
┌─────────────────────────────────────┐   ┌─────────────────────────────────────┐
│ Zero-Lag & Preload Catcher V4       │   │ Lock Screen & XOS Compatibility     │
│ • Universal Tiering (Low/Mid/High)  │   │ • Infinix XOS Launcher Detection    │
│ • Zero-Allocation Drawing Passes    │   │ • Transsion Phone Master Intents    │
│ • Background Lookahead Pre-warming  │   │ • Dynamic Widget/Notification       │
│ • Eager Coroutine Cancellation      │   │   Downward Push (clockBottomDp)     │
│ • Low-RAM Memory Pressure Trimmer   │   │ • Live Inline Charging Preview      │
└─────────────────────────────────────┘   └─────────────────────────────────────┘
            │                                                   │
            ▼                                                   ▼
┌─────────────────────────────────────┐   ┌─────────────────────────────────────┐
│ Depth Engine V4 (Pure On-Device)    │   │ Studio Settings (Reordered)         │
│ • Pure Kotlin CIE L*a*b* Pipeline   │   │ 1. Interface Design System (UI)     │
│ • 420px Bilateral Color Smoothing   │   │ 2. Studio Typography & Fonts        │
│ • Geodesic Edge-Barrier Solver      │   │ 3. Theme + Nav Bar (Bottom/Side)    │
│ • 3-Second Safety Watchdog          │   │ 4. Collapsible Secondary Drawers    │
│ • Single Crisp Foreground Cutout    │   │ • Frosted-Exclusive Liquid Glass    │
└─────────────────────────────────────┘   └─────────────────────────────────────┘
```

### Data Model & State Updates

- **`ZeroCrashEngine` API**:
  - `init(application: Application)`: Registers global uncaught exception handlers and lifecycle callbacks.
  - `runSafely(tag: String, fallback: T, block: () -> T): T`: Traps all throwables (including Errors and OOMs) and returns fallback.
  - `runSuspendSafely(tag: String, fallback: T, block: suspend () -> T): T`: Coroutine-safe wrapper respecting cancellation.
  - `safeCreateBitmap(width: Int, height: Int, config: Bitmap.Config): Bitmap?`: Pre-checks heap headroom before allocating.
  - `guardBitmap(bitmap: Bitmap?): Bitmap?`: Validates non-null, non-recycled, positive dimensions, software config.
- **`OemProfile.INFINIX_XOS`**:
  - `displayName = "Infinix XOS (Transsion)"`
  - `baseStatus = CompatibilityStatus.PARTIALLY_SUPPORTED`
  - `guidance = "Supported on Infinix XOS. Enable 'Display Over Other Apps', allow 'Lock Screen Display' in Phone Master > App Management, and exempt Backdrop in Power Master auto-freeze."`
- **`DepthStudioUiState` (V4)**:
  - `LoadingFullBitmap`
  - `ScanningSubject(stage: ScanStage, fullBitmap: Bitmap)` where `ScanStage` represents `COLOR_SAMPLING`, `BARRIER_TRACING`, or `MATTE_SYNTHESIS`.
  - `Ready(key: String, fullBitmap: Bitmap, result: DepthDetectionResult)`
  - `EngineError(key: String, fullBitmap: Bitmap?, message: String)`
  - `BitmapError(message: String)`
- **`ClockDesignConfig`**:
  - `scale: Float = 1.0f`
  - `widthScale: Float = 1.0f`
  - `heightScale: Float = 1.0f`
  - `freeStretchEnabled: Boolean = false`
  - `depthOverlapEnabled: Boolean = true`
- **`LockScreenLayoutSolver`**:
  - `computeWidgetClearanceY(clockBottomPx, density, minMarginDp)` dynamically shifts widgets and notifications below `clockBottomPx / density + 16.dp`.

### Key Implementation Steps

1. **Build Brand-New `ZeroCrashEngine` (`com.example.engine.crash.ZeroCrashEngine`)**:
   - Create singleton engine with global `UncaughtExceptionHandler`, OOM allocation guards, coroutine boundaries, and safe bitmap validators.
   - Initialize in `MainActivity` and wire across `DepthStudioViewModel`, `DepthDetectionEngine`, `LiquidGlassClockEngine`, `LockScreenOverlayService`, and `BackdropLockActivity`.
   - Implement `ZeroCrashErrorBoundary` composable wrapping the primary UI hierarchy.
2. **Refine Zero-Lag & Zero-Latency Engines to V4**:
   - In `ZeroLagPerformanceEngine.kt`: add hardware-adaptive concurrency limits (`Semaphore(2)` for low-RAM devices, `Semaphore(4)` for high-end), pre-allocated zero-allocation matrix buffers, and non-blocking IO pre-warming.
   - Refactor image loading and canvas operations to eliminate allocations inside high-frequency draw loops.
3. **Infinix XOS Launcher Support**:
   - In `LockScreenCompatibilityManager.kt`: add `OemProfile.INFINIX_XOS`, Transsion detection rules (`infinix`, `transsion`, `tecno`), and intent builders for Transsion Phone Master / Auto-Start permissions.
   - In `LockScreenCompatibilityScreen.kt`: render the dedicated Infinix XOS card with one-tap deep-link buttons.
   - In `LockScreenOverlayService.kt` & `BackdropLockActivity.kt`: implement graceful screen-on/lock lifecycle handlers tuned for XOS Launcher.
4. **Pure On-Device CIE L*a*b* V4 Depth Engine**:
   - In `DepthDetectionEngine.kt`: remove all `com.google.mlkit` imports, classes, and calls.
   - Implement 420px coarse grid, bilateral 3x3 smoothing, Sobel edge magnitude, and multi-sweep geodesic barrier propagation with a `withTimeoutOrNull(3000L)` watchdog.
   - Strip out 2-subject multi-layer stratification and layer models, outputting a single razor-sharp primary cutout.
   - In `app/build.gradle.kts`: remove/comment out `libs.play.services.mlkit.subject.segmentation`.
5. **Proportional Clock Resizing & Zero-Overlap Layout**:
   - Update `ClockBoundingOverlay.kt`: scale numerals proportionally by default from bottom-left/right handles, with optional Free Stretch toggle for independent X/Y stretching.
   - In `SharedLockScreenRenderer.kt` and `DepthStudioScreen.kt`: feed resolved `clockBoxRect.bottomPx` directly to push widgets and notifications downward without overlap.
6. **3-Way Studio Switcher & Inline Live Preview**:
   - Add unified `StudioSwitchBar` to Depth Studio, Preview Studio, and Lock Screen Mode.
   - Build responsive inline live Lock Screen preview in `LockScreenModeScreen.kt` with real-time charging animations (rings, waves, beams, speeds, sizes, colors) and collapsible accordion drawers.
7. **Reorder Studio Settings & Frosted-Only Liquid Glass**:
   - In `SettingsPanelScreen.kt`: sequence sections as (1) Interface Design System, (2) Studio Typography & Fonts, (3) Appearance, Theme & Navigation Layout, (4) Collapsible Secondary Drawers.
   - Restrict Liquid Glass finishes and optical tuning exclusively to Frosted Glass OS.
8. **Testing & Verification Suite**:
   - Update tests to run offline with local Maven cache.
   - Add test coverage for `ZeroCrashEngine` OOM and exception handling, `ZeroLagPerformanceEngine` tiering, `Infinix XOS` detection, Depth V4 single-subject extraction, proportional clock resizing, Free Stretch, and widget pushdown.
