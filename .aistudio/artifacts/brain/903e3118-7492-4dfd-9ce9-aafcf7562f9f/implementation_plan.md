# LABDROID (UI/UX v2 - M3) Kitchen Implementation Plan

LABDROID is a professional Android system kitchen application built 100% with Material Design 3 and Jetpack Compose. It provides automated image partition unpacking/repacking (FORGE engine), an internal kitchen workspace project manager, a custom unified popup file explorer, and a modular `.labd` extension framework with complete GitHub Actions CI/CD.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been aligned based on user clarification:
> - **FORGE Execution & Permissions**: Strict root privilege requirement for full partition unpack and repack operations, with clear root detection/warning indicators and execution logging.
> - **Pre-installed Extension**: Bundling the minimal `.labd` skeleton extension template with manifest, metadata, and execution scripts for instant testing and reference.
> - **CI/CD Pipeline**: GitHub Actions workflow delivering both Debug and signed Release APK artifacts, ready for GitHub push.

- **Confirmed Decision 1**: Root privilege enforcement for FORGE unpack/repack operations with su bridge command execution.
- **Confirmed Decision 2**: Zero system intent dependency—LABDROID uses its own native M3 Popup File Selector for all browsing (`/data/data/com.labdroid.kitchen/` and storage).
- **Confirmed Decision 3**: Unified visual hierarchy where native modules and installed `.labd` extensions share the exact same card layout on the Home screen.

---

## 1. Overview & Core Concept

- **What It Does**: LABDROID provides ROM developers and Android power users with a unified mobile kitchen environment. It handles partition manipulation (`system.img`, `vendor.img`, `boot.img` in ext4/EROFS), project rootfs inspection and file operations, `.labd` extension lifecycle management, and a real-time console log viewer.
- **Target Audience**: Android custom ROM maintainers, security researchers, and developers who need an autonomous partition kitchen on-device.
- **Key Value**: Replaces cumbersome command-line PC tools with an autonomous, elegant, portable Android application that respects filesystem metadata (SELinux contexts, capabilities, file modes) and never relies on external system intents.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **System Status & Feature Launch (Accueil)**:
   - User checks device telemetry (Root status, Device model, Android OS version, CPU architecture: arm64-v8a).
   - User taps any module in the unified vertical card list (FORGE, Kitchen Explorer, Native Tools, or any installed `.labd` extension) to navigate directly into its interface.
   - Quick action `+` button in top bar triggers the custom M3 Popup File Explorer to search and install `.labd` packages.
   - Power button toggles the background daemon engine; info button (`i`) displays technical documentation and credits.

2. **FORGE Partition Processing (UNPACK / REPACK)**:
   - **UNPACK**: User selects partition image (`.img`) via the custom popup file picker, clicks UNPACK. System parses image header, creates a dedicated project folder under `/data/data/com.labdroid.kitchen/home/PROJET/<name>/`, extracts rootfs, saves database metadata, and streams colorized logs in the integrated console.
   - **REPACK**: User selects an existing project from dropdown, reviews partition stats, clicks REPACK. FORGE recreates image to `/sdcard/LABDROID/REPACKED/` without depending on the original source file.

3. **Project Management (PROJET Tab)**:
   - Dedicated kitchen workspace explorer for `/data/data/com.labdroid.kitchen/home/PROJET/`.
   - Displays project list, size, rootfs tree, and metadata.
   - Full file management actions: delete, copy, cut, paste, rename, and inspect.

4. **Settings & Themes (Paramètres Tab)**:
   - Theme toggle (Light, Dark, System default).
   - Security toggles (Root permissions check, storage permissions status).
   - About section with version information and `.labd` developer guide links.

