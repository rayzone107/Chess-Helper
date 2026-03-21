---
feature: app-improvements
stage: planning
status: brief-complete
input_refs:
  - user-improvement-request: 2026-03-19
code_refs:
  - app/src/main/java/com/rachitgoyal/chesshelper/ui/app/ChessHelperApp.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayControls.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowService.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/settings/SettingsScreen.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/settings/AppSettings.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessRules.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStore.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameSnapshot.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/MoveRecord.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/ChessPositionFenEncoder.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/engine/model/EngineRecommendation.kt
last_updated: 2026-03-19
---

# Brief — App Improvements

## Goal

Enumerate concrete, actionable improvements across all dimensions of ChessHelper—new features, UI/UX
polish, performance/quality optimisations, and customisation options—then prioritise them and produce
an implementation-ready plan.

## Current-state summary

| Area | What exists today |
|------|-------------------|
| **Overlay host** | `OverlayWindowService` + `OverlayWindowHost`; `TYPE_APPLICATION_OVERLAY`; drag-to-reposition with peek-strip guard |
| **Chess domain** | Full legal-move gen, en passant, castling, promotion; check/checkmate/stalemate detection in `ChessRules`/`ChessGameStore` |
| **Engine** | Persistent Stockfish UCI session (`movetime 5000`, `Hash 128`, ≤4 threads); `arm64-v8a` + `x86_64` PIE binaries; engine-error surfaced in overlay |
| **Overlay UI** | `OverlayPanel` → `ChessBoard` + `OverlayControls`; recommendation banner + move arrow; undo/reset; side selector; collapse/expand; drag |
| **Settings** | `AppSettings` (DataStore): `autoApplyBestMove`, `autoAnalyzeOnOpponentMove`, `autoMinimizeAfterMove` |
| **Main screen** | `ChessHelperApp`: launch/stop button + settings nav link; no other screens |
| **Pieces** | Text-emoji only |
| **Move history** | Tracked internally in `ChessGameStore` / `MoveRecord` but not surfaced to the user |

### Known gaps (from the request)

- No move-history / notation panel visible to the user
- No FEN/PGN import or export
- No opening book / opening-name display
- No evaluation bar
- No multi-line analysis
- No position setup / custom starting position
- No time controls / clock simulation
- No puzzle mode
- No theme / colour customisation for board squares or pieces
- Piece rendering is text-emoji only (no SVG/vector piece set)
- No haptic feedback on moves
- No sound effects on moves/captures
- No accessibility labels on board squares beyond basic semantics
- No portrait/landscape adaptive layout for the overlay
- No "copy FEN to clipboard" / share functionality
- No onboarding flow for new users
- No deep-link support to launch from other chess apps
- Main screen is very minimal — just launch/stop/settings

---

## Improvement areas and rough priority

### Area 1 — Move-history panel  *(P0 — high user value, domain data already exists)*
`ChessGameStore`/`MoveRecord` already track every move; nothing is surfaced in the overlay.
Displaying algebraic notation (SAN) in a scrollable panel would immediately add tangible value.

**Acceptance bullets:**
- [ ] A collapsible move-history section appears inside `OverlayPanel` (below the board or as a swipe-up
  sheet) when the overlay is expanded.
- [ ] Each move entry shows the move number, white move, and black move in standard algebraic notation
  (e.g., `1. e4  e5`).
- [ ] The panel auto-scrolls to the latest move after each `onSquareTapped` or `onApplyRecommendationClicked`.
- [ ] The history section is hidden in `MINIMIZED` / `COLLAPSED` panel mode to save screen space.
- [ ] No new domain model is introduced — `MoveRecord` list from `ChessGameStore` is consumed directly.

### Area 2 — FEN copy-to-clipboard  *(P0 — zero new dependencies, high utility)*
`ChessPositionFenEncoder` already exists. A single "Copy FEN" action exposes the current position to
any external tool (Lichess analysis board, engine GUIs, etc.).

