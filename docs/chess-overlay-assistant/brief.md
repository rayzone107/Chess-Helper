---
feature: chess-overlay-assistant
stage: discovery
status: enhancement-scoped
input_refs:
  - user-request: 2026-03-17
  - user-bug-fix-request: 2026-03-18
  - user-enhancement-request: 2026-03-18
  - user-enhancement-request: 2026-03-18-check-legality
code_refs:
  - app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessRules.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStore.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameSnapshot.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveTranslator.kt
last_updated: 2026-03-19
---
## Goal
- Build an Android overlay-style chess helper for studying online chess.
- Show a full chess board with pieces and allow move entry for both sides.
- After the opponent move is entered, provide a best-move recommendation on demand.
- Support clear selection state, last-move highlight, undo, expand/collapse, and drag/move behavior.

## Current bug fix
- Repro: the current chess window floats only inside the app activity bounds, so leaving the app hides it instead of keeping it over other apps.
- Expected: after overlay permission is granted, the chess board should render from a real `TYPE_APPLICATION_OVERLAY` host and stay visible above other apps until the user closes it.
- Repro: the expanded overlay header is hard to read (`Floating coach board` appears dark on dark and the header copy is squished), white pieces are too hollow/faint, `Study side` is unclear, the board does not flip for Black, White cannot request the first move recommendation immediately, and the recommended move is text-only with no easy way to visualize or play it.
- Expected: the overlay header should be readable and better laid out, white pieces should be clearly visible, the side selector should read like “you are playing as” and flip the board perspective, White should be able to request a move immediately on move 1, and recommendations should be previewed on-board with a button to apply them directly.
- Enhancement: keep the dark overlay chrome, collapse the header and status copy to compact signals, replace top-right text actions with icon-style controls, show inline loading inside `Show best move`, and draw an actual arrow for the recommended move.
- Enhancement: allow the real cross-app overlay to be dragged mostly off-screen while always keeping a small visible strip so it can be recovered easily.
- Engine integration: package the user-provided `stockfish/stockfish-android-armv8` binary into the app, extract it to app-private storage at runtime, speak UCI to it for recommendations, and fall back to the local heuristic engine if Stockfish setup fails.

## Request update — 2026-03-18 check/checkmate + legality
- Route choice: **enhancement of the existing `chess-overlay-assistant` feature**, with a planning-first pass only. This is not a new slug and does not require a new engine architecture.
- Current repo behavior already routes move entry through `ChessRules.legalMovesFrom(...)` and `ChessGameStore.applyMove(...)`, so the lightest complete path is to expose and verify that legality/check state rather than moving authority into Stockfish.

### Acceptance
- Clearly show when the side to move is **in check** and when the game is **checkmate** during play.
- Keep illegal move attempts blocked when the player is in check, and also block moves that would leave or put that player in check.
- Keep the **domain chess rules layer** as the move-authority source of truth; **Stockfish remains recommendation-only** and its suggested move must still be validated against the current legal move list.
- Differentiate **checkmate** from other terminal states so the UI does not label every no-legal-moves position the same way.
- Add targeted regression coverage for check detection, self-check filtering, checkmate/stalemate classification, and overlay state propagation.

### Repo refs to inspect/change
- `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessRules.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStore.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameSnapshot.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveTranslator.kt`
- `app/src/test/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStoreTest.kt`
- `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`

## Open questions
- Should the final overlay always launch immediately after permission grant, or should the app keep a manual launch/stop control center after grant?
- Should move analysis use a local engine such as Stockfish or an external service?
- What is the minimal board/state architecture needed before engine integration?
- For the new gameplay-status slice, is a compact text/banner plus checked-king highlight sufficient, or does the product want a stronger board-level treatment for checkmate as well?
- Should the UI explicitly label **stalemate** now that checkmate visibility is being added, or is `Game over` still acceptable for non-check terminal states?

## Request update — 2026-03-19 stronger persistent Stockfish output
- Route choice: **enhancement of the existing `chess-overlay-assistant` feature** with a docs-first pass, then the smallest complete engine-quality upgrade. This is not a new feature slug and not a broad architecture rewrite.
- Current repo behavior already bundles Stockfish and routes recommendations through `StockfishMoveRecommendationEngine`, but the implementation still spins up a fresh process for each request, keeps a short search window, and silently falls back to `LocalHeuristicMoveRecommendationEngine` on engine failures.
- The lightest complete fix is therefore to keep the existing Stockfish asset/install path, replace per-request process startup with a persistent reusable UCI session, raise analysis strength defaults, and surface engine failures in the overlay instead of returning weaker heuristic moves.

### Acceptance
- Reuse **one persistent Stockfish UCI process per overlay/view-model engine instance** across recommendation requests instead of spawning a new process every time.
- Optimize for **stronger recommendations even if analysis takes longer**, using clearly stronger default Stockfish search settings than the current short per-request analysis.
- Remove the **heuristic fallback path** from production recommendation flow so Stockfish startup/handshake/search/parse failures are surfaced to the user rather than replaced with lower-quality moves.
- Surface recommendation engine state clearly in the overlay:
  - loading remains visible while analysis runs
  - successful recommendations still show the best move preview/apply flow
  - engine failures show an explicit engine-error status/message instead of generic `No move`
- Keep Stockfish recommendation output **legal-move validated** through the existing domain/translator path before the app previews or applies a move.
- Add targeted regression coverage for the new failure surfacing and any state-flow changes introduced by persistent engine reuse.

### Repo refs to inspect/change
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/MoveRecommendationEngine.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishAssetInstaller.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveTranslator.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelFactory.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayControls.kt`
- `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`

### Open questions
- Lock during implementation: is `movetime`-based stronger analysis sufficient for this pass, or does the current repo need a depth-based search contract as well?
- Should the compact minimized status show a short engine-failure label only, while the expanded banner shows the fuller failure message?

