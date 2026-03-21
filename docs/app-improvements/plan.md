---
feature: app-improvements
plan_version: v1
status: ready-for-implementation
brief_ref: docs/app-improvements/brief.md
last_updated: 2026-03-19
---

# Plan v1 — App Improvements

## Overview

Tasks are grouped into three priority tiers.  
Each tier can be implemented independently; within a tier, tasks are ordered by ascending
implementation complexity.

| Tier | Theme | Tasks |
|------|-------|-------|
| **P0** | Quick wins — high value, almost no new architecture | T1 · T2 · T3 |
| **P1** | High-value features — moderate scope, clear payoff | T4 · T5 · T6 · T7 · T8 · T9 |
| **P2** | Stretch / polish — valuable but deferrable | T10 · T11 · T12 · T13 · T14 |

---

## P0 — Quick wins

---

### T1 — Move-history panel

**Brief ref:** A1  
**Effort:** S (half-day)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/MoveRecord.kt` | Read only — confirm fields available for display |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | Add `moveHistory: List<MoveRecord>` field (already tracked; expose it) |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | Populate `moveHistory` from `ChessGameStore` inside `syncFromStore` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt` | Add `MoveHistoryPanel` composable below board; hide in non-EXPANDED mode |
| `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt` | Add: `moveHistory grows after each tap`, `history resets on game reset` |

#### Implementation notes
- `ChessGameStore` already accumulates a `List<MoveRecord>`; call it via `gameStore.currentSnapshot().moveHistory` (or equivalent getter) inside the existing `syncFromStore` helper.
- New `MoveHistoryPanel` composable: `LazyColumn` of paired `(white, black)` move rows with move-number prefix.
- Display format: long-algebraic (`e2-e4`) first; upgrade to SAN in a follow-up if Q1 from the brief is resolved.
- Use `LazyListState.animateScrollToItem` coroutine call inside a `LaunchedEffect(moveHistory.size)` to auto-scroll on each new move.
- Guard the composable with `if (panelMode == PanelMode.EXPANDED && moveHistory.isNotEmpty())`.

#### Acceptance criteria
- [ ] Move history section is visible in expanded overlay after each move pair.
- [ ] Each row shows: move number, white move string, black move string.
- [ ] List auto-scrolls to the latest entry after every move.
- [ ] Section absent in minimised / collapsed mode.
- [ ] ViewModel unit test: history list grows and resets correctly.

---

### T2 — FEN copy-to-clipboard

**Brief ref:** A2  
**Effort:** XS (1–2 hours)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/ChessPositionFenEncoder.kt` | Read only — confirm public `encode(snapshot)` API |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | Add `fun onCopyFenClicked()` — calls `ChessPositionFenEncoder`, writes to `ClipboardManager` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | Add `fenCopied: Boolean = false` for one-shot Snackbar trigger |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayControls.kt` | Add small copy-icon `IconButton`; show in EXPANDED mode only; call `onCopyFenClicked` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt` | Consume `fenCopied` flag to show `Toast` / `LaunchedEffect` Snackbar; reset via VM callback |

#### Implementation notes
- `ClipboardManager` is obtained via `context.getSystemService(Context.CLIPBOARD_SERVICE)`.
- FEN write: `ClipData.newPlainText("FEN", fenString)`.
- Use a one-shot boolean flag `fenCopied` in `UiState` (set to `true` in VM, reset to `false` after the composable consumes it via `LaunchedEffect`).
- No new permissions required.

#### Acceptance criteria
- [ ] "Copy FEN" icon visible in expanded overlay controls row.
- [ ] Tapping copies the correct FEN string for the current position.
- [ ] A brief Toast/Snackbar confirms the copy.
- [ ] Icon absent in minimised / collapsed mode.

---

### T3 — Haptic feedback on moves

**Brief ref:** A3  
**Effort:** XS (1–2 hours)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/settings/AppSettings.kt` | Add `enableHapticFeedback: Boolean` DataStore pref, key `enable_haptic_feedback`, default `true` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | Add `enableHapticFeedback: Boolean = true` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | Read pref in `init`; set in `uiState` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt` | On legal move: call `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)` for normal move, `LongPress` for capture/check |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/settings/SettingsScreen.kt` | Add `enableHapticFeedback` toggle switch alongside existing settings |

#### Implementation notes
- Obtain haptic via `val haptic = LocalHapticFeedback.current` inside `ChessBoard`.
- Distinguish capture by checking whether the destination square held an opponent piece before the move (`snapshot.board[to] != null`).
- Distinguish check from the post-move `GameStatus.CHECK` in `OverlayBoardUiState`.
- Pass `enableHapticFeedback` and `isCapture`/`isCheck` flags from `OverlayPanel` down to `ChessBoard` via lambda or state params.

