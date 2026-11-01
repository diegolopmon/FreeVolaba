# AGENTS.md - Project Context & Guidelines for AI Agents

Welcome to **FreeVolaba**! This document provides a high-level overview of the project architecture, domain logic, telemetry formulas and derivations, build flavors, and coding guidelines for AI agents working on this codebase.

## 🚲 Project Overview
**FreeVolaba** is a native Android application written in Kotlin for indoor exercise bike telemetry and training session tracking.

- **Language:** Kotlin
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14) / 37
- **UI Framework:** Jetpack Compose with Material 3
- **Local Persistence:** Room Database with Google KSP
- **Unit Testing Framework:** JUnit 5 Jupiter (`useJUnitPlatform()`)

---

## 🏗️ Architecture & Component Overview

### 1. Foreground Telemetry Service (`com.diegolopmon.freevolaba.training.TrainingService`)
- Manages the lifecycle of exercise sessions and sensor connections.
- Processes raw sensor messages, calculates telemetry values, and updates `trainingDataState` and `historyState`.
- Runs as a Foreground Service with a persistent notification.
- Provides `stopAndSummarize()` to trigger workout aggregation, Room DB persistence, and summary state exposure.

### 2. Precise Timer (`com.diegolopmon.freevolaba.training.Chronometer`)
- Coroutine-backed StateFlow chronometer providing `elapsedTimeSeconds` and `elapsedTimeMillis`.
- Used for sub-second precise telemetry delta calculations (distance and calories).

### 3. Sensor Abstraction & Build Flavors (`com.diegolopmon.freevolaba.sensor`)
- `SensorSource`: Interface for hardware/virtual sensor data sources.
- **Build Flavors:**
  - `dev`: Uses `FakeSensor` to simulate bike data without physical hardware.
  - `prod`: Uses `UsbSensor` for real USB serial communication with exercise bikes.
- `SensorDataDecoder`: Decodes raw messages into structured `SensorData(rpm, resistance)`.
- `SensorDataRecorder`: Logs raw sensor data locally.

### 4. Data Models & Aggregation (`com.diegolopmon.freevolaba.training`)
- `TrainingData`: RPM, Resistance (1-10), Distance (m), Speed (km/h), Heart Rate (BPM), Power (Watts), Calories (Double during live accumulation, kcal).
- `TrainingHistoryPoint` & `TrainingSessionHistory`: Captures timeline history points for plotting.
- `WorkoutSummary`: Data model and `calculate()` aggregation logic computing total distance, total calories (Int, rounded), avg/max power, avg/max RPM, and avg resistance upon session completion.

### 5. Local Persistence Layer (`com.diegolopmon.freevolaba.db` & `data`)
- `WorkoutSessionEntity`: Room `@Entity(tableName = "workout_sessions")` storing session stats (`totalCalories: Int`, `historyPoints: List<TrainingHistoryPoint>`).
- `WorkoutConverters`: Room `@TypeConverter` converting `List<TrainingHistoryPoint>` to/from a compact serialized string.
- `WorkoutSessionDao`: Room `@Dao` for queries (`getAllSessions`) and inserts (`insertSession`).
- `WorkoutDatabase`: Room Database singleton provider (`getDatabase(context)`) with `@TypeConverters(WorkoutConverters::class)`.
- `WorkoutRepository`: Domain repository executing database interactions asynchronously via Coroutines.

### 6. UI Layer (`com.diegolopmon.freevolaba`)
- `MainActivity.kt`: Top dashboard (`TopDashboard` with 7 `MetricCard` items: RPM, HR, Speed, Distance, Power, Calories [Int], Resistance), session controls, and `WorkoutSummaryDialog` host.
- `ui/TrainingChart.kt`: Custom Canvas chart visualizing RPM, Resistance, and Heart Rate over time.
- `ui/WorkoutSummaryDialog.kt`: Material 3 Compose dialog displaying a two-column summary layout upon pressing Stop (Left: session `TrainingChart`, Right: stacked session metrics).

---

## 📐 Telemetry Formulas & Mathematical Derivations

### 1. Cadence / RPM (`calculateRPM`)
- **Raw Sensor Behavior:** Values below `DEADBAND = 600` represent background noise or a stopped flywheel.
- **Formula:** 
  $$\text{RPM} = \begin{cases} 0 & \text{if } \text{rawRPM} < 600 \\ \left\lfloor \frac{\text{rawRPM} + 50}{100} \right\rfloor & \text{if } \text{rawRPM} \ge 600 \end{cases}$$
- **Explanation:** Dividing by $100$ scales raw optical sensor pulses to actual flywheel RPM, with $+50$ providing integer rounding.

---

### 2. Resistance Level (`calculateResistance`)
- **Raw Sensor Range:** The physical magnetic brake produces raw analog values bounded between $500$ (maximum resistance, Level 10) and $1800$ (minimum resistance, Level 1).
- **Formula:** Inverted linear clamping mapped to integer range $[1, 10]$:
  $$\text{clamped} = \text{coerceIn}(\text{rawResistance}, 500, 1800)$$
  $$\text{Level} = 1 + \left\lfloor \frac{(1800 - \text{clamped}) \times 9}{1800 - 500} \right\rfloor$$