**Acceptance bullets:**
- [ ] A "Copy FEN" icon/button appears in `OverlayControls` when the overlay is expanded.
- [ ] Tapping it writes the current position FEN (from `ChessPositionFenEncoder`) to the system clipboard.
- [ ] A brief `Toast` / `Snackbar` confirms the copy.
- [ ] No internet permission required; clipboard write only.

### Area 3 — Haptic feedback on moves  *(P0 — one-line implementation, clear UX lift)*
Android `HapticFeedbackConstants` is available via `View.performHapticFeedback()` or Compose's
`LocalHapticFeedback`. A short tick on every legal move and a stronger buzz on capture/check noticeably
improves the tactile feel of the overlay.

**Acceptance bullets:**
- [ ] A subtle `VIRTUAL_KEY` (or equivalent) haptic fires on every legal square tap that results in a move.
- [ ] A stronger `LONG_PRESS` or `CONFIRM` haptic fires when a capture or check move is committed.
- [ ] Haptics are guarded by a new `enableHapticFeedback: Boolean` setting (default `true`) in
  `AppSettings` and `OverlayBoardUiState`.
- [ ] No haptic fires for illegal tap attempts or deselections.

### Area 4 — Evaluation bar  *(P1 — requires Stockfish `info score` parsing)*
Stockfish already emits `info depth … score cp …` lines during analysis. Capturing and displaying a
simple centipawn/mate bar alongside the board gives users a continuous positional sense without extra
engine queries.

**Acceptance bullets:**
- [ ] `StockfishMoveRecommendationEngine` captures the last `info score cp <n>` or `info score mate <m>`
  line during `go movetime` analysis in addition to the `bestmove` line.
- [ ] The captured score is surfaced via `EngineRecommendation` (new optional `evalCp: Int?` /
  `evalMate: Int?` fields).
- [ ] `OverlayBoardUiState` exposes the latest evaluation as `lastEvalCp: Int?` / `lastEvalMate: Int?`.
- [ ] `OverlayPanel` renders a compact vertical bar (white = positive, black = negative) when an
  evaluation is present; the bar updates after each recommendation.
- [ ] Existing unit tests for `StockfishMoveRecommendationEngine` still pass; a new test covers
  `info score` parsing.

### Area 5 — Opening name display  *(P1 — bundled JSON/lookup, no network required)*
For positions still in the opening (≤ ~15 moves, or until the engine reports out-of-book), display the
ECO code and opening name (e.g., `B20 · Sicilian Defense`). A compact bundled JSON (~200 KB) can map
FEN prefixes or move sequences to ECO entries; no network call is needed.

**Acceptance bullets:**
- [ ] A bundled `assets/openings.json` (or equivalent) maps FEN prefixes / move sequences to ECO code +
  name strings.
- [ ] After each move, `OverlayBoardViewModel` performs a lightweight lookup and updates
  `OverlayBoardUiState.currentOpening: String?`.
- [ ] The opening name appears as a single text line in the overlay header area when non-null.
- [ ] The opening field clears (becomes null) once the position leaves the known opening tree.
- [ ] Lookup runs off the main thread (coroutine `Dispatchers.Default`).

### Area 6 — Vector / SVG piece set  *(P1 — visual quality, no new logic)*
Replace the text-emoji piece rendering in `ChessBoard.kt` with vector drawables or Compose
`ImageVector` assets. A single well-licensed SVG set (e.g., Wikimedia chess pieces, Apache 2.0) shipped
as Android vector drawables removes platform-font rendering inconsistencies.

**Acceptance bullets:**
- [ ] 12 vector drawable XML files (one per piece/side combination) are added under
  `app/src/main/res/drawable/`.
- [ ] `ChessBoard.kt` renders pieces via `Image(painter = painterResource(…))` instead of `Text(emoji)`.
- [ ] Rendering at the smallest overlay size (≈40 dp per square) is crisp (no pixelation or clipping).
- [ ] The emoji fallback is removed; no `BUILD_VERSION` guards are needed.

