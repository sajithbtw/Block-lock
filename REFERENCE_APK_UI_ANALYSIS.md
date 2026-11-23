# Reference APK UI analysis — ScrollLock

Source inspected: user-provided `com.hypenet.focused.apk` (6,? MB APK; compiled Android resources).

## What was verified from compiled resources

The APK contains 131 compiled layout XML resources. Important layouts include:

- `_activity_home.xml`: custom `SwipeSections` root, Material app bar, custom `BlockLogo`, four included section layouts, custom bottom navigation and floating action button.
- `_include_bag.xml`: `SectionStack`, `SquareContainer`, Material cards, custom `TileDisplay` / purchase tiles.
- `_include_control.xml`: `SectionStack`, multiple `HapticCard` controls and a custom `SledSwitch`.
- `_include_session2.xml`: square-number displays, preset tiles, custom `HapticSlider`, level graph and rotary wheel.
- `_include_stats.xml`: custom `StatisticsGraph`, tile display, summary cards and rotary wheel.
- `_activity_settings.xml`: theme selector, haptic switch cards and paired settings tiles.
- `_activity_tower.xml`: tower/level display, four statistic cards and a custom bottom slide-to-start control.
- `_sheet_permission.xml`: permission bottom sheet with illustration, title, explanatory text, action buttons and consent control.
- `_slide_to_start.xml`: dedicated slide handle and track.

The home layout includes a custom bottom navigation implementation. The resource names in the APK identify five destinations: Bag, Control, Session, Stats and Settings.

## Verified visual tokens

Several values are explicit in compiled layout attributes:

- Main charcoal: `#1E1C1A`
- Orange accent: `#F55314`
- Muted warm grey: `#6E6862`
- White primary text: `#FFFFFF`
- A warm light surface also appears as `#E8E3DF`

The APK uses MaterialCardView surfaces with rounded corners, custom widgets and animated vector/drawable assets. The custom widgets include `TileDisplay`, `HapticCard`, `HapticPreset`, `HapticSlider`, `SledSwitch`, `RotaryWheel`, `StatisticsGraph`, `BlockLogo` and `SlideToStart`.

## Rebuild scope in this repository

The current ScrollLock implementation is a lightweight native Android prototype. This commit updates its main screen to reproduce the reference app's five-section information architecture and dark warm palette while retaining ScrollLock's existing Accessibility-based app interruption and local session preferences.

This is a functional visual recreation, not a byte-for-byte clone: proprietary custom widget rendering, animations and the original app's exact dimensions/typography require deeper resource-table decoding and runtime comparison. The recreated stats screen is a UI placeholder; persistent analytics are not implemented.
