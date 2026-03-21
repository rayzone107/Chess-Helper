---
feature: chess-overlay-assistant
stage: planning-complete
status: ready-for-implementation
input_refs:
  - docs/chess-overlay-assistant/brief.md
  - docs/chess-overlay-assistant/plan-v3.md
code_refs:
  - app/src/main/java/com/rachitgoyal/chesshelper/settings/AppSettings.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/settings/SettingsScreen.kt
  - app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt
last_updated: 2026-03-19
---
- active_owner: coder
- current_state: Plan v3 (streamlined workflow) complete and ready for implementation. No code changes yet in this pass.
- next_reader: coder
- next_step: implement T1–T7 per docs/chess-overlay-assistant/plan-v3.md
- current_state: `Plan v2026-03-19.1` is implemented and automated validation is now complete. Production recommendation flow uses a persistent reusable Stockfish session with stronger defaults (`movetime 5000`, `Hash 128`, up to 4 threads), the heuristic fallback path is removed from production wiring, activity/service owners explicitly dispose the recommendation stack, and the overlay distinguishes `Engine error` from `No move` while preserving legal-move validation and the existing preview/apply flow. The packaged Stockfish payload has now also been rebuilt as real Android PIE executables for both `arm64-v8a` and `x86_64`, and the installer selects the right asset per ABI while forcing replacement of previously installed broken binaries.
- touched_docs:
  - `docs/chess-overlay-assistant/brief.md`
  - `docs/chess-overlay-assistant/plan.md`
  - `docs/chess-overlay-assistant/design.md`
  - `docs/chess-overlay-assistant/review-inputs.md`
  - `docs/chess-overlay-assistant/status.md`
- touched_code:
  - `app/src/main/java/com/rachitgoyal/chesshelper/engine/EngineUnavailableException.kt`
  - `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt`
  - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`
  - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
  - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt`
  - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
  - `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`
- validations_run:
  - repo-context inspection of production engine wiring in `OverlayBoardViewModelFactory.kt` and `OverlayWindowHost.kt`
  - edited-file consistency review across `StockfishMoveRecommendationEngine.kt`, `OverlayBoardViewModel.kt`, `OverlayBoardUiState.kt`, `OverlayPanel.kt`, and `OverlayBoardViewModelTest.kt`
  - reviewer loop completed via `docs/chess-overlay-assistant/review-inputs.md` items `review-6` and `review-7`
  - `./gradlew :app:testDebugUnitTest --tests com.rachitgoyal.chesshelper.feature.overlay.OverlayBoardViewModelTest --console=plain`
  - `./gradlew :app:testDebugUnitTest --console=plain`
  - packaged-asset verification confirmed both `app/src/main/assets/stockfish/stockfish-android-armv8.gz` and `app/src/main/assets/stockfish/stockfish-android-x86_64.gz` decompress to Android PIE executables
  - `./gradlew --console=plain --rerun-tasks testDebugUnitTest --tests "com.rachitgoyal.chesshelper.engine.stockfish.StockfishAssetInstallerTest" --tests "com.rachitgoyal.chesshelper.engine.stockfish.StockfishMoveTranslatorTest"`
  - `./gradlew --console=plain --rerun-tasks testDebugUnitTest`
- unresolved_risks:
  - The persistent-session behavior is covered by code review and lifecycle wiring, but there is not yet an automated test seam that proves one process is reused across multiple real Stockfish calls.
  - Device/emulator validation is still pending for real Stockfish reuse across repeated overlay requests and for the explicit on-device engine-failure path.
  - This fix now covers `arm64-v8a` and `x86_64`; if physical support for other ABIs is needed later, additional Stockfish builds must be packaged.
- reviewer_note: >-
    Reviewer approval for tester handoff after the final persistent-Stockfish re-review: no blocking
    issue remains against the scoped plan/design. Production construction paths in
    `OverlayBoardViewModelFactory.kt` and `OverlayWindowHost.kt` both inject
    `StockfishMoveRecommendationEngine` explicitly, `OverlayBoardViewModel` no longer carries a hidden
    heuristic or no-op default, engine failures surface as explicit `Engine error` state/detail instead of
    collapsing into `No move`, and the service-hosted overlay disposes its recommendation executor plus the
    persistent Stockfish session on hide/destroy. Focused and full debug unit-test runs both passed.
