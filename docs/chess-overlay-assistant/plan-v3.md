---
feature: chess-overlay-assistant
plan_version: v3
status: ready-for-implementation
brief_ref: docs/chess-overlay-assistant/brief.md
last_updated: 2026-03-19
---

# Plan v3 — Streamlined Move Workflow

## Problem

Per-turn friction removes the joy from using the overlay. Every turn currently requires:

```
expand overlay  →  enter opponent move (2 taps)  →  tap "Show Best Move"  →  wait 5 s  →  read  →  minimize  →  play move
```

The two pure-friction steps are **tapping "Show Best Move"** and **manually minimizing**. With
`autoApplyBestMove` already existing, both can be automated so the entire per-turn overhead collapses to:

```
expand overlay  →  enter opponent move (2 taps)  →  [auto-analyze → auto-apply → auto-minimize]  →  play move
```

## Scope — 3 composable settings (all default ON)

| Setting | Default | Trigger |
|---------|---------|---------|
| `autoAnalyzeOnOpponentMove` | `true` | Fire `onRecommendClicked()` when a move passes the turn to `assistedSide` |
| `autoMinimizeAfterMove` | `true` | Collapse to `MINIMIZED` after `onApplyRecommendationClicked()` succeeds while `EXPANDED` |
| `autoApplyBestMove` | `true` | (existing) Apply recommendation without extra tap |

## Tasks

### T1 — `AppSettings`: add two new prefs keys
**File:** `app/src/main/java/com/rachitgoyal/chesshelper/settings/AppSettings.kt`

Add alongside `autoApplyBestMove`:
- `autoAnalyzeOnOpponentMove` — key `auto_analyze_on_opponent_move`, default `true`
- `autoMinimizeAfterMove` — key `auto_minimize_after_move`, default `true`

### T2 — `OverlayBoardUiState`: expose both settings as fields
**File:** `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`

Add alongside `autoApplyBestMove: Boolean = true`:
```kotlin
val autoAnalyzeOnOpponentMove: Boolean = true,
val autoMinimizeAfterMove: Boolean = true,
```

### T3 — `OverlayBoardViewModel`: auto-analyze trigger in `onSquareTapped`
**File:** `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`

After the existing `syncFromStore(...)` call in `onSquareTapped`, add:
```kotlin
if (moveMade
    && uiState.sideToMove == uiState.assistedSide
    && uiState.autoAnalyzeOnOpponentMove
    && uiState.canRecommend) {
    onRecommendClicked()
}
```
`canRecommend` already guards: not loading, not game over, side matches, no active recommendation.

### T4 — `OverlayBoardViewModel`: auto-minimize trigger in `onApplyRecommendationClicked`
**File:** `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`

After the `syncFromStore(...)` success call, add:
```kotlin
if (uiState.autoMinimizeAfterMove && uiState.panelMode == PanelMode.EXPANDED) {
    uiState = uiState.copy(panelMode = PanelMode.MINIMIZED)
}
```
Must be placed **after** `syncFromStore` so board/recommendation state is already cleared.

### T5 — `OverlayBoardViewModel`: init + `onRecommendClicked` refresh
- In `init`: set `autoAnalyzeOnOpponentMove` and `autoMinimizeAfterMove` from `appSettings`
  (pattern: `appSettings?.autoAnalyzeOnOpponentMove ?: false`; false default keeps tests safe).
- In `onRecommendClicked`: capture both values at request-start and propagate into `uiState.copy()`
  alongside `autoApplyBestMove`, so a mid-game settings change takes effect on the next turn.

### T6 — `SettingsScreen`: two new toggle cards
**File:** `app/src/main/java/com/rachitgoyal/chesshelper/feature/settings/SettingsScreen.kt`

Under the "Overlay" section, after the existing `autoApplyBestMove` card, add:

**Card 1 — Auto-analyze after opponent moves**
- Label: "Auto-analyze after opponent moves"
- Description: "Stockfish starts calculating the moment you finish entering the opponent's move. No need to tap 'Show Best Move'."

**Card 2 — Auto-minimize after move applied**
- Label: "Auto-minimize after move applied"
- Description: "Collapses the overlay automatically once the best move is played on the board, so it's out of the way when you make your move."

### T7 — Tests
**File:** `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`

Add two `internal` test-only constructor parameters to `OverlayBoardViewModel`:
```kotlin
internal val autoAnalyzeForTest: Boolean = false,
internal val autoMinimizeForTest: Boolean = false,
```
The ViewModel uses these when `appSettings` is `null`. Defaults are `false`, so **all existing tests pass unchanged**.

New tests (4):

1. `autoAnalyzeFiresAfterOpponentMoveWhenEnabled`
   — `assistedSide=BLACK`, `autoAnalyzeForTest=true`
   — enter white move e2→e4 (turn passes to BLACK)
   — assert `recommendationState == LOADING` without calling `onRecommendClicked()`

2. `autoAnalyzeDoesNotFireWhenDisabled`
   — same setup, `autoAnalyzeForTest=false`
   — assert `recommendationState == IDLE`

3. `autoMinimizeCollapsesAfterApplyWhenEnabled`
   — `autoMinimizeForTest=true`, `autoApplyBestMove` intentionally not set (false by default in tests)
   — panel `EXPANDED`, get recommendation via `stubEngine`, call `onApplyRecommendationClicked()`
   — assert `panelMode == MINIMIZED`

4. `autoMinimizeDoesNotFireWhenDisabled`
   — `autoMinimizeForTest=false`, same flow
   — assert `panelMode == EXPANDED`

## Interaction Matrix

| `autoAnalyze` | `autoApply` | `autoMinimize` | Full-turn result |
|---|---|---|---|
| ON | ON | ON | Enter move → analyze → apply → minimize. Zero extra taps. |
| ON | ON | OFF | Analyze + apply automatic; overlay stays expanded. |
| ON | OFF | — | Analyze fires; user still taps "Play suggested move"; no auto-minimize. |
| OFF | — | — | All manual (current behavior). |

## Acceptance Checklist

- [ ] Entering the opponent's move fires Stockfish analysis automatically (`autoAnalyzeOnOpponentMove` ON)
- [ ] "Analyzing…" is visible in the minimized bar if the overlay happens to be minimized during analysis
- [ ] Overlay collapses to minimized after move applied (`autoMinimizeAfterMove` + `autoApplyBestMove` both ON)
- [ ] Both new settings appear in the main-app Settings screen and persist across restarts
- [ ] Toggling a setting mid-game takes effect on the very next turn
- [ ] All existing `OverlayBoardViewModelTest` tests pass unchanged
- [ ] 4 new tests pass

## Files Changed

| File | Change |
|------|--------|
| `settings/AppSettings.kt` | +2 prefs keys |
| `feature/overlay/OverlayBoardUiState.kt` | +2 Boolean fields |
| `feature/overlay/OverlayBoardViewModel.kt` | auto-analyze in `onSquareTapped`, auto-minimize in `onApplyRecommendationClicked`, refresh in `init` + `onRecommendClicked`, +2 internal test params |
| `feature/settings/SettingsScreen.kt` | +2 toggle cards |
| `OverlayBoardViewModelTest.kt` | +4 new tests |

**No changes to:** `ChessRules`, `ChessGameStore`, `StockfishMoveRecommendationEngine`, `OverlayWindowHost`, `OverlayControls`, `ChessBoard`, manifest, Gradle.

