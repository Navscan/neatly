================================================================================
NEATLY — PROJECT HANDOFF DOCUMENT
================================================================================

Document Version:   4.2
Last Updated:       2026-10-23
Current App Version: v1.31 (versionCode 36, build-41)
Next Planned Release: v1.32 (TBD)

This document is the single source of truth for the Neatly Android app.
Any AI session provided with this document plus the V4 Ultimate Chat
Prompt can continue work on the project with zero additional context.
If any prior chat, note, or document conflicts with this one, this one wins.

================================================================================
1. WHAT NEATLY IS
================================================================================

Neatly is a spirits bench calculator for professional distillers, blenders,
and serious hobbyists. It handles post-distillation production workflows.

Three top-level tabs:

  Cut        - Dilution, reduction, and blending. Sub-modes: Formulate, Dilute, Blend.
  Bottler    - Packaging calculator. Modes: Cases (cases -> volume) and Volume (volume -> cases).
  Enhancing  - Additive dosing for flavoured spirits, liqueurs, aperitifs.
               Two modes: Finished volume (specify output), Base volume (specify input).

Note on naming: the tab is called Enhancing, not Flavour. Do not rename.
Note on Cut: The word "Cut" is kept because the user confirmed it. Do not relitigate.
Note on Bottler: As of v1.31, the Bottler Cases mode *no longer* performs dilution math. It only calculates the total physical volume required (order volume + wastage). The user then pushes this volume to the Cut -> Formulate tab to calculate spirit and water. This removed massive redundancy from the app.

THE CORE PROBLEM SOLVED

When ethanol and water are mixed, the final volume is less than the sum of
the parts due to molecular contraction. Neatly uses a third-degree 2D polynomial 
fit to empirical OIML ethanol-water contraction data.

================================================================================
2. PROJECT IDENTITY
================================================================================

  App label             Neatly
  Android package       com.neatly.app
  GitHub repo           https://github.com/Navscan/neatly (public)
  Local VPS path        /root/neatly-apk
  Release artifact      Neatly.apk
  Release tag format    build-N (e.g., build-41)
  APK download URL      https://business.treasuretrail.africa/downloads/Neatly.apk
                        (Hosted directly on VPS business page via v1.31+)
  Metadata URL          https://github.com/Navscan/neatly/releases/latest/
                        download/version.json

================================================================================
3. TECH STACK
================================================================================

  Language              Kotlin
  Build                 Gradle 8.5 (installed by CI; no wrapper in repo)
  SDK                   compileSdk 34 · targetSdk 34 · minSdk 26
  UI                    Android Views + Material Components 1.11.0 (no Compose)
  Async                 Kotlin Coroutines 1.7.3
  Networking            HttpURLConnection + org.json (zero external deps)
  PDF/TSV Export        Native Android `PdfDocument` and `FileProvider` 
                        (zero external libraries)
  CI/CD                 GitHub Actions
  Distribution          GitHub Releases + in-app auto-updater + VPS direct download

================================================================================
4. OPERATIONAL ENVIRONMENT
================================================================================

VPS
  OS                    AlmaLinux (dnf/yum, NOT apt)
  IP                    154.66.198.207
  Hostname              server.paybridge.co.za
  SSH user              root

Signing keystore
  Path                  /root/backups/neatly-release.jks
  Base64 backup         /root/backups/neatly-release.jks.b64
  Alias                 neatly
  Format                JKS, RSA 2048-bit, 10,000-day validity
  CRITICAL              If this file or its password is lost, no future
                        update can install over existing user installs.

================================================================================
5. THE MATH ENGINE (SpiritsMath.kt)
================================================================================

The computational core of Neatly. Pure Kotlin object, zero Android dependencies.

POLYNOMIAL COEFFICIENTS
  C0 =  61.223438, C1 =   6.696005, C2 = -10.971703, C3 =   0.008028, C4 =  -0.169239
  C5 =   0.260478, C6 =  -0.000022, C7 =  -0.000054, C8 =   0.001384, C9 =  -0.002073

WATER FORMULA
  W = C0 + C1*S + C2*T + C3*S^2 + C4*S*T + C5*T^2 + C6*S^3 + C7*S^2*T + C8*S*T^2 + C9*T^3