#### Acceptance criteria
- [ ] Subtle haptic fires on every legal move.
- [ ] Stronger haptic fires on capture or check.
- [ ] No haptic on illegal tap / deselection.
- [ ] `enableHapticFeedback = false` completely suppresses haptics.
- [ ] Setting exposed and persisted in `SettingsScreen`.

---

## P1 — High-value features

---

### T4 — Evaluation bar

**Brief ref:** A4  
**Effort:** M (1 day)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/engine/model/EngineRecommendation.kt` | Add `evalCp: Int? = null`, `evalMate: Int? = null` fields |
| `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt` | Parse last `info … score cp <n>` / `score mate <m>` line during `go movetime`; populate new fields |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | Add `lastEvalCp: Int? = null`, `lastEvalMate: Int? = null` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | Copy eval fields from `EngineRecommendation` into `uiState` on recommendation ready |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt` | Add `EvalBar` composable — thin vertical bar beside the board; white fraction = `(evalCp + 1000) / 2000` clamped to [0,1]; mate = full bar |
| `app/src/test/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveTranslatorTest.kt` | Add: `info score cp` parsing, `info score mate` parsing, last-line-wins semantics |

#### Implementation notes
- The Stockfish `go movetime` output emits multiple `info` lines; capture the **last** one with a `score` token before `bestmove` is received.
- Parse via regex: `Regex("score (cp|mate) (-?\\d+)")`.
- Evaluation is from White's perspective (positive = White better). Flip sign when `assistedSide == BLACK`.
- Eval bar: a `Box` split into two `Spacer`s — white region height proportional to clamped eval fraction, black region fills the rest. Render beside the board as a narrow column (≈8 dp wide).
- Mate score: show the full bar in the winning side's colour with a small "M<n>" label.

#### Acceptance criteria
- [ ] `EngineRecommendation` carries `evalCp`/`evalMate` after each analysis.
- [ ] Eval bar updates after every recommendation.
- [ ] Bar correctly shows advantage orientation for both sides.
- [ ] Mate advantage fills the bar and shows mate-in-N label.
- [ ] New unit tests pass for `info score` parsing.
- [ ] Existing engine/translator tests unchanged.

---

### T5 — Opening name display

**Brief ref:** A5  
**Effort:** M (1 day)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/assets/openings.json` | **New file** — bundled ECO opening data (FEN-prefix → ECO + name); sourced from Lichess `chess-openings` (CC0 licence) |
| `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/OpeningBook.kt` | **New file** — singleton loaded lazily from assets; `fun lookup(fen: String): OpeningEntry?` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | Add `currentOpening: String? = null` (display string: `"B20 · Sicilian Defense"`) |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | After each `syncFromStore`, launch `Dispatchers.Default` coroutine to call `OpeningBook.lookup(currentFen)` and update `uiState.currentOpening` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt` | Display `currentOpening` as a single italic line in the overlay header area when non-null |
| `app/src/test/java/com/rachitgoyal/chesshelper/domain/chess/OpeningBookTest.kt` | **New file** — test: starting position lookup, known ECO lookup, out-of-book returns null |

#### Implementation notes
- Opening JSON format: `[{"eco":"B20","name":"Sicilian Defense","fen":"rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"}, …]`
- FEN-prefix matching: strip the halfmove/fullmove clock fields from the encoded FEN before lookup (match on the first 4 space-delimited parts).
- Lazy-load the JSON into a `Map<String, OpeningEntry>` on the first call; store in a `companion object` to avoid repeated asset reads.
- Limit lookup to positions where `moveHistory.size <= 30` to avoid unnecessary work in endgames.
- Resolve Q2 (licensing) before bundling: Lichess `chess-openings` data is CC0.

#### Acceptance criteria
- [ ] Opening name appears in overlay after moves 1–15 for all major openings.
- [ ] Opening field absent / null once position leaves the known tree.
- [ ] Lookup is off the main thread; no ANR risk.
- [ ] Unit tests pass for known and unknown positions.

---

### T6 — Vector / SVG piece set

**Brief ref:** A6  
**Effort:** M (half-day logic + asset prep)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/res/drawable/piece_*.xml` | **12 new files** — one `VectorDrawable` per piece (wK, wQ, wR, wB, wN, wP, bK, bQ, bR, bB, bN, bP) |
| `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/Piece.kt` | Add `fun drawableRes(): Int` or use a `when` mapping in `ChessBoard.kt` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt` | Replace `Text(piece.emoji)` with `Image(painter = painterResource(piece.drawableRes()), contentDescription = …)` |

