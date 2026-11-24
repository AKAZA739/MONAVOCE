# 05 — UI/UX, Accessibility & Localization Audit

This report evaluates the current visual design, end-to-end user experience across screens, **WCAG 2.1 / Android Accessibility Guidelines** compliance, and the quality of Arabic (`RTL`) and English (`LTR`) support.

---

## 1. Screen-by-Screen UI/UX Inspection

In `v1.3.0`, the app uses an **Obsidian OLED Dark** theme ([Theme.kt](../../app/src/main/java/com/myexpenses/app/ui/theme/Theme.kt)) inspired by premium dark finance interfaces, paired with a glowing **Cosmic Neon Voice Orb**:

### A. Dashboard Screen (`DashboardScreen.kt`)
- **Strengths:**
  - Unified `Overview Hero Card` combining the period selector (`Today / Week / Month / Year / All`), interactive `Date Navigator` (Previous/Next arrows and Return to Today), bold spending total (`34.sp, FontWeight.ExtraBold`), mini monthly weekly bar chart (`W1..W5`), and a horizontal bar for the top 3 spending categories with percentages.
  - Floating pill bottom navigation bar (`Floating Pill Dock` in [MainActivity.kt](../../app/src/main/java/com/myexpenses/app/MainActivity.kt)) centered around the glowing microphone button and quick manual-entry button.
- **Weaknesses:**
  - No visual distinction on individual transaction rows when a record's currency differs from the default currency (`MONA-FIN-001`).
  - Missing `First-Run Onboarding` explaining to new users how to bind the side power button or import the offline Arabic speech model (`MONA-UX-003`).

### B. Cosmic Voice Overlay (`VoiceOverlayPanel.kt` & `QuickVoiceActivity.kt`)
- **Strengths:**
  - Striking visual design using `Canvas` to render multi-layered glowing rings (`Cyan #00E5FF` -> `Royal Blue #3B82F6` -> `Magenta #D946EF`) that rotate and pulse in real time with voice amplitude (`voiceRmsLevel`).
  - Clear engine readiness indicator (`Sherpa-ONNX 100% Offline Ready` or a one-tap `.zip` import button if the model is missing).
- **Weaknesses (`MONA-UX-001`):**
  - Colors in [VoiceOverlayPanel.kt](../../app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt) are hardcoded to dark mode (`CosmicTopBg = Color(0xFF060714)`, `CosmicBottomBg = Color(0xFF0E122B)`), completely ignoring the user's **Light Theme** setting and creating harsh contrast clashes between inner cards and the panel when Light Mode is active.

### C. Statistics Screen (`StatisticsScreen.kt`)
- **Strengths:**
  - Custom `Canvas`-drawn `DonutChart` with total spending at the center, a 7-day bar chart, and progress bars per category showing transaction count and percentage.
  - Quick `CSV` and `JSON` export/share buttons at the top of the screen.

### D. Categories (`CategoriesScreen.kt`) & Settings (`SettingsScreen.kt`)
- **Strengths:**
  - Categories screen provides a complete editor for Arabic name, English name, voice recognition keywords, icon (18 choices), and color (12 colors).
  - Settings screen includes an interactive **Side Button Assistant** setup section, home screen voice shortcut pinning, offline voice model management (`130 MB`), backup/restore, and language, currency, and theme selectors.

---

## 2. UI/UX & Accessibility Findings

### 1. `[MONA-UX-001]` (Medium) — Hardcoded Dark Colors in `VoiceOverlayPanel` Ignore Light Theme
- **Exact Location:** [VoiceOverlayPanel.kt](../../app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt), lines [270-303](../../app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt).
- **Evidence:** The panel uses a fixed `#060714` to `#0E122B` gradient and `Color.White` text, while inner cards (such as the model import card or error state at lines 363 and 487) mix in `MaterialTheme.colorScheme.errorContainer` or `Color.White.copy(alpha = 0.05f)`.
- **Recommended Fix:** Unify design tokens under the **MONAVOCE** theme so the voice orb retains its luminous depth inside a container that harmonizes properly with both Dark and Light themes without text contrast conflicts.

---

### 2. `[MONA-UX-002]` (Medium) — TalkBack Accessibility Gaps & Touch Targets Smaller Than `48dp`
- **Exact Locations:**
  1. **Missing `contentDescription` for Screen Readers (TalkBack):**
     - The main `CosmicVoiceOrb` in [VoiceOverlayPanel.kt](../../app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt) uses `.clickable { onClick() }` on a `Box` without `Role.Button` or `semantics { contentDescription = ... }` (`Icon(..., contentDescription = null)` at line 665). Blind screen-reader users—who stand to benefit most from a voice-first finance app—cannot tell what the primary voice orb button does.
     - Category edit and delete buttons in [CategoriesScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/CategoriesScreen.kt) pass `contentDescription = null`.
     - Date navigation buttons in [DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt) pass `contentDescription = null`.
  2. **Touch Targets Below Standard Minimum (`48.dp x 48.dp`):**
     - Delete expense button in transaction cards ([DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt)): `Modifier.size(26.dp)` — too small and prone to accidentally triggering edit instead of delete.
     - Date stepper buttons ([DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt)): `Modifier.size(30.dp)`.
     - Category color swatches ([CategoriesScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/CategoriesScreen.kt)): `Modifier.size(30.dp)`.

---

### 3. `[MONA-UX-003]` (Low) — Missing First-Run Onboarding Flow
- **Analysis:** On first launch, the user lands directly on an empty dashboard without guidance on:
  1. Supported voice command syntax (e.g., logging multiple expenses in one phrase or saying `"delete last expense"`).
  2. The status of the offline Arabic speech engine and how to activate it in seconds.
  3. How to bind the hardware `Side Button` to launch the voice overlay immediately from outside the app.

---

### 4. `[MONA-UX-004]` (Low) — Inline Conditional Bilingual Strings in Compose (`if (isArabic) ... else ...`) Instead of `strings.xml` Resources
- **Exact Location:** All `ui/screens/*.kt` and `ui/components/*.kt` files.
- **Analysis:** Switching language via `CompositionLocalProvider(LocalLayoutDirection provides layoutDirection)` in [MainActivity.kt](../../app/src/main/java/com/myexpenses/app/MainActivity.kt) delivers instant responsiveness (Zero Activity Recreation), but hardcoding strings inline with `if (isArabic) "..." else "..."` makes adding additional global languages (e.g., French, Spanish, Turkish, Urdu) harder as **MONAVOCE** expands internationally.
