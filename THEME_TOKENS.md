# Kinetic Routines — Theme Tokens

**Sources:**  
`app/src/main/java/com/example/ui/theme/Color.kt`  
`app/src/main/java/com/example/ui/theme/Theme.kt`  
`app/src/main/java/com/example/ui/theme/Type.kt`  
`app/src/main/java/com/example/ui/theme/HabitColor.kt`  
`app/src/main/java/com/example/ui/theme/HabitIcons.kt`  

Cross-refs: shapes/spacing sampled from `ui/screens/*` and `ui/components/*`.  
All hex values are `#RRGGBB` unless noted. Compose `Color(0xAARRGGBB)` → hex below omits opaque `FF` alpha.

---

## 1. Brand / semantic palette (`Color.kt`)

| Token ID | Kotlin name | Hex | Typical role / usage |
|----------|-------------|-----|----------------------|
| COL-BRAND-1 | `VoidBackground` | `#0D1117` | Dark scheme `background` |
| COL-BRAND-2 | `CardSurface` | `#161B22` | Dark scheme `surface` |
| COL-BRAND-3 | `CardSurfaceElevated` | `#21262D` | Dark `primaryContainer`, `secondaryContainer`, `surfaceVariant` |
| COL-BRAND-4 | `SurfaceBorder` | `#30363D` | Dark scheme `outline` |
| COL-BRAND-5 | `KineticEmerald` | `#10B981` | Dark `primary`; default habit accent; parse fallback |
| COL-BRAND-6 | `KineticEmeraldGlow` | `#34D399` | Dark `onPrimaryContainer` |
| COL-BRAND-7 | `KineticViolet` | `#8B5CF6` | Dark `secondary`; cue field accent; stack headers |
| COL-BRAND-8 | `KineticAmber` | `#F59E0B` | Dark `tertiary`; sample habit color |
| COL-BRAND-9 | `KineticCoral` | `#F43F5E` | Accent (charts/components; not in Material scheme roles) |
| COL-BRAND-10 | `KineticCyan` | `#06B6D4` | Accent (same) |
| COL-BRAND-11 | `KineticRose` | `#EC4899` | Accent (same) |
| COL-BRAND-12 | `KineticBlue` | `#3B82F6` | Accent (same) |
| COL-BRAND-13 | `TextPrimary` | `#F8FBFF` | Dark `onBackground` / `onSurface` |
| COL-BRAND-14 | `TextSecondary` | `#C2CCD4` | Dark `onSurfaceVariant` |
| COL-BRAND-15 | `TextMuted` | `#9AA7B2` | Secondary text in custom components |
| COL-BRAND-16 | `FloorGold` | `#D97706` | Floor labels/controls (editor, quick log, detail) |
| COL-BRAND-17 | `RestShieldBlue` | `#2563EB` | Rest-day / Rest Shield UI |
| COL-BRAND-18 | `MissedRed` | `#DC2626` | Dark `error`; validation; miss states |

Also used as literals in `Theme.kt`: `Color.Black`, `Color.White`, and light-scheme hexes below.

---

## 2. Material 3 color schemes (`Theme.kt`)

### 2.1 Dark — `KineticDarkColorScheme` (default preference)

| Role | Hex / token | Source |
|------|-------------|--------|
| `primary` | `#10B981` (`KineticEmerald`) | COL-BRAND-5 |
| `onPrimary` | `#000000` | `Color.Black` |
| `primaryContainer` | `#21262D` (`CardSurfaceElevated`) | COL-BRAND-3 |
| `onPrimaryContainer` | `#34D399` (`KineticEmeraldGlow`) | COL-BRAND-6 |
| `secondary` | `#8B5CF6` (`KineticViolet`) | COL-BRAND-7 |
| `onSecondary` | `#FFFFFF` | `Color.White` |
| `secondaryContainer` | `#21262D` | COL-BRAND-3 |
| `onSecondaryContainer` | `#DDD6FE` | literal in Theme.kt |
| `tertiary` | `#F59E0B` (`KineticAmber`) | COL-BRAND-8 |
| `onTertiary` | `#000000` | `Color.Black` |
| `background` | `#0D1117` (`VoidBackground`) | COL-BRAND-1 |
| `onBackground` | `#F8FBFF` (`TextPrimary`) | COL-BRAND-13 |
| `surface` | `#161B22` (`CardSurface`) | COL-BRAND-2 |
| `onSurface` | `#F8FBFF` | COL-BRAND-13 |
| `surfaceVariant` | `#21262D` | COL-BRAND-3 |
| `onSurfaceVariant` | `#C2CCD4` (`TextSecondary`) | COL-BRAND-14 |
| `outline` | `#30363D` (`SurfaceBorder`) | COL-BRAND-4 |
| `error` | `#DC2626` (`MissedRed`) | COL-BRAND-18 |
| `onError` | `#FFFFFF` | `Color.White` |