#### Implementation notes
- Source: Wikimedia's "Chess" SVG set is CC-BY-SA 3.0 (attribution required in `NOTICES` / `README`).  
  Alternatively, the "Staunty" set from `lichess-org/lila` is AGPL-3 and may require open-sourcing.  
  **Resolve Q5 before committing to a set.** Placeholder: use a generic free-licensed set.
- Convert SVG → Android `VectorDrawable` XML via Android Studio's "Import SVG" tool or `svg2vectordrawable`.
- Naming convention: `piece_wk.xml`, `piece_bq.xml`, etc.
- Ensure `fillColor` values are hardcoded (not referencing theme attributes) so the overlay dark background does not affect piece colour.
- Remove the emoji text piece rendering entirely — no fallback needed.

#### Acceptance criteria
- [ ] All 12 piece types render as vector drawables on the board.
- [ ] Pieces are crisp at the minimum overlay square size (≈40 dp).
- [ ] No text-emoji rendering remains in `ChessBoard.kt`.
- [ ] Licence attribution present in `app/NOTICES` or `README.md`.

---

### T7 — Multi-line analysis

**Brief ref:** A7  
**Effort:** M (1 day)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/settings/AppSettings.kt` | Add `multiPvCount: Int` DataStore pref, key `multi_pv_count`, default `1` |
| `app/src/main/java/com/rachitgoyal/chesshelper/engine/model/EngineRecommendation.kt` | Add `pvLines: List<PvLine> = emptyList()` where `data class PvLine(val rank: Int, val move: String, val evalCp: Int?, val evalMate: Int?)` |
| `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt` | When `multiPvCount > 1`: send `setoption name MultiPV value N` before `go`; parse `info multipv <rank> … score … pv <move>` lines; populate `pvLines` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | Add `pvLines: List<PvLine> = emptyList()` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | Copy `pvLines` from `EngineRecommendation`; handle tapping a PV line to preview that move's arrow via `onPreviewPvLine(pvLine: PvLine)` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt` | Show ranked `PvLine` list below the primary recommendation; each row shows rank, move, eval; tapping previews that move's arrow |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/settings/SettingsScreen.kt` | Add `multiPvCount` selector (1 / 2 / 3) |
| `app/src/test/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveTranslatorTest.kt` | Add: multi-PV line parsing test |

#### Implementation notes
- `MultiPV` must be reset to `1` before each analysis when `multiPvCount == 1` to avoid surprising Stockfish state.
- Parse `info multipv` lines carefully: keep only the **last** `info multipv N` line per rank before `bestmove`.
- `onPreviewPvLine` updates `uiState.recommendation` to the tapped PV move and re-draws the arrow; it does **not** call `onApplyRecommendationClicked`.
- When `multiPvCount == 1` the `pvLines` list is empty and the new UI section is hidden entirely.

#### Acceptance criteria
- [ ] `multiPvCount = 1` (default): UI unchanged from today.
- [ ] `multiPvCount = 3`: overlay shows 3 ranked candidate moves with evaluations.
- [ ] Tapping a PV line previews that line's arrow on the board.
- [ ] Setting persisted and applied from `SettingsScreen`.
- [ ] New multi-PV parsing test passes.

---

### T8 — Onboarding flow

**Brief ref:** A8  
**Effort:** S (half-day)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/settings/AppSettings.kt` | Add `onboardingComplete: Boolean` DataStore pref, key `onboarding_complete`, default `false` |
| `app/src/main/java/com/rachitgoyal/chesshelper/ui/app/ChessHelperApp.kt` | Read `onboardingComplete`; show `OnboardingSheet` when `false` |
| `app/src/main/java/com/rachitgoyal/chesshelper/ui/app/OnboardingSheet.kt` | **New file** — `ModalBottomSheet` explaining the overlay, a "Grant permission" `Button` that launches `ACTION_MANAGE_OVERLAY_PERMISSION`, and a "Skip" text button |
| `app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt` | In `onResume`: if permission now granted, set `onboardingComplete = true` via `AppSettings` |

#### Implementation notes
- Detect overlay permission: `Settings.canDrawOverlays(context)`.
- Deep-link to permission screen: `Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))`.
- Use `ModalBottomSheet` (Material 3) — no extra navigation dependency needed.
- The "Skip" path sets `onboardingComplete = true` immediately so the user is not bothered again.
- Three content elements in the sheet: (1) hero icon + title, (2) two-line explanation, (3) CTA button row.

#### Acceptance criteria
- [ ] Bottom sheet shown on first launch (and on subsequent launches until dismissed or permission granted).
- [ ] "Grant permission" button opens Android's overlay permission settings for the app.
- [ ] Returning to the app after granting permission dismisses / suppresses the sheet.
- [ ] "Skip" permanently suppresses the sheet.
- [ ] No new navigation graph required.

---

### T9 — Accessibility labels on board squares

**Brief ref:** A9  
**Effort:** S (half-day)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt` | Add `Modifier.semantics { contentDescription = squareDescription(file, rank, piece, isSelected, isLegalTarget) }` to each square; add `contentDescription` to move-arrow canvas |

