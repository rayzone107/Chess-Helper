# Review Inputs

- id: review-chess-overlay-001
  status: open
  severity: high
  problem: >-
    The MVP verification is incomplete for the overlay state layer. `plan.md` Task 6 calls for
    reducer + overlay behavior coverage, including recommend gating, highlight/recommendation state
    transitions, and overlay behavior. The current test set only covers `ChessGameStore` and the
    heuristic engine; there is no `OverlayBoardViewModel` test coverage, and the expected
    `OverlayPanelUiTest` is also absent. That leaves the documented MVP behavior for panel drag /
    minimize state, recommendation staleness and clearing on move or undo, and assisted-side
    gating unverified.
  required_change: >-
    Add focused tests for `OverlayBoardViewModel` that exercise at least: recommend enable/disable
    gating after opponent moves, stale recommendation handling after a new move, undo/reset
    clearing behavior, and panel drag/clamp + minimize/expand state transitions. If the team still
    intends to satisfy Task 6 fully, also add the planned overlay UI/instrumentation test or update
    the plan/status docs to explicitly narrow the test scope.
  file_refs:
    - /Users/rachit/AndroidStudioProjects/ChessHelper/app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt
    - /Users/rachit/AndroidStudioProjects/ChessHelper/app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt (missing)
    - /Users/rachit/AndroidStudioProjects/ChessHelper/app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt (missing)
    - /Users/rachit/AndroidStudioProjects/ChessHelper/docs/chess-overlay-assistant/plan.md

- id: review-chess-overlay-002
  status: open
  severity: medium
  problem: >-
    The chess rules implementation materially diverges from the documented Task 3 plan. The plan
    explicitly recommends a chess-library wrapper and says not to hand-roll full legality unless
    that dependency choice fails and is resolved. Instead, `ChessRules.kt` now contains a custom
    full-rules implementation with only narrow regression coverage. This increases correctness risk
    for exactly the area the plan tried to de-risk, and the implementation/docs no longer match.
  required_change: >-
    Either refactor the domain layer to use a wrapped chess rules library as planned, or document a
    deliberate deviation in the feature docs/status and back the custom rules engine with broader
    regression tests for castling, check/checkmate or stalemate handling, promotion, and en passant
    execution—not just target generation.
  file_refs:
    - /Users/rachit/AndroidStudioProjects/ChessHelper/app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessRules.kt
    - /Users/rachit/AndroidStudioProjects/ChessHelper/app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStore.kt
    - /Users/rachit/AndroidStudioProjects/ChessHelper/docs/chess-overlay-assistant/plan.md
    - /Users/rachit/AndroidStudioProjects/ChessHelper/docs/chess-overlay-assistant/status.md