### Area 7 — Multi-line analysis panel  *(P1 — extends existing Stockfish session)*
Stockfish supports `MultiPV N` UCI option. Requesting 3 principal variations and surfacing them as
ranked candidate moves in the overlay helps intermediate players understand alternatives.

**Acceptance bullets:**
- [ ] A new `multiPvCount: Int` setting (default `1`, options `1 / 2 / 3`) is added to `AppSettings` and
  `SettingsScreen`.
- [ ] When `multiPvCount > 1`, `StockfishMoveRecommendationEngine` sets `setoption name MultiPV value N`
  before `go movetime`.
- [ ] `EngineRecommendation` is extended with `pvLines: List<PvLine>` where `PvLine` carries rank, move,
  and eval.
- [ ] `OverlayPanel` shows the ranked list under the recommendation banner; each line is tappable to
  preview that move's arrow.
- [ ] When `multiPvCount == 1` the UI is unchanged from today.

### Area 8 — Onboarding flow  *(P1 — retention/discoverability)*
New users face a blank `ChessHelperApp` screen with no guidance on what the app does or how to grant
the overlay permission. A single-screen (or bottom-sheet) onboarding step dramatically reduces drop-off.

**Acceptance bullets:**
- [ ] On first launch (detected via a DataStore flag), a bottom-sheet or full-screen onboarding card
  explains: (a) what the overlay does, (b) how to grant `ACTION_MANAGE_OVERLAY_PERMISSION`, and
  (c) how to return to the app after granting it.
