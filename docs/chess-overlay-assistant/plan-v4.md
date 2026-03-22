---
feature: chess-overlay-assistant
plan_version: v4
status: ready-for-implementation
brief_ref: docs/chess-overlay-assistant/brief.md
parent_plan: docs/chess-overlay-assistant/plan-v3.md
last_updated: 2026-03-22
---

# Plan v4 — Close Confirmation, Match History, Controls Cleanup

Three independent changes batched into one plan. No ordering dependency between C1/C2/C3 except where noted.

---

## C1 — Close (✕) confirmation dialog on the overlay

### Summary
The ✕ button in `ExpandedOverlayHeader` and `MinimizedOverlayHeader` currently calls `onCloseOverlay` directly. Add an "Are you sure?" dialog before closing.

### Tasks

| # | File | Change |
|---|------|--------|
| C1-1 | `feature/overlay/components/OverlayPanel.kt` | Add `var showCloseConfirmation by remember { mutableStateOf(false) }` inside `OverlayWindowCard`. Pass a lambda `{ showCloseConfirmation = true }` as `onCloseOverlay` to both headers instead of the raw callback. Add an `AlertDialog` (or a custom overlay-themed dialog) at the bottom of the `Card` content, gated on `showCloseConfirmation`, with "Close" (calls real `onCloseOverlay`) and "Cancel" (resets flag) buttons. |

**No other files change.** The dialog state is purely local to `OverlayWindowCard`.

### Open questions
- Q: Should the dialog match the dark overlay theme or use default `AlertDialog`?  
  Decision: Use the dark overlay card colors (`OverlayCardColor` / `OverlaySurfaceColor`) for visual consistency.

---

## C2 — Match history in the main app

### Summary
Persist completed/abandoned games and let the user browse + replay them from the main app.

### Data model

```kotlin
data class MatchRecord(
    val id: String,            // UUID
    val timestamp: Long,       // System.currentTimeMillis()
    val moves: List<MoveRecord>,
    val result: String,        // "checkmate_white", "checkmate_black", "stalemate", "abandoned"
    val assistedSide: Side,
)
```

### Tasks (ordered by dependency)

| # | File (relative to `app/src/main/java/com/rachitgoyal/chesshelper/`) | Change |
|---|------|--------|
| C2-1 | `domain/chess/model/MatchRecord.kt` | **Create.** Data class above. |
| C2-2 | `domain/chess/MatchHistoryRepository.kt` | **Create.** Reads/writes `List<MatchRecord>` to SharedPreferences key `"match_history"` as a JSON array via `org.json`. Methods: `save(record)`, `getAll(): List<MatchRecord>`, `getById(id): MatchRecord?`, `clear()`. |
| C2-3 | `feature/overlay/OverlayBoardViewModel.kt` | Accept optional `MatchHistoryRepository` in constructor. Add `fun saveCurrentGame(result: String)` — builds a `MatchRecord` from `uiState.moveHistory`, `uiState.assistedSide`, and the `result` param, then calls `repository.save(...)`. Guard: no-op if `moveHistory.isEmpty()`. |
| C2-4 | `feature/overlay/OverlayBoardViewModel.kt` | In `onResetBoard()`: call `saveCurrentGame("abandoned")` **before** `store.reset()`. |
| C2-5 | `feature/overlay/OverlayBoardViewModel.kt` | Add `fun saveAndClose(onClose: () -> Unit)` — determines result from `gameStatus` (checkmate→ winner side, stalemate, else "abandoned"), calls `saveCurrentGame(result)`, then `onClose()`. Expose for the close path. |
| C2-6 | `feature/overlay/OverlayBoardViewModel.kt` | In `onSquareTapped` (or `syncFromStore`): after a move that makes `gameStatus.isTerminal`, auto-call `saveCurrentGame(...)` with the correct terminal result. Guard with a `private var gameSaved` flag reset in `onResetBoard`/`onLoadFen`. |
| C2-7 | `feature/overlay/OverlayBoardRoute.kt` | Change `onRequestClose` wiring: instead of passing raw close, call `viewModel.saveAndClose { onRequestClose() }`. |
| C2-8 | `feature/overlay/service/OverlayWindowHost.kt` | No code change needed — the `onRequestClose` lambda flows from Service → Host → Route → ViewModel already. Confirm only. |
| C2-9 | `feature/history/MatchHistoryScreen.kt` | **Create.** Composable that takes `List<MatchRecord>`, shows a `LazyColumn` of cards (timestamp, result badge, move count, assisted side). Each card taps to `onMatchSelected(id)`. |
| C2-10 | `feature/history/MatchReplayScreen.kt` | **Create.** Composable with: `ChessBoard` (reused), forward/back `IconButton`s, move index slider. Accepts a `MatchRecord`, maintains `currentMoveIndex` state, replays board positions by applying moves `0..currentMoveIndex` from an initial board. |
| C2-11 | `feature/history/MatchReplayViewModel.kt` | **Create** (optional, can be stateless). Holds a `ChessGameStore`, applies moves up to index N, exposes `board` / `lastMove` / `checkedKingSquare` for the composable. |
| C2-12 | `ui/app/ChessHelperApp.kt` | Add `MATCH_HISTORY` and `MATCH_REPLAY` to `AppScreen` enum. Add nav wiring: `HomeScreen` gets a "Match history" button; `MATCH_HISTORY` renders `MatchHistoryScreen`; `MATCH_REPLAY` renders `MatchReplayScreen`. Pass `MatchHistoryRepository(context)` down. |
| C2-13 | `ui/app/ChessHelperApp.kt` | In `HomeScreen`: add an `OutlinedButton("Match history")` in the card, below the overlay control buttons. |
| C2-14 | `MainActivity.kt` | Instantiate `MatchHistoryRepository` alongside `AppSettings`, pass to `ChessHelperApp`. |

