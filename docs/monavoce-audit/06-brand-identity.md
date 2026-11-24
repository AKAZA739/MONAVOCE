# 06 — Global Brand Identity & Material 3 Design System (MONAVOCE)

This document defines the complete design and engineering specification for transforming the app from its provisional identity (`My Expenses`) to the approved global brand:

# **MONAVOCE**

---

## 1. Brand Strategy & Positioning

### A. Etymology & Core Concept
- **MONA** (*Moneta*): The Latin root for money, currency, and stored financial value.
- **VOCE** (*Voce*): The Italian/Latin word for the human voice, tone, and living expression.
- **Combined Meaning:** **"The Voice of Money"** — instant personal finance management driven by your voice, with absolute privacy on your device and zero internet dependency.

### B. Brand Personality & Taglines
- **Brand Personality:** Premium, International, Minimal, Trustworthy, Sophisticated, and Innovative.
- **Primary Taglines:**
  - *Speak Your Spend. Private by Design.*
  - *Your Financial Voice. 100% On-Device.*

### C. Naming Landscape Verification
- Checks across Google Play and global markets found no existing finance app using the exact name **`MONAVOCE`** (only phonetically adjacent names such as `Monavo`, `MonAi`, and `Monivo`). Therefore, **MONAVOCE** is distinctive and strongly registrable as a trademark, provided the software package ID is changed from `com.myexpenses.app` to **`com.monavoce.app`** (`MONA-PLAY-001`).

---

## 2. Logo & Symbolic Direction

### Logo Concept: **"The Sonic Coin / Monogram MV Wave"**
Rather than a generic microphone icon or an overused dollar sign, the **MONAVOCE** mark combines:
1. **A symmetrical geometric letter `M`** whose center peaks form an **Acoustic Waveform** dipping into a central **`V`**.
2. **Sovereign Vault Ring:** A clean, refined outer circle enclosing the waveform—simultaneously representing a **Coin (Moneta)**, a **Zero-Cloud Privacy Vault**, and the **Pulsing Voice Orb (Voce Orb)**.

---

## 3. Approved Color System (MONAVOCE Color Palette & Material 3 Tokens)

The proposed **MONAVOCE** palette preserves the user's preferred luxury aesthetic (**Obsidian OLED + Soft Periwinkle + Sonic Cyan/Violet Orb**) while refining contrast to international standards (**WCAG AAA / AA**) and adding a cohesive light theme (**Alabaster Prestige**) to resolve `MONA-UX-001`.

### A. Primary Dark Theme — **"Obsidian Vault"** (Default Premium Dark Theme)

| Token Name | HEX Code | Material 3 / Compose Role |
|---|---|---|
| `MonaObsidianBg` | `#07080D` | `background` — AMOLED-friendly primary screen background. |
| `MonaSurface1` | `#11131C` | `surface` — Standard card background (`ExpenseListItemCard`, `CategoryCard`). |
| `MonaSurface2` | `#171A26` | `surfaceVariant` — `Overview Hero Card` and floating navigation dock. |
| `MonaSurfaceElevated` | `#1F2333` | Nested containers, `Segmented Pills`, and search fields. |
| `MonaBorderSubtle` | `#272B3F` | `outlineVariant` — Hairline card borders (`1.dp`) for subtle glass depth. |
| `MonaPrimaryPeriwinkle` | `#8E8FFA` | `primary` — Primary brand accent (chart bars, active indicators, primary buttons). |
| `MonaPrimaryContainer` | `#23254B` | `primaryContainer` — Active item pill background in bottom navigation. |
| `MonaSonicCyan` | `#00E5FF` | `secondary` — Acoustic cyan for the active listening ring and offline indicator. |
| `MonaSonicMagenta` | `#D946EF` | `tertiary` — Complementary spectrum accent for the `Cosmic Voice Orb`. |
| `MonaGoldSovereign` | `#F5B041` | Financial highlight color for currencies, budgets, and key badges. |
| `MonaEmeraldSuccess` | `#10B981` | Voice model readiness (`100% Offline Ready`) and save confirmation states. |
| `MonaCoralDanger` | `#F43F5E` | `error` — Delete actions, budget overruns, and microphone errors. |
| `MonaTextPrimary` | `#F4F5FA` | `onBackground` / `onSurface` — Financial amounts and primary headings (`17.8:1` AAA contrast). |
| `MonaTextSecondary` | `#9CA3C2` | `onSurfaceVariant` — Subtitles, dates, and percentages (`7.4:1` AAA contrast). |

