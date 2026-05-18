<p align="center">
  <img src="src/main/resources/papalugo-logo.svg" alt="Papalugo" width="180"/>
</p>

<p align="center">
  <strong>Papalugo.com</strong> — connecting the abstract with human
</p>

---

# Project Color Bar — IntelliJ Plugin

Color your IDE title bar and window borders **per project** for instant visual identification.

---

## What it does

| Without plugin | With plugin |
|---|---|
| All projects look identical | Each project has its own color stripe on the title bar and border |

- Assigns a custom color to the top band and borders of the IDE window
- Color is **saved per project** in `.idea/projectColorBar.xml` and restored on next open
- Preset palette of 10 colors + full custom color picker
- Works on **Windows**, **macOS**, and **Linux**

---

## Build & Install

### Prerequisites

- **JDK 17+** (JDK 21 recommended)
- **Gradle 8.x** (bundled via wrapper)

### 1. Build the plugin

```bash
./gradlew buildPlugin
```

The `.zip` file will be generated at:
```
build/distributions/project-color-bar-1.0.0.zip
```

### 2. Install in IntelliJ IDEA

1. Open IntelliJ IDEA
2. Go to **Settings → Plugins**
3. Click the **⚙ gear icon → Install Plugin from Disk…**
4. Select the `.zip` file from `build/distributions/`
5. Restart the IDE when prompted

---

## Usage

1. Open any project
2. Go to **Tools → Set Project Color…**
3. Pick a color from the preset swatches or choose a custom one
4. The title bar and borders update immediately
5. To remove: **Tools → Clear Project Color**

---

## How it works

The plugin hooks into the IDE window lifecycle:

- `ProjectColorSettings` — `PersistentStateComponent` that stores the color in `.idea/projectColorBar.xml`
- `ProjectColorStartupActivity` — `ProjectActivity` that reapplies the color when a project opens
- `ProjectColorApplier` — paints a `GradientPaint` band on the glass pane and adds a `LineBorder` to the content pane
- On macOS, it also tints the root pane background which bleeds into the native unified title bar

---

## Compatibility

| IntelliJ version | Supported |
|-----------------|---|
| 2024.1+         | ✅ |
| 2026.x          | ✅ (change `intellijIdeaCommunity("2023.3")` in `build.gradle.kts`) |

---

## Development (run IDE sandbox)

```bash
./gradlew runIde
```

This launches a sandboxed IntelliJ instance with the plugin already installed.

---

## Project structure

```
src/main/kotlin/com/projectcolor/
├── ProjectColorSettings.kt        # Persistent state per project
├── ProjectColorApplier.kt         # Painting logic (band + border)
├── ProjectColorStartupActivity.kt # Auto-applies color on project open
├── SetProjectColorAction.kt       # "Set Project Color…" menu action
└── ClearProjectColorAction.kt     # "Clear Project Color" menu action

src/main/resources/
├── META-INF/plugin.xml            # Plugin descriptor
└── icons/palette.svg              # Toolbar icon
```