### Visual Identity & Theme
- **Theme**: Minimalist Material 3. Clean white/surface cards on subtle light gray/slate background (`#F8F9FA` light / `#121214` dark).
- **Cards**: Minimalist horizontal cards displaying only the feature icon, bold title, and trailing chevron (`>`), without visual clutter or descriptions.
- **Typography**: Clean sans-serif with monospace accents for paths, hashes, and console output.
- **Color Tokens**:
  - Primary: Deep slate/indigo (`#2563EB` / `#3B82F6`)
  - Status Indicators: Green (`#16A34A`) for Root/Success, Amber (`#D97706`) for Warning, Rose (`#DC2626`) for Error
  - Console: Monospace terminal surface (`#0F172A`) with high-contrast colored stream tags

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Custom File Picker Popup vs. Storage Access Framework (SAF)**
  - *Chosen Approach*: Fully custom Jetpack Compose dialog modal that navigates the local Linux filesystem directly (`/sdcard`, `/storage/emulated/0`, `/data/data/com.labdroid.kitchen/`).
  - *Why*: Eliminates system Intent picker restrictions, provides uniform M3 styling, supports instant file filtering (`.img`, `.labd`), and works natively in root/internal app storage.
  - *Alternatives Considered*: Standard `ActivityResultContracts.OpenDocument` (rejected per user's strict prohibition).

- **Decision 2: Autonomous FORGE Metadata Database**
  - *Chosen Approach*: During UNPACK, capture file metadata, SELinux contexts, capabilities, and filesystem parameters into `database/project_meta.json` and `database/fs_config`. REPACK strictly relies on this database and `rootfs/`.
  - *Why*: Guarantees zero retention or dependency on the original `.img`, enabling reproducible standalone builds.

- **Decision 3: Unified Extension Engine (`.labd`)**
  - *Chosen Approach*: Standard ZIP archive containing `manifest.json`, executable scripts/binaries for `arm64-v8a`, and UI metadata. Deployed directly to internal kitchen bin and registered into the Home screen card list seamlessly.
  - *Why*: Provides first-class citizenship for community and custom extensions.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        LABDROID Main Activity                          │
├────────────────────────────────────────────────────────────────────────┤
│                       Material 3 Scaffold & Insets                     │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ TopAppBar (Title + Version | [+] Install Labd | Daemon | Info)   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    Current Screen (Bottom Nav)                   │  │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐  │  │
│  │  │  Accueil (Home)  │  │   PROJET (Mgr)   │  │   Paramètres   │  │  │
│  │  │ - Telemetry Card │  │ - Project List   │  │ - Theme Switch │  │  │
│  │  │ - Unified Cards  │  │ - File Tree      │  │ - Root Status  │  │  │
│  │  │   (Forge + Labd) │  │ - Cut/Copy/Paste │  │ - Doc & About  │  │  │
│  │  └──────────────────┘  └──────────────────┘  └────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Popup File Selector Modal (Custom M3, internal + sdcard paths)    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ NavigationBar (Accueil | PROJET | Paramètres)                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Core Business & Engines                         │
│  ┌─────────────────────────┐  ┌─────────────────────────────────────┐  │
│  │      Forge Engine       │  │        Labd Extension Engine        │  │
│  │ - Unpack & FS Analyzer  │  │ - Archive Validator (.labd zip)     │  │
│  │ - Metadata DB Builder   │  │ - Manifest Parser (manifest.json)   │  │
│  │ - Rootfs Rebuilder      │  │ - Bin Deployer (arm64-v8a)          │  │
│  │ - Repack to /sdcard/    │  │ - Extension Registry                │  │
│  └─────────────────────────┘  └─────────────────────────────────────┘  │
│  ┌─────────────────────────┐  ┌─────────────────────────────────────┐  │
│  │  Project Filesystem Mgr │  │       Real-Time Log Streamer        │  │
│  │ - /data/data/.../PROJET │  │ - Monospace M3 Console              │  │
│  │ - Safe copy/move/delete │  │ - INFO, SUCCESS, WARNING, ERROR     │  │
│  └─────────────────────────┘  └─────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Filesystem Layout Contract
- Kitchen Root: `/data/data/com.labdroid.kitchen/home/`
- Projects Root: `/data/data/com.labdroid.kitchen/home/PROJET/`
- Project Specifics:
  - Rootfs: `/data/data/com.labdroid.kitchen/home/PROJET/<project_name>/rootfs/`
  - Metadata: `/data/data/com.labdroid.kitchen/home/PROJET/<project_name>/database/`
  - Temp: `/data/data/com.labdroid.kitchen/home/PROJET/<project_name>/temp/`
- Output Export: `/sdcard/LABDROID/REPACKED/`
- Extensions Registry: `/data/data/com.labdroid.kitchen/extensions/`
- Central Binaries: `/data/data/com.labdroid.kitchen/bin/`

### CI/CD and Extension Documentation Deliverables
- `.github/workflows/android-build.yml`: Automated GitHub Actions workflow compiling debug and release APKs on push.
- `docs/LABD_SPECIFICATION.md`: The official bible and skeleton specification for `.labd` module creation.
- Sample bundled `.labd` skeleton package for immediate on-device testing.