**Where used:** Almost all screens via `MaterialTheme.colorScheme.*`. Chrome accents in editor/create sheet use `primary` / `onPrimary`. System bars: transparent + dark content style when dark (`SystemBarStyle.dark`).

### 2.2 Light — `KineticLightColorScheme`

| Role | Hex | Notes |
|------|-----|-------|
| `primary` | `#059669` | Darker emerald than brand token |
| `onPrimary` | `#FFFFFF` | |
| `primaryContainer` | `#D1FAE5` | |
| `onPrimaryContainer` | `#065F46` | |
| `secondary` | `#7C3AED` | Violet |
| `onSecondary` | `#FFFFFF` | |
| `secondaryContainer` | `#EDE9FE` | |
| `onSecondaryContainer` | `#5B21B6` | |
| `tertiary` | `#D97706` | Same hex as `FloorGold` |
| `onTertiary` | `#FFFFFF` | |
| `background` | `#F8FAFC` | Slate-50-ish |
| `onBackground` | `#0F172A` | |
| `surface` | `#FFFFFF` | |
| `onSurface` | `#0F172A` | |
| `surfaceVariant` | `#F1F5F9` | |
| `onSurfaceVariant` | `#334155` | |
| `outline` | `#CBD5E1` | |
| `error` | `#DC2626` | Same as `MissedRed` |
| `onError` | `#FFFFFF` | |

**Theme modes** (`ThemeMode`): `DARK` (“Dark”), `LIGHT` (“Light”), `SYSTEM` (“System Default”). DataStore default = **DARK**.  
`MyApplicationTheme` forces `ThemeMode.DARK`.

### 2.3 Semantic colors used outside Material roles

| Token | Hex | Usage examples |
|-------|-----|----------------|
| `FloorGold` | `#D97706` | Floor field labels; floor actions |
| `RestShieldBlue` | `#2563EB` | Rest day cells / shield UI |
| `MissedRed` | `#DC2626` | Miss / validation / delete emphasis |
| `KineticViolet` | `#8B5CF6` | Cue field tint; stack section accents |
| `KineticEmerald` | `#10B981` | Reminder switch; success chrome when not using scheme.primary |
| Habit `colorHex` | per-habit | Cards, day cells, gauges, charts (not editor chrome) |

---

## 3. Habit color presets (`HabitColor.kt`)

`DEFAULT_HABIT_COLOR_HEX` = `#10B981`  
Stored on habit as `#RRGGBB`; used on cards / activity chrome only (not Material scheme).

| # | Hex | Comment in source |
|---|-----|-------------------|
| 1 | `#10B981` | emerald |
| 2 | `#14B8A6` | teal |
| 3 | `#06B6D4` | cyan |
| 4 | `#0EA5E9` | sky |
| 5 | `#3B82F6` | blue |
| 6 | `#6366F1` | indigo |
| 7 | `#8B5CF6` | violet |
| 8 | `#A855F7` | purple |
| 9 | `#EC4899` | pink |
| 10 | `#F43F5E` | rose |
| 11 | `#EF4444` | red |
| 12 | `#F97316` | orange |
| 13 | `#F59E0B` | amber |
| 14 | `#EAB308` | yellow |
| 15 | `#84CC16` | lime |
| 16 | `#22C55E` | green |

Helpers: `parseHabitColor`, `normalizeHex` (3- or 6-digit), `colorToHex`, `hsvToColor`, `colorToHsv` — custom color dialog in editor.

