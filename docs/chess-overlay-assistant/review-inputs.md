---
feature: chess-overlay-assistant
stage: review
status: approved
input_refs:
  - docs/chess-overlay-assistant/plan.md
  - docs/chess-overlay-assistant/design.md
  - docs/chess-overlay-assistant/status.md
code_refs:
  - app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt
  - app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessRules.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/engine/MoveRecommendationEngine.kt
last_updated: 2026-03-18
---
- id: review-1
  status: resolved
  severity: medium
  problem: The new ViewModel tests covered recommendation gating/staleness/undo, but the overlay UI test needed to verify the full minimize-expand round trip.
  required_change: Extended `OverlayPanelUiTest` to tap `Expand` after minimizing and assert the expanded `Minimize` affordance returns.
  file_refs:
    - app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt
- id: review-2
  status: resolved
  severity: medium
  problem: The implementation used an in-repo rules engine instead of the originally suggested dependency wrapper without documenting the deviation.
  required_change: Documented the local rules/heuristic-engine choice as the MVP resolution while keeping `MoveRecommendationEngine` as the swap boundary for a future Stockfish integration.
  file_refs:
    - docs/chess-overlay-assistant/plan.md
    - docs/chess-overlay-assistant/status.md
    - app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessRules.kt
    - app/src/main/java/com/rachitgoyal/chesshelper/engine/MoveRecommendationEngine.kt
- id: review-3
  status: resolved
  severity: medium
  problem: `ChessBoard.kt` draws the recommendation arrow after the board cells/pieces/coordinates, so the green line and endpoint circles sit on top of piece glyphs and rank/file labels. That violates the approved layering contract, which requires the arrow underneath the rendered pieces/coordinates, and it risks obscuring the board state instead of acting as a clean preview.
  required_change: Reorder the board rendering so the recommendation arrow is painted above square highlights but below piece glyphs and board coordinates, while continuing to derive source/target centers from the flipped-board orientation helper.
  file_refs:
    - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt
    - docs/chess-overlay-assistant/design.md
- id: review-4
  status: resolved
  severity: medium
  problem: `OverlayPanel.kt` uses 30dp icon buttons with terse semantics (`Expand`, `Minimize`, `Close`), but the current design locked the top-right window controls to icon affordances with explicit accessibility labels and minimum 40dp hit targets. The controls are therefore smaller and less accessible than the approved chrome contract.
  required_change: Increase the icon-control tap targets to at least 40dp and expose the explicit content descriptions from the design contract (`Expand overlay`, `Minimize overlay`, `Close overlay`). Update the UI test expectations to match the final semantics.
  file_refs:
    - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt
    - app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt
    - docs/chess-overlay-assistant/design.md
- id: review-5
  status: resolved
  severity: medium
  problem: The follow-up tests now cover the icon-control semantics, the immediate loading transition, and one stale async path (`playerSide` changes), but regression coverage is still not strong enough for the loading/stale-result contract in Plan v2026-03-18.2. `OverlayBoardViewModelTest.kt` still does not assert duplicate request rejection while `LOADING`, nor does it verify that an in-flight recommendation is discarded when the board position changes before the result returns.
  required_change: Extend `OverlayBoardViewModelTest.kt` with focused assertions that a second `onRecommendClicked()` during `LOADING` does not start another engine request, and that a move entered while a request is in flight clears loading and prevents the stale result from surfacing afterward.
  file_refs:
    - app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt
    - docs/chess-overlay-assistant/plan.md



