# Neatly

**Professional Spirits Bench Calculator for Distillers, Blenders, and Serious Hobbyists.**

Neatly solves the molecular contraction problem that causes naive linear math (V1 x ABV1 = V2 x ABV2) to produce incorrect tank fills. Using a third-degree 2D polynomial fit to empirical OIML ethanol-water contraction data, Neatly is accurate to +/- 0.2 L per 100 L and extrapolates cleanly up to 96.4% ABV.

## Download

[![Download Latest APK](https://img.shields.io/badge/Download-Latest%20APK-brightgreen?style=for-the-badge&logo=android)](https://github.com/Navscan/neatly/releases/latest/download/Neatly.apk)

*Requires Android 8.0 (API 26) or higher.*

## Core Features

*   **Cut Tab:** Formulate, Dilute, Compounding, Solve, and From Order modes.
*   **Bottler Tab:** Order-driven production planning (Cases and Volume modes).
*   **Enhancing Tab:** Additive dosing for flavoured spirits with density-aware math.
*   **Dynamic Unit System:** Toggle between ABV/PROOF, METRIC/US, and VOLUME/MASS.
*   **Compliance Checks:** Built-in minimum proof warnings for Brandy, Whisky, and Rum.
*   **Export:** Generate PDF or TSV reports of your calculations.
*   **Pro Integration:** Built-in Paystack checkout for Pro tier unlocking.

## Tech Stack

*   **Language:** Kotlin
*   **UI:** Android Views + Material Components (No Compose)
*   **Math:** Custom OIML polynomial engine (`SpiritsMath.kt`)
*   **CI/CD:** GitHub Actions (auto-compiles and signs `Neatly.apk`)
*   **Updater:** In-app auto-updater pulling from GitHub Releases

## Architecture

Neatly is built to be fast, offline-first, and dependency-free. It uses standard `HttpURLConnection` for network requests and custom `SpannableString` logic for its typography, avoiding heavy external libraries.

---
*Property of PayBridge (Pty) Ltd. Built for the distillery floor.*