- **Explanation:** Inverts the analog raw value so that lower resistance sensor readings correspond to higher user resistance levels (1 to 10).

---

### 3. Speed in km/h (`calculateSpeed`)
- **Virtual Gear:** $V_{\text{gear}} = 3.0\text{ meters/revolution}$ (standard exercise bike virtual ratio).
- **Formula:** 
  $$\text{Speed (km/h)} = \text{round}\left(\text{RPM} \times 3.0\text{ m/rev} \times 0.06\right)$$
- **Derivation:** $0.06$ converts meters per minute ($\text{m/min}$) to kilometers per hour ($\text{km/h}$):
  $$\frac{60\text{ min/hour}}{1000\text{ meters/km}} = 0.06\text{ (km/h) / (m/min)}$$

---

### 4. Distance Delta in meters (`calculateDeltaDistance`)
- **Formula:**
  $$\Delta \text{Distance (m)} = \text{RPM} \times 3.0\text{ m/rev} \times \left(\frac{\Delta t\text{ (ms)}}{60000\text{ ms/min}}\right)$$
  $$\text{Distance}_{\text{new}} = \text{round}\left(\text{Distance}_{\text{prev}} + \Delta \text{Distance}\right)$$
- **Explanation:** Computes distance covered during the elapsed time interval $\Delta t$ in milliseconds based on current RPM.

---

### 5. Mechanical Power Output in Watts (`calculatePower`)
- **Ergometer Model:** Based on the standard **Monark mechanical ergometer** formula.
- **Formula:**
  $$\text{Power (Watts)} = \text{BrakeForce (kp)} \times \text{FlywheelDistance (m/rev)} \times \text{RPM} \times K_{\text{conversion}}$$
- **Parameters & Constants:**
  - $\text{BrakeForce (kp)} = \text{ResistanceLevel} \times 0.3\text{ kp/level}$ (Level $1 = 0.3\text{ kp}$, Level $10 = 3.0\text{ kp}$).
  - $\text{FlywheelDistance} = 6.0\text{ m/rev}$ (Monark flywheel circumference/gear convention).
  - $K_{\text{conversion}} = 0.1635\text{ Watts} / (\text{kp}\cdot\text{m/min})$, derived from $1\text{ kp}\cdot\text{m/min} = \frac{9.80665\text{ N}}{60\text{ s}} \approx 0.16344\text{ Watts}$.

---

### 6. Calorie Expenditure in kcal (`calculateCalories`)
- **Human Metabolic Efficiency:** Human cycling efficiency is approximately $21\%$ ($0.21$).
- **Formula:**
  $$\text{Metabolic Watts} = \frac{\text{Mechanical Power (W)}}{0.21}$$
  $$\Delta \text{Energy (Joules)} = \text{Metabolic Watts} \times \left(\frac{\Delta t\text{ (ms)}}{1000\text{ ms/s}}\right)$$
  $$\Delta \text{kcal} = \frac{\Delta \text{Energy (Joules)}}{4184\text{ Joules/kcal}}$$
  $$\text{Calories}_{\text{new}} = \text{Calories}_{\text{prev}} + \Delta \text{kcal}$$
- **Explanation:** Converts mechanical energy output into gross human metabolic energy consumed over the elapsed millisecond interval $\Delta t$. During the live session it accumulates as `Double`, and upon summary generation it rounds to `Int`.

---

## 📋 Task & Feature Request Guidelines

When requested to define a new feature, Kanban card, or GitHub Issue:
1. **Follow the Template:** Format the task strictly following `.github/ISSUE_TEMPLATE/feature_request.md`.
2. **Language:** Provide the output in English inside a copyable Markdown code block.
3. **Structure Requirements:**
   - **Title:** `feat(scope): Short title`
   - **User Story:** `As a [user], I want to [goal], So that [benefit].`
   - **Description:** Clear summary of triggers, UI flow, and expectations.
   - **Acceptance Criteria:** Checkbox list (`- [ ]`) covering UI, state, database/persistence, and user interactions.
   - **Technical Implementation Notes:** Collapsible `<details><summary>🛠️ Technical Implementation Notes</summary>...</details>` block detailing architecture, entities, DAOs, services, and Compose UI elements.

---

## 💡 Developer & Coding Guidelines

1. **Jetpack Compose Best Practices:**
   - Use `StateFlow` and Compose state collectors (`collectAsStateWithLifecycle()`).
   - Keep UI components modular and previewable with `@Preview`.
2. **Testing:**
   - Write local unit tests in `src/test/` using **JUnit 5 Jupiter** (`WorkoutSummaryTest.kt`). Run tests via `./gradlew test`.
3. **Resource Management:**
   - Avoid global `import android.R`. Always use `com.diegolopmon.freevolaba.R` or qualified `android.R.drawable...`.
   - Store UI strings in `res/values/strings.xml`, `res/values-es/strings.xml`, and `res/values-en/strings.xml`.
4. **Thread Safety:**
   - Telemetry calculation callbacks from sensors may execute on background threads. Ensure thread-safety for mutable fields.