- next_reader: tester
- next_step: run the short manual overlay sanity pass for repeated recommendations, engine-failure surfacing, recommendation retry after failure, and overlay close/reopen behavior on device.
- progress_log:
  - owner: orchestrator
    stage: discovery
    touched_docs:
      - `docs/chess-overlay-assistant/brief.md`
      - `docs/chess-overlay-assistant/status.md`
    touched_code: []
    blockers: []
    next_step: planning
  - owner: orchestrator
    stage: planning
    touched_docs:
      - `docs/chess-overlay-assistant/brief.md`
      - `docs/chess-overlay-assistant/plan.md`
      - `docs/chess-overlay-assistant/status.md`
    touched_code: []
    blockers:
      - `D23` stalemate wording decision remains open
      - product confirmation on whether checkmate needs stronger board treatment than the planned compact-status + king-highlight route
    next_step: review
  - owner: reviewer
    stage: review
    touched_docs:
      - `docs/chess-overlay-assistant/plan.md`
      - `docs/chess-overlay-assistant/status.md`
    touched_code: []
    blockers: []
    next_step: testing-handoff
  - owner: tester
    stage: testing-handoff
    touched_docs:
      - `docs/chess-overlay-assistant/plan.md`
      - `docs/chess-overlay-assistant/status.md`
    touched_code: []
    blockers:
      - no code change in this pass; execution testing begins after android-dev implementation of `Plan v2026-03-18.6`
    next_step: wait for implementation, then run the Task 5 manual checklist for check, checkmate, stalemate, and invalid-move attempts
  - owner: android-dev
    stage: implementation
    touched_docs:
      - `docs/chess-overlay-assistant/status.md`
      - `review-inputs.md`
    touched_code:
      - `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameStatus.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessRules.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStore.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameSnapshot.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
      - `app/src/test/java/com/rachitgoyal/chesshelper/domain/chess/ChessRulesTest.kt`
      - `app/src/test/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStoreTest.kt`
      - `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`
    blockers:
      - broader custom-rules regression depth remains a follow-up risk outside this lightest complete pass
    next_step: reviewer validates the authority boundary (`ChessRules`/`ChessGameStore` vs Stockfish) and tester runs manual gameplay-status checks in the overlay.
  - owner: reviewer
    stage: review
    touched_docs:
      - `docs/chess-overlay-assistant/status.md`
    touched_code: []
    blockers: []
    next_step: tester runs the manual gameplay-status checklist.
  - owner: orchestrator
    stage: discovery
    touched_docs:
      - `docs/chess-overlay-assistant/brief.md`
      - `docs/chess-overlay-assistant/status.md`
    touched_code: []
    blockers: []
    next_step: planning/design for stronger persistent Stockfish recommendations without heuristic fallback.
  - owner: orchestrator
    stage: planning
    touched_docs:
      - `docs/chess-overlay-assistant/plan.md`
      - `docs/chess-overlay-assistant/design.md`
      - `docs/chess-overlay-assistant/status.md`
    touched_code: []
    blockers: []
    next_step: implementation of persistent Stockfish reuse, stronger analysis defaults, and explicit overlay engine-error surfacing.
  - owner: android-dev
    stage: implementation
    touched_docs:
      - `docs/chess-overlay-assistant/status.md`
      - `docs/chess-overlay-assistant/review-inputs.md`
    touched_code:
      - `app/src/main/java/com/rachitgoyal/chesshelper/engine/EngineUnavailableException.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
      - `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`
    blockers:
      - command execution unavailable in this session, so compile/test execution still needs external validation
    next_step: reviewer validates no-fallback behavior, lifecycle teardown, and explicit engine-failure surfacing.
  - owner: reviewer
    stage: review
    touched_docs:
      - `docs/chess-overlay-assistant/review-inputs.md`
      - `docs/chess-overlay-assistant/status.md`
    touched_code: []
    blockers: []
    next_step: tester runs focused unit tests and a short manual overlay retry/failure-surfacing checklist.
  - owner: reviewer
    stage: review
    touched_docs:
      - `docs/chess-overlay-assistant/status.md`
    touched_code: []
    blockers: []
    next_step: tester validates repeated recommend requests, explicit engine-error surfacing, and overlay close/reopen behavior on device.
  - owner: tester
    stage: validation
    touched_docs: []
    touched_code: []
    blockers:
      - device/emulator validation still pending for real Stockfish process reuse and real engine-start failure surfacing
    next_step: run the manual overlay checklist on device/emulator; local automated validation already passed with `:app:testDebugUnitTest` and focused `OverlayBoardViewModelTest`.
  - owner: android-dev
    stage: bug-fix
    touched_docs:
      - `docs/chess-overlay-assistant/status.md`
    touched_code:
      - `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishAssetInstaller.kt`
      - `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt`
      - `app/src/test/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishAssetInstallerTest.kt`
      - `app/src/main/assets/stockfish/stockfish-android-armv8.gz`
      - `app/src/main/assets/stockfish/stockfish-android-x86_64.gz`
    blockers: []
    next_step: verify on-device that the rebuilt Android engine starts successfully on both a real arm64 device and, if used, an x86_64 emulator.



