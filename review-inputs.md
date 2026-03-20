# Review Inputs

- id: review-chess-overlay-001
  status: resolved
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
  resolution_note: >-
    Addressed across the existing `OverlayPanelUiTest.kt` and the expanded
    `OverlayBoardViewModelTest.kt`. The view-model suite now covers recommendation gating,
    stale-result discard, undo/reset clearing, loading duplicate rejection, and check/checkmate /
    stalemate propagation for the overlay state layer.

- id: review-chess-overlay-002
  status: partially-addressed
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
  resolution_note: >-
    This pass keeps the documented custom-rules deviation and adds targeted regression coverage for
    check, self-check filtering, checkmate, stalemate, and store/view-model propagation. Broader
    bespoke-rules coverage (for example castling/promotion/en-passant execution edge cases) remains
    a follow-up risk and stays open in `docs/chess-overlay-assistant/status.md`.

- id: review-chess-overlay-003
  status: resolved
  severity: medium
  problem: >-
    The new `GameStatus` domain model now exposes `compactLabel`, and the overlay reads those
    strings directly for `Check`, `Checkmate`, and `Stalemate`. That leaks presentation copy into
    the domain layer, which weakens the authority boundary this slice is otherwise careful to
    preserve: `ChessRules` should own chess semantics, while UI text should stay in the overlay/UI
    layer. It also makes the domain enum carry an overlay-specific `Ready` label that is not a
    domain concept.
  required_change: >-
    Keep `GameStatus` semantic-only (for example `isTerminal` is fine), and move user-facing labels
    into the overlay state/view layer or string resources. The UI can still derive the same compact
    status text, but the domain model should not become the source of truth for presentation copy.
  file_refs:
    - /Users/rachit/AndroidStudioProjects/ChessHelper/app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameStatus.kt
    - /Users/rachit/AndroidStudioProjects/ChessHelper/app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt
  resolution_note: >-
    `GameStatus` now stays semantic-only (`isTerminal` only), and the overlay derives the compact
    `Check` / `Checkmate` / `Stalemate` labels in `OverlayBoardUiState` instead of the domain
    layer owning presentation copy.

