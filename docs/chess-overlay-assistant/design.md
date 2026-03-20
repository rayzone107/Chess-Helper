---
feature: chess-overlay-assistant
stage: design
status: drafted
input_refs:
  - docs/chess-overlay-assistant/brief.md
  - docs/chess-overlay-assistant/plan.md
  - docs/chess-overlay-assistant/status.md
code_refs:
  - app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt
  - app/src/main/AndroidManifest.xml
  - app/build.gradle.kts
last_updated: 2026-03-19
---

# Chess Overlay Assistant MVP Design

## Scope lock
- This update targets the existing Compose overlay panel already reused by the app surface and the overlay host.
- No host-lifecycle or permission-flow changes are part of this UI delta; keep this slice focused on overlay chrome, recommendation feedback, and board preview behavior.
- Keep the current local chess state/store and local engine boundary; remote analysis remains out of scope.

## Implementation entry points
- `MainActivity` -> `ChessHelperApp()`
- `feature/overlay/OverlayBoardRoute()`
- `feature/overlay/OverlayBoardViewModel`
- `feature/overlay/OverlayBoardUiState`
- `feature/overlay/components/OverlayPanel`
- `feature/overlay/components/ChessBoard`
- `feature/overlay/components/OverlayControls`
- `domain/chess/ChessGameStore`
- `engine/MoveRecommendationEngine`

## Design update v2026-03-18.2
Plan `v2026-03-18.2` is implementation-ready for the current Compose overlay stack. Apply this delta in:

- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
  - `OverlayWindowCard(...)`
  - `RecommendationBanner(...)`
  - `minimizedStatus(...)`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayControls.kt`
  - `OverlayControls(...)`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`
  - `ChessBoard(...)`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`
  - `activeRecommendedMove`
  - `canRecommend`
  - `boardBottomSide`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
  - `onRecommendClicked()`
  - `onSquareTapped()`
  - `onApplyRecommendationClicked()`
  - `onUndoClicked()`

### Overlay chrome contract

#### Expanded
- Restore the dark overlay treatment on the outer card/header chrome; keep the board squares unchanged.
- Remove standalone title/body helper copy. The header should carry signals only.
- Header structure:
  1. left drag region
  2. compact signal cluster: `Color to move`, `You: White|Black`
  3. top-right icon controls: minimize, then close
- `RecommendationBanner(...)` becomes a compact status strip, not a paragraph. One short line max:
  - `Analyzing…`
  - `Best: <notation>`
  - `Best: <notation> • stale`
  - `No move`
  - `Game over`

#### Minimized
- Use the same dark chrome.
- Entire chip remains draggable.
- Content stays signal-only: `Color to move` plus the current compact recommendation status.
- Replace text actions with icon-style expand and close controls in the top-right cluster.

### Drag and window controls
- Use icon-only affordances with minimum 40dp hit targets and explicit semantics:
  - `Expand overlay`
  - `Minimize overlay`
  - `Close overlay`
- In expanded mode, only the non-button header area starts drag; the icon controls must not act as drag handles.
- In minimized mode, the chip body remains draggable while icon controls still take priority on tap.
- Preserve the current `panelOffsetPx` behavior across expand/minimize; no snap rules change.

### Compact-copy rules
- Remove the current explanatory sentences in `OverlayWindowCard(...)` and `OverlayControls(...)`.
- Keep only short labels/signals needed to act:
  - `Color to move`
  - `You`
  - `Show best move`
  - `Undo`
  - `Play suggested move`
  - `New game`
- Do not reintroduce sentence-length coaching copy elsewhere in the panel.

### Best-move CTA loading contract
- Keep the CTA in `OverlayControls(...)` at its current position and width.
- Loading state is inline inside the same button: spinner + `Analyzing…`.
- While loading:
  - disable the CTA
  - reject duplicate taps
  - keep undo / side switching available unless implementation discovers a state-consistency issue
- `OverlayBoardViewModel.onRecommendClicked()` must expose at least one frame of `RecommendationState.LOADING`; do the engine call off the main thread or make the engine boundary async.
- If the board state changes before the result returns, discard the outdated result and clear loading instead of showing an arrow for the wrong position.

### Recommended-move arrow contract
- Replace the current recommendation square tinting with one arrow from source square center to target square center.
- Keep existing selection, legal-target, and last-move square feedback.
- Arrow visibility rules:
  - show only for `activeRecommendedMove`
  - hide during `LOADING`
  - hide when stale, cleared, undone, or applied
- Layering:
  1. base square colors
  2. selected / legal-target / last-move square feedback
  3. recommendation arrow
  4. piece glyphs and board coordinates
- Arrow placement must use displayed board coordinates, not raw algebraic ordering. `ChessBoard(...)` should derive display row/column from `bottomSide` before computing centers so the arrow stays correct when the board is flipped for Black.
- The destination arrow head points toward the destination square in both orientations; source/target math must come from the same orientation helper that drives square rendering.

### Selection feedback and last-move highlighting
- Keep the existing selected-square and legal-target feedback unchanged.
- Keep `lastMove` highlighting on both source and destination after any played move, including `Play suggested move`.
- If a selected square or legal target overlaps the recommended move, the square highlight still wins on the square itself; the arrow remains an additional overlay, not a replacement for those states.

## Primary UI states

### 1. Overlay panel state
Represent in `OverlayBoardUiState`.

- `Expanded`
  - Shows header, board, recommendation row, and control row.
  - Default launch state.
- `Minimized`
  - Shows compact header chip only.
  - Header still shows turn + latest recommendation status summary.
- `Dragging`
  - Transient interaction state layered on either `Expanded` or `Minimized`.
  - Holds current `IntOffset` being dragged.

Required fields:
- `panelMode: Expanded | Minimized`
- `panelOffsetPx: IntOffset`
- `isDragging: Boolean`
- `windowBoundsPx: IntSize` for clamping

State rules:
- Toggling minimize/expand preserves the last free-position offset.
- Clamp the panel fully on-screen after drag end.
- No snap zones in MVP; preserve exact last position after release.

### 2. Board interaction state
Represent in `ChessGameStore` / `OverlayBoardUiState`.

Required fields:
- `board: GameSnapshot`
- `selectedSquare: SquareId?`
- `legalTargets: Set<SquareId>`
- `lastMove: MoveRecord?`
- `sideToMove: Side`
- `moveHistory: List<MoveRecord>`

State rules:
- `selectedSquare == null` => no legal-target highlight.
- `selectedSquare != null` => show only legal targets for that piece in the current position.
- After a legal move, clear selection, clear legal targets, and update `lastMove`.
- Undo recomputes `selectedSquare = null`, `legalTargets = emptySet()`, `lastMove = previous move or null`.

### 3. Recommendation state
Represent in `OverlayBoardUiState`.

Required fields:
- `recommendationState: Idle | Loading | Ready | Error`
- `recommendation: EngineRecommendation?`
- `assistedSide: Side` (hardcode `White` for MVP if no side picker is added yet)
- `isRecommendationStale: Boolean`

State rules:
- Any new board move invalidates a previously shown recommendation and marks it stale.
- `Loading` disables further recommend requests until completion/cancel.
- `Ready` stores move text plus optional score/depth if the engine exposes them.
- `Error` is recoverable and does not block move entry or undo.

## Overlay panel behavior

### Expanded state
Structure in `OverlayPanel`:
1. **Drag handle / header row**
   - drag affordance area
   - turn indicator
   - minimize button
2. **Board region**
   - fixed 8x8 board
   - square taps only; no piece drag in MVP
3. **Recommendation row**
   - status text or best move text
4. **Control row**
   - `Recommend`
   - `Undo`

Interaction rules:
- Only the header row starts panel drag while expanded.
- Board taps must not move the panel.
- Panel elevation stays constant during drag; do not animate scale in MVP.

### Minimized state
Structure:
- Single compact chip/card with:
  - expand button
  - turn indicator
  - recommendation summary (`Ready`, `Loading`, `Error`, or blank)

Interaction rules:
- Entire minimized chip is draggable.
- Tapping the explicit expand affordance expands.
- Tapping the body does nothing in MVP; avoid ambiguous tap-vs-drag behavior.

## Board interactions
Implement in `ChessBoard` with actions routed to `OverlayBoardViewModel`.

Tap rules:
1. Tap own-side piece on turn -> select it.
2. Tap the selected square again -> clear selection.
3. Tap another own-side piece -> switch selection to that piece.
4. Tap a highlighted legal target -> apply move.
5. Tap a non-legal empty square or opponent piece -> clear selection.

Move-entry rules:
- Single-tap source, single-tap destination only.
- Enforce legal moves through the domain store/library wrapper.
- Reject illegal move attempts silently except for selection clear; no toast/snackbar for MVP.
- Board orientation follows `playerSide`: White at bottom for White, Black at bottom for Black.
- Promotion UI is deferred; if the rules library requires a promotion piece, default to queen for MVP and document it in code comments.

## Highlight rules

### Selected-piece feedback
- Selected source square gets the highest-priority highlight.
- Use a clearly visible outline/ring plus a light fill tint.
- Persist until move completion, selection switch, undo, or explicit deselect.

### Legal-target feedback
- Show on all legal destination squares for the currently selected piece.
- Empty target: centered dot.
- Capture target: border ring.
- Legal-target highlight priority is lower than selected source, higher than last-move tint.

### Last-move feedback
- Highlight both source and destination of `lastMove`.
- Use a lower-emphasis background tint than the selection highlight.
- Persist until the next legal move or until history becomes empty after undo.

### Highlight precedence
Apply one visual stack only:
1. selected square
2. legal target
3. last-move source/destination
4. base light/dark square

Do not blend highlight types in MVP; higher-priority state fully wins.

## Recommendation and undo controls

### `Recommend`
Location: `OverlayControls` and mirrored summary in the recommendation row.

Enable when:
- `panelMode == Expanded`
- `recommendationState != Loading`
- `sideToMove == assistedSide`
- there is at least one prior move in history, or `assistedSide == White` at move 1

Disable when:
- game is over/stalemated/checkmated in the domain snapshot
- it is not the assisted side's turn
- engine request already running

Behavior:
- Tap -> set `Loading`, invoke `MoveRecommendationEngine`, then update to `Ready` or `Error`.
- On success, show the compact best-move signal and the board arrow preview.
- On any subsequent board move after a ready result, keep the compact text visible but mark it stale until replaced or cleared.

### `Undo`
Location: `OverlayControls`.

Enable when:
- `moveHistory.isNotEmpty()`

Behavior:
- Removes exactly one ply per tap.
- Cancels current selection and recommendation loading state.
- Clears recommendation if it no longer matches the current position.
- Recomputes `lastMove` from remaining history.

## Feedback and transitions
- Expand/minimize: simple `AnimatedVisibility` or size transition; keep duration short and non-bouncy.
- Drag: immediate position updates with no spring during pointer movement.
- Recommend loading: inline spinner in the button and compact `Analyzing…` status strip text.
- Error: one-line compact status strip message; no modal.
- Empty recommendation state: no coaching paragraph; show only compact turn/recommendation signals.

## Deferred from MVP
- Piece drag-and-drop, premoves, multiple arrows, engine lines, or move list panel
- Saved panel position across process death
- Engine strength/depth settings
- Custom promotion picker
- Animation-heavy move playback
- Cloud analysis or account sync

## Next implementation slice
1. Update `OverlayPanel.kt` to restore the dark chrome, signal-only header, compact status strip, and icon window controls.
2. Update `OverlayControls.kt` so `Show best move` owns the inline loading state and descriptive copy is removed.
3. Update `ChessBoard.kt` to draw one recommendation arrow in display-space coordinates while preserving existing selection/legal-target/last-move feedback.
4. Update `OverlayBoardViewModel.kt` / `OverlayBoardUiState.kt` so async recommendation loading, stale handling, and arrow visibility match the compact UI contract.



## Design update v2026-03-19.1

### Scope lock
- This delta does **not** redesign the board or move-entry UX.
- Keep the existing compact overlay chrome, current best-move arrow/apply flow, and current legality boundary.
- Change only the engine-lifecycle and recommendation-status contract needed to support stronger Stockfish output with explicit error surfacing.

### Persistent engine session contract
- `StockfishMoveRecommendationEngine` owns one reusable UCI process for its lifetime.
- Session lifecycle:
  1. install binary if needed
  2. start process once
  3. perform `uci` / option setup / `isready` once
  4. reuse the process for later `position fen ...` + `go ...` requests
  5. close the process when the owning `OverlayBoardViewModel` is cleared or the overlay host is torn down
- The session must be **single-flight/synchronized** so overlapping requests cannot interleave UCI commands on the same process.
- Any handshake, timeout, parse, or process-death failure invalidates the session and surfaces an explicit error back to the view-model.

### Stronger analysis defaults
- Favor stronger output over speed for this pass.
- Minimum design intent for defaults:
  - longer analysis time than the current short search
  - larger hash size than the current small default
  - explicit analysis-oriented UCI options where supported
- Keep exactly one PV/result surfaced in the UI; no multi-line analysis panel is added.

### Recommendation status contract update
- Keep the existing top-level states in `OverlayBoardUiState`, but distinguish two user-facing non-success outcomes:
  - **No move**: there is genuinely no legal recommendation to show for the requested position
  - **Engine failure**: Stockfish could not start/respond/return a valid move
- The UI must not reuse `No move` text for engine failures.

### Compact overlay copy rules
- Minimized/compact status may use a short label such as:
  - `Analyzing…`
  - `Best: <notation>`
  - `Best: <notation> • stale`
  - `Engine error`
  - `No move`
  - `Check`, `Checkmate`, `Stalemate` variants as already defined
- Expanded banner/detail treatment:
  - if ready: may continue showing compact status or recommendation summary
  - if engine failure: show a short explicit failure message derived from the surfaced engine error, not a heuristic fallback summary

### Overlay behavior rules for engine failure
- A failed recommendation request must:
  - clear loading
  - leave move entry, undo, and side switching available
  - clear any active recommendation preview/apply CTA
  - preserve current board state
- A later recommend tap may retry the engine and can recreate the persistent session if the prior one was discarded.

### Implementation entry points for this delta
- `engine/stockfish/StockfishMoveRecommendationEngine.kt`
- `feature/overlay/OverlayBoardViewModel.kt`
- `feature/overlay/OverlayBoardUiState.kt`
- `feature/overlay/components/OverlayPanel.kt`
- `feature/overlay/components/OverlayControls.kt`