### B. Light Theme — **"Alabaster Prestige"** (Harmonized Light Theme)

| Token Name | HEX Code | Material 3 / Compose Role |
|---|---|---|
| `MonaLightBg` | `#F6F7FB` | `background` — Warm, eye-friendly light canvas. |
| `MonaLightSurface1` | `#FFFFFF` | `surface` — Crisp white transaction cards. |
| `MonaLightSurface2` | `#ECEEFA` | `surfaceVariant` — Nested containers and period selectors. |
| `MonaLightBorder` | `#DCE0F2` | `outlineVariant` — Light-mode card borders. |
| `MonaLightPrimary` | `#4F46E5` | `primary` — Deep royal indigo delivering `7.2:1` contrast on light surfaces. |
| `MonaLightTextPrimary` | `#0D1021` | `onBackground` — Dark financial figures and headings. |
| `MonaLightTextSecondary` | `#52587A` | `onSurfaceVariant` — Secondary labels and dates. |

---

## 4. Typography & Numeric System

In a bilingual finance app, figures must align cleanly in columns (`Tabular Figures`) and weights must stay visually balanced between Arabic and English:
- **Recommended Arabic Typeface:** **`IBM Plex Sans Arabic`** or **`Cairo`** (bundled/downloadable font in `res/font/`).
- **Recommended Latin & Numeric Typeface:** **`Plus Jakarta Sans`** or **`Inter`** with `fontFeatureSettings = "tnum"` (Tabular Numbers) enabled on all monetary amounts and percentages.
- **Approved Type Scale (`MonaTypography`):**
  - `Hero Financial Total`: `34.sp`, `FontWeight.ExtraBold`, `letterSpacing = (-0.5).sp`, `fontFeatureSettings = "tnum"`.
  - `Screen Title`: `20.sp`, `FontWeight.Bold`.
  - `Card Amount`: `16.sp`, `FontWeight.Bold`, `fontFeatureSettings = "tnum"`.
  - `Voice Live Transcript`: `18.sp`, `FontWeight.Medium`, `lineHeight = 26.sp`.
  - `Metadata & Badges`: `12.sp`, `FontWeight.SemiBold`.

---

## 5. Screen Redesign Specifications Under MONAVOCE

1. **Brand Header (`MONAVOCE Brand Header`):**
   - Replace the temporary `"My Expenses"` text in Settings and the app header with the **MONAVOCE** wordmark and privacy badge:
     `🔒 100% On-Device Voice Vault`.
2. **Voice Sheet (`MONAVOCE Sonic Sheet`):**
   - Keep the signature `CosmicVoiceOrb` while adding:
     - Interactive example prompt chips (`"Coffee 500 and petrol 3000"`, `"Delete last expense"`).
     - Full TalkBack accessibility support (clear `contentDescription` for every state: `Listening`, `Analyzing`, `Tap to speak`).
     - Harmonized inner card colors across both Dark and Light themes.
3. **Adaptive Launcher Icon (`Adaptive Launcher Icon` — Android 8.0+ / Android 13+ Monochrome):**
   - **Background layer (`ic_launcher_background.xml`):** Deep diagonal gradient from `#07080D` to `#131629` with a subtle center glow `#1E2246`.
   - **Foreground layer (`ic_launcher_foreground.xml`):** **MV Sonic Wave** emblem surrounded by a gradient ring from `#00E5FF` through `#8E8FFA` to `#D946EF`.
   - **Monochrome layer (`ic_launcher_monochrome.xml`):** Clean line-art version of **MV Sonic Wave** for Material You themed icons on Android 13–16.
4. **Splash Screen (`Android 12+ SplashScreen API`):**
   - `#07080D` background with centered **MONAVOCE** icon and zero artificial delay, opening the dashboard in under `350ms`.