- [ ] A "Got it / Grant permission" button deep-links to `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
- [ ] Once dismissed or permission granted, the flag is set and onboarding never shows again.
- [ ] `ChessHelperApp` (main screen) is the sole integration point; no new navigation graph is required.

### Area 9 — Accessibility labels on board squares  *(P1 — correctness/compliance)*
`ChessBoard.kt` squares and pieces have no `contentDescription`. Screen readers cannot convey the board
state.

**Acceptance bullets:**
- [ ] Every square in `ChessBoard` carries a `Modifier.semantics { contentDescription = … }` of the form
  `"e4 — White Pawn"` (or `"e4 — empty"` for empty squares).
- [ ] The recommended-move arrow canvas layer has a `contentDescription` describing the move
  (e.g., `"Recommended move: e2 to e4"`).
- [ ] Selected and legal-target highlight squares include `"selected"` / `"legal target"` in their
  description.
- [ ] No new runtime dependencies are added.

### Area 10 — Portrait/landscape adaptive overlay layout  *(P2 — polish)*
The overlay is laid out for portrait only. On landscape orientation the board may clip or overflow.
Adapting the layout based on `WindowMetrics` or `LocalConfiguration` lets the overlay remain usable
in either orientation.

**Acceptance bullets:**
- [ ] In landscape mode the overlay shrinks the board to fit the shorter screen dimension and repositions
  controls to a side column.
- [ ] `OverlayWindowPositioning.kt` clamps the overlay position to the new screen bounds on orientation
  change.
- [ ] Existing drag / peek-strip behaviour is preserved in both orientations.

### Area 11 — FEN/PGN import  *(P2 — power-user feature)*
Allow users to paste a FEN string or PGN to load an arbitrary position into the overlay board.

**Acceptance bullets:**
- [ ] A "Paste FEN" field appears in the overlay when expanded (or via a dedicated icon in controls).
- [ ] Pasting a valid FEN replaces the current `ChessGameStore` position via a new `loadFen(fen: String)`
  method.
- [ ] Invalid FEN strings show an error message; the board is not changed.
- [ ] (Stretch) PGN paste replays moves up to the final position.

### Area 12 — Theme / colour customisation  *(P2 — personalisation)*
Add a light/dark board colour theme selector (e.g., classic brown, blue, green) to `SettingsScreen`.

**Acceptance bullets:**
- [ ] `AppSettings` exposes `boardTheme: BoardTheme` (sealed class: `CLASSIC`, `BLUE`, `GREEN`).
- [ ] `ChessBoard.kt` reads `boardTheme` from the composed UI state and applies the appropriate light/dark
  square colours.
- [ ] The theme applies immediately on settings change without requiring overlay restart.

### Area 13 — Sound effects  *(P2 — sensory polish)*
A short WAV/OGG sample for move, capture, and check events, played via `SoundPool`.

**Acceptance bullets:**
- [ ] `SoundPool` is initialised lazily in a `ViewModel`-scoped object and released on `onCleared`.
- [ ] Three samples bundled in `assets/sounds/`: `move.ogg`, `capture.ogg`, `check.ogg`.
- [ ] A `enableSoundEffects: Boolean` setting (default `false` — opt-in) controls playback.
- [ ] No `INTERNET` or `RECORD_AUDIO` permissions are required.

### Area 14 — Richer main screen  *(P2 — polish)*
`ChessHelperApp` is a single button. Adding a brief description, an app icon treatment, and a
permission-status indicator makes the app feel less bare.

**Acceptance bullets:**
- [ ] The main screen displays: app name + icon, one-sentence description, overlay-permission status
  chip ("Granted" / "Not granted"), and the existing launch/stop + settings controls.
- [ ] The permission chip taps through to `ACTION_MANAGE_OVERLAY_PERMISSION` when not granted.
- [ ] No new navigation graph is required.

---

## Priority summary

| Tier | ID | Improvement | Rationale |
|------|----|-------------|-----------|
| P0 | A1 | Move-history panel | Domain data exists; instant user value |
| P0 | A2 | FEN copy-to-clipboard | Zero dependencies; high utility |
| P0 | A3 | Haptic feedback | One-line impl; clear UX lift |
| P1 | A4 | Evaluation bar | Moderate Stockfish work; high chess value |
| P1 | A5 | Opening name display | Bundled JSON lookup; zero network |
| P1 | A6 | Vector / SVG piece set | Visual quality; no logic change |
| P1 | A7 | Multi-line analysis | Extends existing Stockfish session |
| P1 | A8 | Onboarding flow | Retention / discoverability |
| P1 | A9 | Accessibility labels | Correctness / compliance |
| P2 | A10 | Portrait/landscape adaptive layout | Polish |
| P2 | A11 | FEN/PGN import | Power-user feature |
| P2 | A12 | Theme / colour customisation | Personalisation |
| P2 | A13 | Sound effects | Sensory polish (opt-in) |
| P2 | A14 | Richer main screen | Polish |

---

## Open questions

1. **Move notation format** — Should SAN (standard algebraic) be computed in the domain layer via a new
   `ChessRules.toSan(move, snapshot)` helper, or is a simpler long-algebraic format (`e2e4`) acceptable
   for the initial history panel?
2. **Opening book data source** — Should the bundled opening JSON be derived from a permissively-licensed
   ECO database (e.g., the public-domain Lichess `chess-openings` repo), or is a hand-curated subset
   of the most common 50–100 openings sufficient?
3. **Evaluation bar orientation** — Should the bar appear vertically along the side of the board, or as a
   thin horizontal strip above/below the board, given the constrained overlay height?
4. **Multi-PV default** — Is `multiPvCount = 1` (unchanged today) the right default, or should `3` be the
   default so users immediately see alternatives?
5. **Piece set licensing** — Which specific vector piece set should be used? (Wikimedia chess pieces
   are CC-BY-SA; the `chess.com` set is commercial. Need to confirm licence for bundling.)
6. **Sound effects opt-in vs. opt-out** — Default `false` (opt-in) is proposed for sounds because users
   playing on a device in public may not want unexpected audio. Confirm this default with product.
7. **FEN/PGN import scope** — Should PGN import be in-scope for the initial implementation pass or deferred
   to a follow-up iteration?