PUBLIC FUNCTIONS
  waterPer100L(sourceAbv, targetAbv)
  calculate(volumeLiters, sourceAbv, targetAbv) -> Reduce mode
  calculateBatch(finishedVolumeLiters, stockAbv, targetAbv) -> Formulate mode
  blendCombine(volumeA, abvA, volumeB, abvB) -> Compounding mode
  blendSolve(abvA, abvB, targetAbv, batchSizeLiters) -> Solve mode
  calculateFlavour(...) / calculateFlavourFromBase(...) -> Enhancing mode

  Density table and mass-mode conversions:
    densityAt(abv), kgToLitres(kg, abv), litresToKg(litres, abv), etc.

================================================================================
6. CURRENT APP STRUCTURE (v1.31)
================================================================================

TOP TABS
  Cut        Fully working (Formulate / Dilute / Blend)
  Bottler    Fully working (Cases / Volume) - NO ABV math.
  Enhancing  Fully working (Finished / Base)

AGE GATE (v1.30)
  On first launch, a full-screen overlay appears asking the user to confirm 
  they are of legal drinking age. Uses SharedPreferences ("isAgeVerified") 
  to remember the choice. Subsequent launches bypass the gate.

DYNAMIC LABEL SYSTEM (v1.23+)
  The UI dynamically switches the text of labels based on the global VOL/MASS toggle.

EXPORT ENGINE (v1.24 - v1.28)
  - UI: Two pill buttons below the results card: [ PDF Document ] and [ Excel (.tsv) ].
  - Global Report: Exports data from ALL THREE tabs.
  - Zero-Fill Logic: Unused tabs are populated with "0.00" or "None".
  - Cache Persistence: Navigating away does not erase the cache.
  - Dynamic Additives: Uses the exact names typed in the "Name" field for headers.

PUSH-FORWARD CHEVRONS
  Small arrow (->) on result rows that have a natural destination.
  v1.31 specifically added `pushBottlerWastageToCutVol` to push the final volume from Bottler Cases to Cut Formulate.

================================================================================
7. FILE TREE
================================================================================

/root/neatly-apk/
├── .github/workflows/build.yml         CI: assemble, sign, publish release
├── .gitignore
├── build.gradle.kts                    Root: plugin versions only
├── settings.gradle.kts
├── gradle.properties
├── make_icons.sh
├── NEATLY_01_HANDOFF.md                This document
├── Neatly_User_Manual.md               User manual
└── app/
    ├── build.gradle.kts                versionCode 36, versionName "1.31"
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/neatly/app/
        │   ├── MainActivity.kt         UI wiring, export cache, logic
        │   ├── SpiritsMath.kt          Polynomial engine + calculations
        │   ├── ExportManager.kt        PDF and TSV generation
        │   ├── Updater.kt              GitHub Releases auto-updater
        │   └── FontSpan.kt             Custom span for API 26 fonts
        └── res/
            ├── layout/activity_main.xml
            ├── drawable/ic_pdf.xml, ic_excel.xml
            ├── values/strings.xml, colors.xml, themes.xml
            ├── xml/file_paths.xml
            └── mipmap-*/ic_launcher.png (Generated from AI image)

================================================================================
8. VERSION HISTORY
================================================================================

  v1.20     20   26     From Order swap warning
  v1.21     21   28     Comments restored, Dilute-to-Bottler chevron fix
  v1.22     22   29     Compound compliance, lowest/highest proof
  v1.22.2   24   32     Result headlines removed, contraction fixed
  v1.23     25   33     Dynamic mass labels, barrel aging row, density fix
  v1.24     26   34     Export UI (PDF/TSV), ExportManager.kt created
  v1.25     27   35     FileProvider fix: allow sharing export files
  v1.26     28   36     Force global export report with default zeroes
  v1.27     29   37     Persist export cache across tab navigation
  v1.28     30   38     Capture additive names dynamically in export
  v1.29     31   39     Remove From Order tab, simplify Blend, decimal fix
  v1.30     35   40     Age Gate overlay, AI copper distillery icon
  v1.31     36   41     Strip dilution math from Bottler, push to Cut

================================================================================
9. DEPLOYMENT METHODOLOGY (NON-NEGOTIABLE)
================================================================================

One file per payload. One payload per message. Byte-verified after paste.

PER-FILE DEPLOY PATTERN
  Part 1 - Command: rm -f && nano
  Part 2 - Full file content.
  Part 3 - Save in nano: Ctrl+O, Enter, Ctrl+X.
  Part 4 - Verification: wc -c, grep -c.

