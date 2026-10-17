# NEATLY - User Manual
**Version:** 1.23
**Developer:** PayBridge (Pty) Ltd
**Platform:** Android

---

## 1. Introduction: Why Neatly?
When you mix pure ethanol and water, the final volume is slightly less than the sum of the parts. This is called **molecular contraction**. A standard hobbyist calculator using linear math (Volume × ABV) will give you the wrong tank fill, the wrong water addition, and incorrect duty calculations.

Neatly solves this. Using a third-degree 2D polynomial fit to empirical OIML ethanol-water contraction data, Neatly is accurate to ±0.2 L per 100 L. It extrapolates cleanly up to 96.4% ABV (the practical distillation ceiling).

Neatly does not police your process. It calculates the math with laboratory precision so you can focus on making the cut.

---

## 2. Global Controls (Header Pills)
The pill buttons in the top-right corner dictate how the entire app behaves.

| Pill | Description |
| :--- | :--- |
| **% ABV / PROOF** | Toggles all strength fields between Alcohol by Volume (ABV) and US Proof (ABV × 2). |
| **METRIC / US** | Toggles volume fields between Litres (L) and US Gallons (gal). Toggles mass between Kilograms (kg) and Pounds (lb). |
| **VOL / MASS** | **Critical.** Toggles the bulk inputs between Volume (L) and Mass (kg). When MASS is selected, you are weighing your spirits on a scale. Neatly automatically converts mass to volume using the specific density of the spirit at its ABV. |
| **BRANDY...RUM** | Sets the global product type. This drives the regulatory compliance minimums in the Cut tab. |

---

## 3. The Cut Tab
The Cut tab is used for reduction and blending. It has three sub-modes: Formulate, Dilute, and Blend.

### 3.1 Formulate
*Use this when you need to make a specific final volume from scratch.*
1. Enter your **Target volume** (e.g., 100 L).
2. Enter the **Base strength** of your stock (e.g., 72% ABV).
3. Enter the **Target strength** you want to achieve (e.g., 43% ABV).
4. **Results:** Neatly tells you exactly how much **Base spirit needed** to draw, and how much **Water to add**. It also shows the Pure Alcohol and the Contraction (volume lost to molecular bonding).

### 3.2 Dilute
*Use this when you have a known volume of spirit and want to water it down to a target strength.*
1. Enter your **Base volume** (e.g., 100 L).
2. Enter the **Base strength** (e.g., 72% ABV).
3. Enter the **Target strength** (e.g., 43% ABV).
4. **Results:** Neatly calculates the **Water to add** and the **Final volume** you will end up with.

### 3.3 Blend
The Blend sub-mode has three sub-categories:
*   **Compounding:** You have two different spirits (Component A and B) with known volumes and strengths. Neatly tells you the pooled volume, final strength, pure alcohol, and contraction.
*   **Solve:** You know the strength of two spirits and the final batch size you need. Neatly calculates exactly how much of Component A and Component B to use.
*   **From Order:** You need to fulfill a specific order (e.g., 100 L at 43%). 
    *   *Ratio mode:* You specify the percentage of the strong spirit to use.
    *   *Cap mode:* You specify a physical limit (cap) on one of the streams (e.g., "I only have 20 L of the strong spirit left"). Neatly calculates how much of the other spirit to use. 

---

## 4. The Bottler Tab
The Bottler tab is for production planning and packaging runs.

### 4.1 Cases Mode
*Use this when you have an order for a certain number of bottles.*
1. Enter your **Bottle size** (e.g., 750 mL) and **Pack size** (e.g., 12 bottles per case).
2. Enter the **Cases** ordered (e.g., 72).
3. Enter your **Target strength** and **Base strength**.
4. *(Optional)* Enter your **Wastage** percentage (spillage, line losses).
5. **Results:** Neatly tells you the total volume required, the volume required *with* wastage factored in, the **Base spirit needed**, and the water required. 

**Barrel Aging (Angels' Share):** If you check "Include barrel aging", you can enter the years in the barrel and the annual loss percentage (e.g., 7%). Neatly will calculate the **Barrel start volume**—the larger amount of spirit you must put into the barrel *today* to end up with your target volume years from now.

### 4.2 Volume / Mass Mode
*Use this in reverse: you have a known amount of spirit and want to know how many cases it will fill.*
Enter your Finished volume/mass and Neatly tells you exactly how many **Whole cases**, **Bottles**, **Residual** (leftover), and the **Total bottled**.

---

## 5. The Enhancing Tab
The Enhancing tab is for dosing additives, colours, and flavour concentrates into a finished spirit.

1. Choose **Finished volume** (specify your final batch size) or **Base volume** (specify your starting spirit volume).
2. Enter your **Target strength** and **Base spirit strength**.
3. **Additives:** You can enter up to three additives.
    *   **Name:** Free text (e.g., E150A, Heart Rum).
    *   **Dose:** The amount to add.
    *   **Unit:** Choose `mL/L`, `g/L`, or `drops/L` depending on how your supplier specifies the dosage. (Note: This remains independent of the global VOL/MASS toggle).
    *   **ABV:** The alcohol content of the additive (default 0).
    *   **Density:** Crucial for accurate mass conversions. If you are dosing in `g/L`, density determines the volume. If you are dosing in `mL/L` but weighing on a scale (MASS mode), Neatly uses the density to tell you the exact weight (kg) to put on the scale.
4. **Results:** Neatly provides the exact Base spirit to charge, water, individual additive volumes/masses, pure alcohol, and the final total.

---

## 6. Pro Tips & UI Features

*   **Push-Forward Chevrons (→):** Look for the small arrows on the result rows. Tapping an arrow instantly transfers that value into the input field of the next logical tab. For example, tapping the arrow on "Water to add" in the Bottler tab will take you straight to the Dilute tab and populate the volume.
*   **The Compliance Engine:** In the Cut → Blend → From Order mode, you can set a regulatory **Minimum required** percentage. If your blend results in a base spirit share ("Lowest proof") that falls below this minimum, a warning will appear.
*   **Contraction:** Contraction is always displayed in Volume only, even if you are in MASS mode. This is because contraction is a volume delta, not a physical substance; there is no such thing as a "kilogram of contraction."
*   **Dynamic Labels:** When you toggle between VOL and MASS, the labels dynamically change (e.g., "Target volume" changes to "Target mass") so you always know exactly what you are entering.

---
*Neatly v1.23 - © PayBridge (Pty) Ltd. All rights reserved.*