---

## 4. Typography (`Type.kt`)

### 4.1 Font families

| Family | Resource | Weights loaded | Use |
|--------|----------|----------------|-----|
| `PlusJakartaSans` | `R.font.plus_jakarta_sans` (variable) | Normal (400), Medium (500), SemiBold (600), Bold (700) | All Material3 `Typography` roles |
| `Fraunces` | `R.font.fraunces` (variable) | SemiBold (600), Bold (700) | App wordmark “Routines” (dashboard, about) — **not** in Material Typography |

License note in source: SIL Open Font License.

### 4.2 Material3 type scale

`Typography = Typography().withAppType()` — **only `fontFamily` is overridden** to Plus Jakarta Sans. Size/weight/lineHeight/letterSpacing remain **Material3 defaults** (Compose Material3 / M3 type scale):

| Style | Size / line height (sp) | Default weight (M3) | App font |
|-------|-------------------------|---------------------|----------|
| `displayLarge` | 57 / 64 | Regular | PlusJakartaSans |
| `displayMedium` | 45 / 52 | Regular | PlusJakartaSans |
| `displaySmall` | 36 / 44 | Regular | PlusJakartaSans |
| `headlineLarge` | 32 / 40 | Regular | PlusJakartaSans |
| `headlineMedium` | 28 / 36 | Regular | PlusJakartaSans |
| `headlineSmall` | 24 / 32 | Regular | PlusJakartaSans |
| `titleLarge` | 22 / 28 | Medium | PlusJakartaSans |
| `titleMedium` | 16 / 24 | Medium | PlusJakartaSans |
| `titleSmall` | 14 / 20 | Medium | PlusJakartaSans |
| `bodyLarge` | 16 / 24 | Regular | PlusJakartaSans |
| `bodyMedium` | 14 / 20 | Regular | PlusJakartaSans |
| `bodySmall` | 12 / 16 | Regular | PlusJakartaSans |
| `labelLarge` | 14 / 20 | Medium | PlusJakartaSans |
| `labelMedium` | 12 / 16 | Medium | PlusJakartaSans |
| `labelSmall` | 11 / 16 | Medium | PlusJakartaSans |

### 4.3 Ad-hoc text styles (common hardcoded sizes)

Many screens set `fontSize` / `FontWeight` directly instead of Material roles:

| Context | Typical size | Weight |
|---------|--------------|--------|
| Wordmark “Routines” | 22.sp | Bold + Fraunces |
| Create sheet titles | 20.sp | Bold |
| Editor top bar title | 20.sp | Bold |
| Section / chip labels | 11–13.sp | Bold / SemiBold |
| Supporting / hints | 10–11.sp | Normal |
| Category chip label | 12.sp | Medium/Bold |
| Empty-state body | 12–14.sp | Normal |
| Editor field input | 16.sp | SemiBold |

---

## 5. Shapes / corner radii

No central `Shape` theme object. Radii used in UI (dp):

| Radius | Typical use |
|--------|-------------|
| `2.dp` | Heatmap legend ticks; thin bars |
| `3.dp` | Heatmap cells |
| `4.dp` | Chart bar segments; weekly bars |
| `5–6.dp` | Activity chart chips; table icon wells |
| `8.dp` | Type chips; day cells; cards’ icon wells; friction chips; locked modality box |
| `10.dp` | Brand mark clip; settings chips; metric/activity row corners; quick-log buttons |
| `12.dp` | FAQ items; manage list rows; about cards; sort rows; editor sections |
| `14.dp` | Habit card outer; detail metric cards; about logo |
| `16.dp` | Activity chart panel; heatmap/weekly containers; nudge banner; settings rows |
| `18.dp` | Editor color swatch wells |
| `20.dp` | Category/filter chips; sort mode chips; organize chips |
| `CircleShape` | FAB; small count badges |

Borders: commonly `1.dp` with `outline` or habit-color alpha (0.3–0.45).

---

## 6. Spacing

No design-token object; recurring values from layout code:

| Value | Pattern |
|-------|---------|
| `4.dp` | Tight vertical gaps under titles |
| `6.dp` | Icon–label gaps |
| `8.dp` | Chip/row spacing; list content vertical padding |
| `10.dp` | Title bar icon gap; empty-state spacer |
| `12.dp` | Horizontal padding in chips; FAQ content |
| `14.dp` | LazyColumn item spacing (dashboard); chip/FAQ padding |
| `16.dp` | Screen horizontal content padding; section gaps |
| `20.dp` | Editor / create-sheet horizontal padding |
| `28.dp` | Create sheet bottom padding; FAB icon size related |
| `40.dp` | Empty-state vertical padding |
| `72.dp` | Dashboard list bottom spacer (clear FAB) |

Content paddings: dashboard `PaddingValues(horizontal = 16.dp, vertical = 8.dp)`; FAQ `16.dp` / `12.dp`.

---

## 7. Elevation

| Finding | Detail |
|---------|--------|
| Material `Card` elevation | **Not used** for habit list — cards are bordered clipped boxes (`HabitCard` 14.dp radius + 1.dp outline) |
| FAB | Material3 `FloatingActionButton` default elevation (platform default; not overridden in dashboard) |
| Bottom sheet | `ModalBottomSheet` system elevation |
| Dialogs | `AlertDialog` / custom dialogs — Material defaults |
| App bars | `TopAppBarDefaults` with `containerColor = background` (flat, no custom elevation) |

There is **no** custom elevation token table in theme code.

---

## 8. Habit icon catalog (`HabitIcons.kt`)

Stored as string `Habit.iconName`. Lookup: `habitIconVector(name)` → catalog or **`Icons.Default.Star`** fallback.

| # | Stored name | Material icon (`Icons.Default.*`) |
|---|-------------|-------------------------------------|
| 1 | `SelfImprovement` | SelfImprovement |
| 2 | `WaterDrop` | WaterDrop |
| 3 | `FitnessCenter` | FitnessCenter |
| 4 | `Psychology` | Psychology |
| 5 | `Timer` | Timer |
| 6 | `Bolt` | Bolt |
| 7 | `Favorite` | Favorite |
| 8 | `MenuBook` | MenuBook |
| 9 | `AutoStories` | AutoStories |
| 10 | `Brush` | Brush |
| 11 | `Create` | Create |
| 12 | `People` | People |
| 13 | `Home` | Home |
| 14 | `WbSunny` | WbSunny |
| 15 | `NightsStay` | NightsStay |
| 16 | `Bedtime` | Bedtime |
| 17 | `Spa` | Spa |
| 18 | `DirectionsRun` | DirectionsRun |
| 19 | `DirectionsBike` | DirectionsBike |
| 20 | `Hiking` | Hiking |
| 21 | `Pool` | Pool |
| 22 | `LocalCafe` | LocalCafe |
| 23 | `Restaurant` | Restaurant |
| 24 | `Park` | Park |
| 25 | `Pets` | Pets |
| 26 | `MusicNote` | MusicNote |
| 27 | `Headphones` | Headphones |
| 28 | `Code` | Code |
| 29 | `Laptop` | Laptop |
| 30 | `Phone` | Phone |
| 31 | `Lightbulb` | Lightbulb |
| 32 | `EditNote` | EditNote |
| 33 | `CheckCircle` | CheckCircle |
| 34 | `Flag` | Flag |
| 35 | `LocalFireDepartment` | LocalFireDepartment |
| 36 | `Air` | Air |
| 37 | `Star` | Star |

**Defaults note:** `Habit` model default `iconName = "Sparkles"` is **not** in the catalog → resolves to Star. Editor create default uses `"SelfImprovement"`. Category/stack library defaults use names like `Favorite`, `Folder` (Folder also not in habit catalog — category UI may not use `habitIconVector`).

---

## 9. System chrome

| Item | Behavior |
|------|----------|
| Edge-to-edge | Enabled; status/nav bars transparent |
| Dark bars | `SystemBarStyle.dark(TRANSPARENT)` |
| Light bars | `SystemBarStyle.light(TRANSPARENT, TRANSPARENT)` |
| Window background | Set to `colorScheme.background` |

---

*End of THEME_TOKENS.md*