### Open questions
- Q: Should `MatchReplayScreen` use the existing `ChessBoard` composable directly (overlay-themed dark) or a light-mode variant?  
  Decision: Reuse `ChessBoard` as-is; wrap in a `Surface` with appropriate background. Good enough for v1.
- Q: Max stored matches? Suggest 50 with FIFO eviction in the repository `save()`.

---

## C3 — Move "Play as" into dropdown menu

### Summary
Remove the dedicated `SideSegmentedToggle` row from `OverlayControls`. Add a side-toggle item in the existing `⋯` dropdown. The controls section becomes: `[Show best move (full width)] [⋯ button]` on one row.

### Tasks

| # | File | Change |
|---|------|--------|
| C3-1 | `feature/overlay/components/OverlayControls.kt` | Remove the "Play as" `Text` + `SideSegmentedToggle` from the first `Row`. Move the `⋯` button + `DropdownMenu` into the same `Row` as the `FilledTonalButton`. Layout: `FilledTonalButton(Modifier.weight(1f))` then the `⋯` `Surface`/`IconButton`. |
| C3-2 | `feature/overlay/components/OverlayControls.kt` | Add a new `DropdownMenuItem` at the **top** of the menu: text = `"Play as: ${uiState.assistedSide.displayName}"`, onClick toggles to `opposite()` via `onAssistedSideChanged(uiState.assistedSide.opposite())` and closes menu. |
| C3-3 | `feature/overlay/components/OverlayControls.kt` | Delete the private `SideSegmentedToggle` composable (dead code after C3-1). |

**No other files change.** `onAssistedSideChanged` callback and `OverlayBoardUiState.assistedSide` remain unchanged.

---

## File change summary (alphabetical)

| File | C1 | C2 | C3 |
|------|:--:|:--:|:--:|
| `domain/chess/model/MatchRecord.kt` | | **new** | |
| `domain/chess/MatchHistoryRepository.kt` | | **new** | |
| `feature/history/MatchHistoryScreen.kt` | | **new** | |
| `feature/history/MatchReplayScreen.kt` | | **new** | |
| `feature/history/MatchReplayViewModel.kt` | | **new** | |
| `feature/overlay/OverlayBoardRoute.kt` | | edit | |
| `feature/overlay/OverlayBoardViewModel.kt` | | edit | |
| `feature/overlay/components/OverlayControls.kt` | | | edit |
| `feature/overlay/components/OverlayPanel.kt` | edit | | |
| `feature/overlay/service/OverlayWindowHost.kt` | | verify | |
| `ui/app/ChessHelperApp.kt` | | edit | |
| `MainActivity.kt` | | edit | |

## Suggested implementation order

1. **C1** (1 file, self-contained)
2. **C3** (1 file, self-contained)
3. **C2-1 → C2-2** (model + repository)
4. **C2-3 → C2-6** (ViewModel save logic)
5. **C2-7** (Route close wiring)
6. **C2-9 → C2-11** (history + replay screens)
7. **C2-12 → C2-14** (nav + MainActivity wiring)

## Acceptance checklist

- [ ] ✕ button shows confirmation dialog before closing overlay
- [ ] Games auto-save on checkmate, stalemate, "New game", and overlay close
- [ ] Main app shows match history list with timestamps and results
- [ ] Tapping a match opens replay with forward/back step-through
- [ ] "Play as" row removed; side toggle appears as first dropdown item
- [ ] Controls section is a single row: [best move button] [⋯]
- [ ] Existing tests pass unchanged