ABSOLUTE PROHIBITIONS
  NO sed, NO cat for writes, NO in-place VPS edits.
  NO multiple files in one payload.
  NO partial code blocks or placeholders.
  NO secrets written in chat.

================================================================================
10. VERSION NUMBERING (ODOMETER RULE)
================================================================================

Format: vMAJOR.MINOR.PATCH (counted like an odometer).
Two-part numbering is used (v1.31, v1.32). 
versionCode increments monotonically (36, 37).
No letter suffixes. No three-part patches.

================================================================================
11. DESIGN SYSTEM
================================================================================

COLORS
  bg #F3EFE7, card_bg #FFFFFF, ink #1A1A1A, ink_muted #6B6B6B
  border #E5E0D6, border_strong #D4CEC1, accent_dark #1A1A1A

TYPOGRAPHY
  Wordmark: Cinzel Decorative Bold ("N") + Bodoni Moda ("eatly").
  Numerical: Playfair Display SemiBold (600).
  Body: System sans-serif (Roboto).

================================================================================
12. WHAT THE AI MUST NEVER DO
================================================================================

  Never use sed, cat, or any in-place edit tool.
  Never produce partial snippets with placeholders.
  Never bundle multiple files in one payload.
  Never write secrets into chat.
  Never invent version numbers. Odometer rule.
  Never use apostrophes in strings.xml values.

================================================================================
13. THE ACODE TRAP (CRITICAL WARNING)
================================================================================

The operator maintains a "master copy" of the source tree in Acode on
the phone, at /sdcard/Documents/neatly-source/.

HISTORY: This copy was stale from v1.7 until 2026-10-16. A full rsync 
brought it to v1.22.2. Later, rsync --delete accidentally removed the 
Handoff Document from the phone because it wasn't in the VPS repo. It has 
now been restored and committed to Git so rsync will protect it.

RULE: Always keep the Handoff Document in Git. If rsync deletes it, 
re-copy from chat history and push to Git immediately.

================================================================================
14. SESSION HISTORY (for AI context - do not repeat these lessons)
================================================================================

  - The operator's shed uses mass inputs. Mass toggle is first-class.
  - Additive dosing must respect density for mass conversion.
  - Contraction is a volume delta only. Never display it in kg.
  - FileProvider must be explicitly configured for any shared directory.
  - Session caches must persist across tab navigation.
  - If a build fails, check the GitHub Actions log for AAPT2 / string 
    resource errors (missing strings.xml is a common culprit).

================================================================================
15. DEVELOPMENT ROADMAP
================================================================================

Items confirmed for potential future work:
  - Enhancing hint text cleanup: "Specify base volume..." is static in MASS mode.
  - Two-product allocation (CRM territory, not Neatly)
  - Notes system extension to more tabs (the (!) pattern)

Explicitly excluded from Neatly scope:
  - Recipe library / saved formulas
  - Cost-per-batch
  - CRM API bridge
  - Floor QR scanning

================================================================================
16. HOW TO RESUME WORK (FOR A NEW AI CHAT)
================================================================================

  1. Read this document in full.
  2. Ask the operator: "What would you like to work on next?"
  3. For any file that will change, request its current content by
     instructing the operator to run: cat /root/neatly-apk/<absolute path>
  4. Confirm change scope in prose outside any code block.
  5. Produce the payload (Command -> Full File -> Verification).
  6. End with exactly: AWAITING OPERATOR CONFIRMATION.
  7. Wait for operator confirmation before the next payload.

================================================================================
17. OPEN ITEMS
================================================================================

  6.1  CLOSED: Updater TTL confirmed at 15 minutes.
  6.2  CLOSED: Acode master resynced.
  6.3  CLOSED: Handoff document updated to v4.2.
  6.4  OPEN: Enhancing hint text does not switch to "mass" in MASS mode.
  6.5  CLOSED: Export engine fully built and verified.
  6.6  CLOSED: Age Gate implemented.
  6.7  CLOSED: Bottler dilution redundancy removed.

================================================================================
18. CONTACT / OWNERSHIP
================================================================================

  Operator      Navscan (GitHub account)
  Phone         Android, Termux (SSH client) + Acode (editor)
  VPS           root@154.66.198.207 via SSH
  VPS hostname  server.paybridge.co.za
  Distiller     Rum, from fermented Puratex syrup, distilled to 90%+,
                flavoured with concentrates.

================================================================================
END OF HANDOFF DOCUMENT
================================================================================