#### Implementation notes
- Helper function `squareDescription(file: Int, rank: Int, piece: Piece?, isSelected: Boolean, isLegalTarget: Boolean): String`:
  ```
  val squareName = "${'a' + file}${rank + 1}"
  val pieceDesc = piece?.let { "${it.side.name} ${it.type.name}" } ?: "empty"
  val stateDesc = when {
      isSelected   -> ", selected"
      isLegalTarget -> ", legal target"
      else         -> ""
  }
  return "$squareName — $pieceDesc$stateDesc"
  ```
- Arrow canvas overlay: wrap in a `Box` with `semantics { contentDescription = "Recommended move: ${from} to ${to}" }`.
- Use `clearAndSetSemantics` for the arrow canvas to avoid the underlying square descriptions being re-read.

#### Acceptance criteria
- [ ] Every square has a meaningful `contentDescription`.
- [ ] Selected and legal-target squares include their state in the description.
- [ ] Recommended-move arrow has a descriptive `contentDescription`.
- [ ] No new dependencies added.

---

## P2 — Stretch / polish

---

### T10 — Portrait/landscape adaptive overlay layout

**Brief ref:** A10  
**Effort:** M (1 day)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowPositioning.kt` | Re-clamp overlay position on `CONFIGURATION_CHANGED` broadcast |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt` | Register `CONFIGURATION_CHANGED` receiver; call positioning re-clamp |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt` | Use `LocalConfiguration.current.orientation` to switch between portrait (board + controls stacked) and landscape (board left, controls right column) layouts |

#### Acceptance criteria
- [ ] Overlay remains fully usable in landscape orientation.
- [ ] Drag / peek-strip behaviour preserved in both orientations.
- [ ] Overlay position re-clamped to new screen bounds on rotation.

---

### T11 — FEN/PGN import

**Brief ref:** A11  
**Effort:** L (1.5 days)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStore.kt` | Add `fun loadFen(fen: String): Result<Unit>` — parses FEN and replaces current position |
| `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/FenParser.kt` | **New file** — pure FEN string → `GameSnapshot` conversion; validates piece placement, side to move, castling rights, en passant |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | Add `fun onLoadFen(fen: String)` — calls `gameStore.loadFen`; updates `uiState`; shows error `Toast` on invalid FEN |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt` | Add "Paste FEN" text field + "Load" button inside expanded controls |
| `app/src/test/java/com/rachitgoyal/chesshelper/domain/chess/FenParserTest.kt` | **New file** — test: starting position, mid-game position, invalid FEN rejection |

#### Scope note
PGN import deferred to a follow-up iteration (resolve Q7 from the brief first).

#### Acceptance criteria
- [ ] Valid FEN loads correctly; board and side-to-move reflect the pasted position.
- [ ] Invalid FEN shows an error; board unchanged.
- [ ] Loaded position flows through the existing legal-move and engine-recommendation paths unchanged.
- [ ] Unit tests cover valid and invalid FEN strings.

---

### T12 — Theme / colour customisation

**Brief ref:** A12  
**Effort:** S (half-day)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/BoardTheme.kt` | **New file** — `enum class BoardTheme { CLASSIC, BLUE, GREEN }` with light/dark square colour pairs |
| `app/src/main/java/com/rachitgoyal/chesshelper/settings/AppSettings.kt` | Add `boardTheme: BoardTheme` DataStore pref (serialised as string), default `CLASSIC` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | Add `boardTheme: BoardTheme = BoardTheme.CLASSIC` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | Read `boardTheme` from `AppSettings` in `init`; expose in `uiState` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt` | Replace hardcoded light/dark square colours with `boardTheme.lightSquareColor` / `boardTheme.darkSquareColor` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/settings/SettingsScreen.kt` | Add `boardTheme` selector (Classic / Blue / Green) |

#### Acceptance criteria
- [ ] Three themes available: Classic (brown), Blue, Green.
- [ ] Board colour updates immediately on settings change without overlay restart.
- [ ] Theme persisted across app launches.

---

### T13 — Sound effects

**Brief ref:** A13  
**Effort:** S (half-day + asset sourcing)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/assets/sounds/move.ogg` | **New asset** — short neutral move click (~50 ms) |
| `app/src/main/assets/sounds/capture.ogg` | **New asset** — heavier capture thud (~80 ms) |
| `app/src/main/assets/sounds/check.ogg` | **New asset** — distinct check alert (~100 ms) |
| `app/src/main/java/com/rachitgoyal/chesshelper/settings/AppSettings.kt` | Add `enableSoundEffects: Boolean` DataStore pref, key `enable_sound_effects`, default `false` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | Add `enableSoundEffects: Boolean = false` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt` | Initialise `SoundPool` lazily; load three samples from assets on first use; release in `onDestroy` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | Emit a `SoundEvent` (sealed class) via `SharedFlow` on move, capture, and check; `OverlayWindowHost` collects and plays via `SoundPool` |
| `app/src/main/java/com/rachitgoyal/chesshelper/feature/settings/SettingsScreen.kt` | Add `enableSoundEffects` toggle |

#### Acceptance criteria
- [ ] Sound effects off by default (opt-in).
- [ ] When enabled: move, capture, and check each play distinct samples.
- [ ] `SoundPool` released when the overlay service is destroyed.
- [ ] No `INTERNET` or `RECORD_AUDIO` permissions added.

---

### T14 — Richer main screen

**Brief ref:** A14  
**Effort:** XS (1–2 hours)

#### Touched files
| File | Change |
|------|--------|
| `app/src/main/java/com/rachitgoyal/chesshelper/ui/app/ChessHelperApp.kt` | Add: app logo `Image`, one-sentence description `Text`, overlay-permission `AssistChip` ("Granted" / "Not granted") that navigates to `ACTION_MANAGE_OVERLAY_PERMISSION` when not granted; retain existing launch/stop + settings controls |

#### Acceptance criteria
- [ ] Main screen shows app name, description, and permission chip.
- [ ] Permission chip reflects live permission state.
- [ ] Tapping the "Not granted" chip opens the system overlay permission page.
- [ ] No new navigation graph required.

---

## Cross-cutting concerns

### Testing strategy (all tiers)
- **Unit tests** for every new domain/engine change (T4 score parsing, T5 opening lookup, T11 FEN
  parser, T7 multi-PV parsing).
- **ViewModel tests** for every new `UiState` field and `ViewModel` event (T1 history, T3 haptic flag,
  T7 PV line tap, T11 load-FEN).
- **No instrumented tests required** for P0–P1 scope; device/emulator validation is a manual smoke
  pass.

### Dependency additions
| Task | New dependency | Justification |
|------|---------------|---------------|
| T5 | None — JSON parsed via `org.json.JSONObject` (already in Android SDK) | Zero-dep opening book |
| T6 | None — vector drawables via `androidx.compose.ui:ui` already present | |
| T8 | `androidx.compose.material3:material3` `ModalBottomSheet` — already in project | |
| T13 | None — `android.media.SoundPool` (platform API) | |

No net-new library additions required for any task in this plan.

### File-change summary (all tasks)

| File | Tasks that touch it |
|------|---------------------|
| `AppSettings.kt` | T3, T7, T8, T12, T13 |
| `OverlayBoardUiState.kt` | T1, T2, T3, T4, T5, T7, T12, T13 |
| `OverlayBoardViewModel.kt` | T1, T2, T3, T4, T5, T7, T8, T11 |
| `OverlayPanel.kt` | T1, T2, T4, T7, T10, T11 |
| `ChessBoard.kt` | T3, T6, T9 |
| `OverlayControls.kt` | T2 |
| `SettingsScreen.kt` | T3, T7, T12, T13 |
| `OverlayWindowHost.kt` | T10, T13 |
| `StockfishMoveRecommendationEngine.kt` | T4, T7 |
| `EngineRecommendation.kt` | T4, T7 |
| `ChessHelperApp.kt` | T8, T14 |
| `MainActivity.kt` | T8 |

### Recommended implementation order (within each tier)

**P0:** T2 → T3 → T1  
*(FEN copy is the smallest and doesn't touch ChessBoard; haptics are self-contained; history panel is
slightly larger and benefits from haptics/copy being already merged.)*

**P1:** T9 → T8 → T6 → T4 → T5 → T7  
*(Accessibility and onboarding are independent quick wins; piece set is a contained visual change;
eval bar + opening + multi-PV build on the Stockfish output stream.)*

**P2:** T14 → T12 → T10 → T13 → T11  
*(Main screen and theme are isolated UI changes; landscape layout is moderate; sounds need asset
sourcing; FEN import is the largest task.)*

