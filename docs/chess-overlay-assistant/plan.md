---
feature: chess-overlay-assistant
stage: planning
status: proposed
input_refs:
  - docs/chess-overlay-assistant/brief.md
  - user-request: 2026-03-17
code_refs:
  - app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt
  - app/src/main/AndroidManifest.xml
  - app/build.gradle.kts
  - gradle/libs.versions.toml
last_updated: 2026-03-19
---

## Plan v2026-03-17.1

### Recommended MVP architecture
- **Surface**: ship MVP as an **in-app floating panel** inside `MainActivity`, not a system overlay. This keeps the repo single-process, avoids `SYSTEM_ALERT_WINDOW`/service complexity, and still proves drag/minimize/board UX. Revisit true system overlay only after the board + engine loop is stable.
- **Engine**: prefer a **local in-app engine boundary** over an external service. Define `MoveRecommendationEngine` now; wire a fake/simple implementation first if needed, then back it with bundled local Stockfish/UCI assets. External analysis is deferred to avoid backend/key/network/privacy work.
- **Rules/state**: use a chess rules library wrapper (recommended: `chesslib`) for legal moves, undo, FEN/history, while keeping app-facing state in small Kotlin models. Avoid writing move legality from scratch.
- **State split**:
  - `domain/chess`: game reducer/store, move history, selection, last-move highlight, whose turn.
  - `engine`: best-move request/response + engine session adapter.
  - `feature/overlay`: `OverlayBoardViewModel` and UI state for panel position, minimized state, loading/error.
  - `ui/components`: board grid, piece cell, highlights, controls.
- **Compose shape**: `MainActivity` -> `ChessHelperApp` -> `OverlayBoardRoute` -> `OverlayPanel` + `ChessBoard` + `OverlayControls`.

### Ordered tasks
| # | Task | Owner | Depends on | Repo refs | Acceptance check |
|---|---|---|---|---|---|
| 1 | Lock MVP decisions: in-app floating panel, local engine boundary, rules lib wrapper. | planner/dev | brief only | `docs/chess-overlay-assistant/brief.md`, `docs/chess-overlay-assistant/status.md`, `docs/chess-overlay-assistant/plan.md` | Decisions are documented; no hidden dependency on overlay permission/backend. |
| 2 | Replace starter screen with app shell + draggable/minimizable overlay panel scaffold. | android-dev | 1 | `app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/ui/app/ChessHelperApp.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardRoute.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt` | App launches to a movable panel that can expand/collapse without crashes. |
| 3 | Add chess domain/store with selection, legal move entry for both sides, undo, selected-piece and last-move highlights. | android-dev | 2 | `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStore.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameSnapshot.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/MoveRecord.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | User can enter legal moves for white/black, undo them, and see selection + last-move highlights update correctly. |
| 4 | Render the actual board/pieces and connect controls for recommend/undo/minimize. | android-dev | 3 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/BoardSquare.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayControls.kt`, `app/src/main/res/values/strings.xml` | Board is fully interactive; control actions reflect state immediately. |
| 5 | Add on-demand best-move recommendation after opponent moves through a local engine adapter. | android-dev | 3,4 | `app/src/main/java/com/rachitgoyal/chesshelper/engine/MoveRecommendationEngine.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/model/EngineRecommendation.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/LocalEngineCoordinator.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`, `app/src/main/assets/stockfish/` | Recommend button is enabled after an opponent move, shows loading, then displays a move or recoverable error. |
| 6 | Add verification for reducer + overlay behavior and document follow-up for real system overlay if still desired. | android-dev | 2,3,4,5 | `app/src/test/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStoreTest.kt`, `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`, `app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt`, `docs/chess-overlay-assistant/status.md` | Tests cover move/undo/highlight/recommend gating; status doc points to next implementation step. |

### Decisions to resolve during implementation
- **D1 resolved (2026-03-18)**: the MVP uses an in-repo rules engine in `domain/chess/ChessRules.kt` because a stable dependency choice was not confirmed quickly within the current repo constraints. The rules code is isolated behind `ChessGameStore` so a library-backed implementation can replace it later.
- **D2 resolved (2026-03-18)**: the MVP ships a local heuristic recommender behind `MoveRecommendationEngine` in `engine/local/LocalHeuristicMoveRecommendationEngine.kt`. A bundled Stockfish/UCI adapter remains the intended upgrade path without changing the UI contract.
- **D3**: if true cross-app overlay remains a product requirement after MVP, create a follow-up plan for `SYSTEM_ALERT_WINDOW`, a foreground/service host, and permission UX in `AndroidManifest.xml` rather than folding it into this first pass.

### Next reader
- Start with Task 2 only after keeping Task 1 decisions intact; treat system overlay support and remote analysis as explicit follow-ups, not scope creep inside MVP.

## Plan v2026-03-18.1

### Cross-app overlay conversion constraints
- `SYSTEM_ALERT_WINDOW` is a **special app-ops permission**, so this repo must deep-link to `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` from a visible activity and re-check with `Settings.canDrawOverlays(...)`; there is no normal runtime permission dialog path.
- With `minSdk = 34` and `targetSdk = 36` in `app/build.gradle.kts`, the overlay window should use `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` only; legacy phone/system alert window types are out of scope.
- A persistent overlay that remains after the app UI is backgrounded should be started from an explicit foreground user action and hosted by a service; for this target-SDK level, confirm whether the service must run as a foreground service with an ongoing notification (likely) before implementation is finalized.
- The current Compose panel can be reused, but once it moves out of `MainActivity` it needs a service-owned `ComposeView`/state host instead of relying on activity-only composition and `viewModel()` defaults.

### Likely file targets
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/ui/app/ChessHelperApp.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardRoute.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowService.kt` *(new)*
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt` *(new, if the `WindowManager` bridge is split out)*
- `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`

### Ordered tasks
| # | Task | Owner | Depends on | Repo refs | Acceptance check |
|---|---|---|---|---|---|
| 1 | Lock the Android overlay/service path for this repo: special-permission UX, service lifetime, and whether the overlay host must be foreground-service-backed for `targetSdk 36`. | planner/android-dev | `brief.md`, `status.md`, current app config | `docs/chess-overlay-assistant/brief.md`, `docs/chess-overlay-assistant/status.md`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml` | A short implementation note in code/docs names the approved service model and explicitly captures the permission + target-SDK constraints above. |
| 2 | Replace the in-app-only launch path with a permission-aware entry flow that can request overlay access, re-check on resume, and start/stop the overlay host from `MainActivity`. | android-dev | 1 | `app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/ui/app/ChessHelperApp.kt`, `app/src/main/res/values/strings.xml` | From a clean install, the app can send the user to overlay settings, detect the result on return, and expose a working start/stop overlay action without crashes. |
| 3 | Add the manifest and service plumbing for a true cross-app overlay window, including permission declaration, service registration, and notification pieces if foreground-service hosting is required. | android-dev | 1 | `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowService.kt`, `app/src/main/res/values/strings.xml` | The service can be started from the app, creates exactly one overlay host, and tears it down cleanly on stop/destroy with no leaked window. |
| 4 | Rehost the existing chess overlay Compose UI inside the service-managed `WindowManager` view while preserving drag, minimize/expand, and current board/recommendation behavior. | android-dev | 2,3 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardRoute.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt` | The same chess panel appears above other apps, dragging updates the overlay window position instead of in-activity bounds only, and minimize/expand still works while the underlying app stays usable. |
| 5 | Add focused regression coverage for the migrated overlay state/host boundaries and document the manual validation steps that cannot be fully automated (permission settings and cross-app behavior). | android-dev | 4 | `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`, `docs/chess-overlay-assistant/status.md` | Automated tests still cover panel state transitions/recommendation behavior, and the status doc records a manual checklist for permission grant, app-switching, drag, minimize, and overlay shutdown. |

### Decisions to resolve before coding
- **D4**: Confirm the service lifetime model for `targetSdk 36`: plain started service vs foreground service with ongoing notification. Do not assume the current MVP activity lifecycle is sufficient once the overlay must survive app switching.
- **D5**: Decide where overlay position becomes source-of-truth after migration—keep `OverlayBoardViewModel` offsets and mirror them into `WindowManager.LayoutParams`, or let the service/window host own raw pixel coordinates and keep the view-model UI-only.
- **D6**: Decide whether `OverlayBoardRoute` stays directly reusable in a service-hosted `ComposeView` or whether a thin overlay-specific host composable is needed to avoid activity-only dependencies.

### Next reader
- Start with Task 1 and verify the service-lifetime decision against this repo's `targetSdk = 36` before touching UI code; Tasks 2-4 should reuse the current board/recommendation logic rather than rebuilding the chess overlay from scratch.


## Plan v2026-03-18.2

### Delta scope
- Keep the overlay chrome on the earlier dark treatment instead of the new light card styling.
- Remove the verbose helper copy above/below the board; keep only a concise `Color to move` signal in the panel chrome.
- Replace `Minimize` / `Close` text actions with compact top-right icon-style window controls.
- Show an inline loading state inside `Show best move` while recommendation work is in flight.
- Replace the current square-only recommendation preview with an on-board move arrow that respects board flipping.

### Likely file targets
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayControls.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/MoveRecommendationEngine.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/LocalHeuristicMoveRecommendationEngine.kt`
- `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`
- `app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt`
- `gradle/libs.versions.toml` and `app/build.gradle.kts` *(only if Material icon assets are added instead of drawing/custom vectors)*

### Ordered tasks
| # | Task | Owner | Depends on | Repo refs | Acceptance check |
|---|---|---|---|---|---|
| 1 | Restore the overlay container/header to the dark visual treatment, collapse the current descriptive copy to a single concise `Color to move` signal, and move window actions into compact top-right icon controls for both expanded and minimized states. | android-dev | current overlay implementation | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`, `app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts` | Expanded/minimized overlay chrome is dark again, no verbose top/bottom helper paragraphs remain, and minimize/close are icon affordances rather than text buttons. |
| 2 | Make recommendation requests visibly asynchronous so the `Show best move` CTA can show an inline spinner/loading label while work is computing and reject duplicate taps until completion. | android-dev | 1 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayControls.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/MoveRecommendationEngine.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/LocalHeuristicMoveRecommendationEngine.kt`, `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt` | Tapping `Show best move` immediately swaps the button into an inline loading state that remains visible until a result/error lands, and a second request cannot start while loading. |
| 3 | Replace the current recommended-move square tinting with an arrow overlay that anchors to the source/target squares correctly for both White-bottom and Black-bottom board orientations. | android-dev | 1,2 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`, `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt` | A ready recommendation draws one clear arrow from source to destination, the arrow disappears when the recommendation is stale/cleared/applied, and the arrow orientation still matches the flipped board perspective. |
| 4 | Tighten regression coverage around the revised panel chrome and recommendation affordances, then record the UI delta in the feature docs after implementation. | android-dev | 1,2,3 | `app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt`, `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`, `docs/chess-overlay-assistant/status.md` | Tests/assertions cover the new loading state and icon-control flow, and `status.md` notes the dark-theme restoration + arrow preview once verified. |

### Decisions to resolve before coding
- **D7**: Use Compose Material icons vs custom/vector drawables for the window controls. If Material icons are chosen, confirm whether the repo should add `material-icons-extended` or keep the implementation dependency-free.
- **D8**: Decide whether the recommendation engine contract becomes `suspend`/background-threaded at the boundary, or whether the view-model alone should dispatch the current synchronous engine off the main thread to guarantee the loading state is actually visible.
- **D9**: Decide whether the move arrow stays inside `ChessBoard.kt` as a board-overlay `Canvas`, or whether a small dedicated composable/layer is warranted for readability and future arrow styling.

### Next reader
- Yes, use versioned plan docs here: append implementation follow-ups as new `Plan vYYYY-MM-DD.N` sections in `docs/chess-overlay-assistant/plan.md` rather than rewriting earlier plan history or creating an unversioned replacement.
- Start with Task 1 in `OverlayPanel.kt`; do not touch engine/UI tests until the dark chrome + compact header contract is fixed because that contract changes node text and control discovery.


## Plan v2026-03-18.3

### Delta scope
- Allow the live cross-app overlay window to be dragged partially off-screen instead of forcing the full card to remain inside the display bounds.
- Keep a small, intentional visible strip on-screen at all times so the user can still find and drag the overlay back; use a safe clamp target of **40-56dp visible** on the nearest screen edge.
- Make the clamp logic live in the real overlay host path (`OverlayWindowService` + `WindowManager`) rather than relying on the old in-activity full-screen root bounds model.

### Real host path to change
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt`
  - `show()`
  - `moveBy(delta: Offset)`
  - `createLayoutParams()`
  - **likely new helpers**: `clampPosition(...)`, `currentWindowBoundsPx()`, `minimumVisibleStripPx()`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardRoute.kt`
  - `OverlayWindowContent(...)`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
  - `onDragStart()` / `onDragEnd()`
  - review whether `onDrag(...)`, `onRootBoundsChanged(...)`, `onPanelSizeChanged(...)`, and `clampOffset(...)` should stop acting as the source of truth for service-hosted window coordinates
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
  - `OverlayWindowCard(...)` / size reporting only if the host needs the rendered card size to clamp correctly after expand/minimize
- `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt` *(only if view-model drag responsibilities change)*
- `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHostTest.kt` *(new, if a pure clamp helper is extracted and unit-testable)*

### Safe clamp strategy
- Treat the `WindowManager.LayoutParams.x/y` values in `OverlayWindowHost` as the authoritative overlay position for the cross-app window.
- Clamp horizontally so the card may slide beyond the left/right display edges, but never so far that less than the minimum strip remains visible:
  - `minX = -(panelWidth - visibleStripPx)`
  - `maxX = displayWidth - visibleStripPx`
- Clamp vertically with the same principle, while still respecting top/bottom safe space for status/navigation areas if those bounds are available from the current window metrics:
  - `minY = topSafeInset - (panelHeight - visibleStripPx)`
  - `maxY = displayHeight - bottomSafeInset - visibleStripPx`
- Use `visibleStripPx = 40.dp..56.dp` converted once from density in the host; default to `48.dp` unless device testing shows the minimized header or drag affordance needs more.
- Re-run the clamp whenever the overlay content size changes (expanded/minimized) so the panel cannot become fully unreachable after a mode switch.

### Ordered tasks
| # | Task | Owner | Depends on | Repo refs | Acceptance check |
|---|---|---|---|---|---|
| 1 | Move the drag-boundary logic to the real `WindowManager` host by extracting a clamp helper around `OverlayWindowHost.moveBy(...)` and `createLayoutParams()`, using the current display metrics plus a minimum visible strip instead of full in-app bounds. | android-dev | current overlay service path | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt` | Dragging left/right/up/down can place most of the overlay off-screen, but at least the configured visible strip always remains reachable. |
| 2 | Trim or repurpose the old Compose-root bounds coupling so the service-hosted overlay no longer depends on `OverlayBoardViewModel.clampOffset(...)` for actual cross-app window placement; keep only drag state/UI state there unless a secondary in-app mode still needs local clamping. | android-dev | 1 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardRoute.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt` | The real overlay position is controlled by the host, and expand/minimize/drag still feel correct without fighting a second Compose-side clamp. |
| 3 | Add focused regression coverage for the clamp math and record a short manual verification checklist for expanded and minimized sizes near each screen edge. | android-dev | 1,2 | `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHostTest.kt`, `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`, `docs/chess-overlay-assistant/status.md` | Tests cover left/right/top/bottom clamp cases and manual validation confirms a small visible edge remains draggable in both panel modes. |

### Decisions to resolve before coding
- **D10**: Should `OverlayWindowHost` measure the rendered overlay view width/height directly from the `ComposeView`, or should Compose continue reporting size through the existing panel callbacks and a shared state object?
- **D11**: Which bounds source should the host trust on Android 14+/`targetSdk 36` for overlays: raw current window metrics, maximum window metrics, or a custom inset-adjusted rectangle for cutout/status/navigation safety?
- **D12**: Should the visible-strip constant vary by panel mode (for example, a slightly larger strip for the minimized chip/header) or stay fixed at one density-based value such as `48.dp`?

### Next reader
- Start in `OverlayWindowHost.kt`, not `OverlayPanel.kt`: this enhancement is about the real cross-app overlay window managed by the service/`WindowManager`, and the Compose layer should only supply drag events and any size signal the host still needs.


## Plan v2026-03-18.4

### Stockfish engine integration path
- Treat the checked-in ARM64 engine binary at `stockfish/stockfish-android-armv8` as the source artifact for Android; do **not** try to execute it from the repo root at runtime.
- Prefer a build-time copy into packaged app assets over hand-maintaining a duplicate binary in source control. Recommended packaged location: generated main assets under `app/build/generated/.../stockfish/stockfish-android-armv8`, with `Copying.txt` bundled beside it.
- On device, copy the packaged binary from assets into an app-private executable location such as `Context.noBackupFilesDir/stockfish/stockfish-android-armv8`, then mark it executable before launching via `ProcessBuilder`.
- Speak plain UCI over the process stdin/stdout (`uci`, `isready`, `ucinewgame`, `position fen ...`, `go depth ...`, `quit`) from Kotlin, and translate `bestmove` into the existing `EngineRecommendation` contract.
- Keep the current heuristic engine as an explicit fallback path if binary install, process spawn, UCI handshake, or move parsing fails; the UI should degrade to a recoverable warning rather than losing recommendations entirely.

### Likely file targets
- `stockfish/stockfish-android-armv8`
- `stockfish/Copying.txt`
- `app/build.gradle.kts`
- `app/src/main/assets/stockfish/` *(new generated or checked-in packaged location; generated is preferred)*
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/MoveRecommendationEngine.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/model/EngineRecommendation.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/LocalHeuristicMoveRecommendationEngine.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/StockfishBinaryInstaller.kt` *(new)*
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/StockfishUciClient.kt` *(new)*
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/StockfishMoveRecommendationEngine.kt` *(new)*
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/FallbackMoveRecommendationEngine.kt` *(new, if wrapper stays separate)*
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardRoute.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt` *(only if app-level engine wiring/factory is introduced there)*
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/rachitgoyal/chesshelper/engine/local/LocalHeuristicMoveRecommendationEngineTest.kt`
- `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`
- `app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt` *(only if fallback/startup messaging is surfaced in UI)*

### Ordered tasks
| # | Task | Owner | Depends on | Repo refs | Acceptance check |
|---|---|---|---|---|---|
| 1 | Lock the packaging strategy for the real engine: source binary is `stockfish/stockfish-android-armv8`, build copies it plus GPL notice into packaged assets, and runtime extracts it into an app-private executable file. | planner/android-dev | current repo layout | `stockfish/stockfish-android-armv8`, `stockfish/Copying.txt`, `app/build.gradle.kts` | A short note in code/docs and the Gradle wiring clearly identify the source binary, packaged path, and extracted runtime path; no manual post-build copy step is required. |
| 2 | Add the asset packaging/copy plumbing so debug/release builds always include the ARMv8 engine payload and required license text. | android-dev | 1 | `app/build.gradle.kts`, `app/src/main/assets/stockfish/` *(generated or checked-in)* | Building the app produces an APK/AAB containing `stockfish/stockfish-android-armv8` and `stockfish/Copying.txt` under app assets. |
| 3 | Add a runtime installer that checks whether the extracted binary already exists/currently matches the packaged asset, copies it into `noBackupFilesDir` (or equivalent app-private dir), and marks it executable before first use. | android-dev | 2 | `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/StockfishBinaryInstaller.kt` | On first engine use, the app can obtain a readable/executable binary path without crashing; repeated launches skip unnecessary rewrites unless the packaged binary changed. |
| 4 | Implement a Kotlin UCI client around `ProcessBuilder` that starts Stockfish, performs `uci` + `isready` handshake, sends `position fen ...` / `go depth ...`, parses `bestmove`, and shuts the process down cleanly. | android-dev | 3 | `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/StockfishUciClient.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/StockfishMoveRecommendationEngine.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/model/EngineRecommendation.kt` | A unit/integration-style test can feed a FEN and receive a parsed move recommendation from the real process, and the process does not leak after completion/error. |
| 5 | Wrap the real Stockfish engine with an explicit fallback strategy so failures during install/startup/handshake/analysis drop back to `LocalHeuristicMoveRecommendationEngine` and surface a recoverable status message. | android-dev | 4 | `app/src/main/java/com/rachitgoyal/chesshelper/engine/MoveRecommendationEngine.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/LocalHeuristicMoveRecommendationEngine.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/local/FallbackMoveRecommendationEngine.kt`, `app/src/main/res/values/strings.xml` | If Stockfish cannot start, recommendation requests still return a heuristic move, and the UI/state can tell the user that fallback mode was used instead of silently failing. |
| 6 | Rewire overlay/app engine construction so both the activity-hosted and service-hosted overlay paths can create the context-aware real engine safely, ideally from one shared factory instead of hardcoding `LocalHeuristicMoveRecommendationEngine()` in the view-model default. | android-dev | 5 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardRoute.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt` | The overlay uses the same engine stack whether shown in-app or from the overlay service, and no path depends on a context-less default constructor for the real engine. |
| 7 | Add focused tests and a smoke-check checklist for asset presence, extraction/executable permissions, UCI parsing, and fallback behavior. | android-dev | 3,4,5,6 | `app/src/test/java/com/rachitgoyal/chesshelper/engine/local/LocalHeuristicMoveRecommendationEngineTest.kt`, `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`, `docs/chess-overlay-assistant/status.md` | Tests cover the fallback and request flow, and the status doc records a device smoke test: launch overlay, request move, confirm Stockfish path works, then simulate a startup failure and confirm heuristic fallback. |

### Decisions to resolve before coding
- **D13**: Build packaging shape: generated assets from `app/build.gradle.kts` vs checking a copied binary into `app/src/main/assets/stockfish/`. Generated assets avoid drift; confirm that choice before implementation.
- **D14**: Engine lifecycle: one long-lived warmed Stockfish process reused across requests vs spawn-per-request. A reused session is faster, but needs explicit synchronization and cleanup for both `MainActivity` and `OverlayWindowService` paths.
- **D15**: Contract shape at the engine boundary: keep `MoveRecommendationEngine.recommend(...)` synchronous and hide threading internally, or make it suspend/result-based so startup/fallback/error states are first-class.
- **D16**: How much fallback state should reach UI: summary suffix only, a dedicated `recommendationError`/status banner, or a structured source field on `EngineRecommendation` to indicate `stockfish` vs `heuristic`.
- **D17**: GPL packaging completeness for this app distribution: whether bundling `Copying.txt` plus the checked-in `stockfish/` source tree in-repo is sufficient for release artifacts, or whether an in-app/open-source-notices surface should be added as part of the same change.

### Next reader
- Start with Task 1 in `app/build.gradle.kts` and the exact binary path `stockfish/stockfish-android-armv8`; do not wire Kotlin process code until the packaged/extracted file path is fixed, because every runtime class depends on that path contract.


## Plan v2026-03-18.5

### Restore target
- Reconcile the repo to the latest intended combined state from feature history, not to any single intermediate snapshot:
  - **preserve** the real cross-app overlay permission + foreground-service/window-host flow from `Plan v2026-03-18.1` and `Plan v2026-03-18.3`
  - **preserve** the real Stockfish packaging/runtime/fallback integration from `Plan v2026-03-18.4`
  - **restore** the cleaned overlay UI contract from `Plan v2026-03-18.2` and `design.md`: dark chrome, compact header, no verbose study-side copy, icon minimize/close controls, solid white pieces, recommendation arrow, and `Play suggested move`

### Preserve-as-source-of-truth refs
- `app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowService.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowHost.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowPositioning.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelFactory.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishAssetInstaller.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveTranslator.kt`
- `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/ChessPositionFenEncoder.kt`
- `app/src/main/assets/stockfish/stockfish-android-armv8`
- `app/src/main/assets/stockfish/COPYING.txt`

### Likely regressed files to restore
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
  - currently shows the old light card + verbose `Floating coach board` copy + text `Minimize`/`Close`; restore the compact dark header/chip contract.
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayControls.kt`
  - still uses `Study side` copy and lacks the final CTA wording/loading/apply-move affordance; restore `You:`, `Show best move`, and `Play suggested move` behavior.
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`
  - still renders an unflipped board with no recommendation arrow path; restore flipped-perspective rendering and the arrow overlay while keeping last-move/selection highlights.
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`
  - likely missing the final UI-facing fields/computed state for board orientation, active recommendation preview, and apply-suggested-move enablement.
- `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
  - preserve async recommendation/Stockfish usage, but restore the final UI behavior for recommendation preview lifecycle and direct application of the suggested move.
- `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/Piece.kt`
  - likely involved in the white-piece visibility regression if earlier history changed glyph strategy; restore only if the final solid-white treatment depends on piece-symbol mapping instead of board-side rendering.
- `app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt`
  - currently asserts the pre-cleanup chrome/text controls; restore expectations to the icon-control/compact-header contract.
- `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`
  - extend/realign coverage around apply-suggested-move, arrow-preview staleness, White-first recommendation access, and any restored board-orientation state.

### Ordered tasks
| # | Task | Owner | Depends on | Repo refs | Acceptance check |
|---|---|---|---|---|---|
| 1 | Freeze the reconciliation baseline: treat the current overlay permission/service host and Stockfish engine path as non-regression anchors, and restore UI behavior on top of them instead of checking out an older overlay snapshot wholesale. | planner/dev | current repo + `Plan v2026-03-18.1`/`.2`/`.3`/`.4` | `docs/chess-overlay-assistant/plan.md`, `docs/chess-overlay-assistant/design.md`, `docs/chess-overlay-assistant/status.md`, preserve refs above | The restore note explicitly says `MainActivity`/manifest/service/Stockfish classes are the keepers unless a UI restore proves a targeted compatibility fix is required. |
| 2 | Restore the cleaned overlay chrome in both expanded and minimized modes without touching the service host contract: dark treatment, compact header, no sentence-length helper copy, and icon minimize/close controls with explicit semantics. | android-dev | 1 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`, `app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt` | Overlay content shown from both `OverlayBoardRoute` and `OverlayWindowHost` matches the compact dark UI contract; no `Floating coach board`, `Study side`, or text window actions remain. |
| 3 | Restore the final recommendation UX on the existing async/Stockfish-backed state flow: `Show best move` loading, on-board arrow preview, and `Play suggested move` action that applies the recommended move and clears stale preview state correctly. | android-dev | 1,2 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayControls.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`, `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt` | Recommendation requests still flow through the preserved engine stack, show inline loading, render one arrow in the current board orientation, and can be applied directly from the panel without leaking stale results. |
| 4 | Restore board readability details that were part of the intended cleaned UI: solid-looking white pieces and correct board flip/perspective for the assisted side, while keeping the existing legal-move/last-move data model intact. | android-dev | 3 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/Piece.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | White pieces are clearly visible against light squares, Black perspective flips correctly, and the recommendation arrow/source-target math still matches the displayed orientation. |
| 5 | Re-run targeted regression coverage and update feature docs so the repo records the reconciled end state rather than the currently mixed snapshot. | android-dev | 2,3,4 | `app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt`, `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`, `docs/chess-overlay-assistant/status.md` | Tests/assertions name the restored chrome/CTA semantics, and `status.md` states that the app now combines real overlay hosting + Stockfish with the cleaned compact overlay UI. |

### Decisions to resolve before restore work starts
- **D18**: Solid white pieces implementation source of truth — restore prior filled glyph/asset treatment in `ChessBoard.kt`, or restore earlier piece-symbol mapping in `Piece.kt` only if that is what the feature history used.
- **D19**: `Play suggested move` placement — keep it as a dedicated secondary CTA in `OverlayControls.kt` vs a contextual button shown only when `recommendationState == READY`; resolve before updating tests.
- **D20**: Board orientation source of truth — derive display orientation from `assistedSide` in `OverlayBoardUiState`/`OverlayBoardViewModel`, not from service-host state, so both activity-hosted and service-hosted overlay paths stay aligned.

### Next reader
- Start by restoring `OverlayPanel.kt`, `OverlayControls.kt`, and `ChessBoard.kt`; do **not** revert `MainActivity`, `AndroidManifest.xml`, `OverlayWindowService.kt`, `OverlayWindowHost.kt`, or anything under `engine/stockfish/` unless a narrowly scoped compatibility issue is proven.


## Plan v2026-03-18.6

### Route choice: lightest complete enhancement
- Stay on the existing `chess-overlay-assistant` slug.
- Treat this as an **enhancement to the current domain + overlay stack**, not a new feature track and not an engine rewrite.
- Keep move legality in `domain/chess`; do **not** make Stockfish responsible for blocking user moves. Stockfish should continue to recommend moves only, while the app validates both user-entered and engine-suggested moves against the current legal move set.

### Why this is the lightest route
- `ChessRules.legalMovesFrom(...)` already filters moves that leave the moving side in check via `!isKingInCheck(...)`.
- `ChessGameStore.tapSquare(...)` and `ChessGameStore.applyMove(...)` already route all move entry through that legal-move layer.
- `StockfishMoveTranslator.legalMoveFromUci(...)` already rejects engine output that is not legal in the current position.
- The missing pieces are mainly: **explicit game-state classification**, **clear UI surfacing for check/checkmate**, and **regression coverage**.

### Implementation shape
- Add a small domain-facing game-status model so the overlay can distinguish:
  - `NORMAL`
  - `CHECK`
  - `CHECKMATE`
  - `STALEMATE`
- Expose the checked king square when relevant so the board can render a clear in-play signal without inventing new legality rules in the UI.
- Keep recommendation behavior aligned with the new state:
  - recommend stays disabled for terminal states
  - stale/ready recommendation text must not hide check/checkmate status
  - `Play suggested move` continues to rely on store legality, not Stockfish authority

### Exact file refs to inspect/change
- Inspect/change `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessRules.kt`
  - add a small helper/API for position-state classification using existing `isKingInCheck(...)` + `legalMoves(...)`
  - keep legality authority here
- Inspect/change `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStore.kt`
  - surface the richer status in snapshots
  - keep move-entry blocking behavior unchanged except for any bug fix uncovered by tests
- Inspect/change `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameSnapshot.kt`
  - add `gameStatus` and any checked-king-square field the UI needs
- Likely add `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameStatus.kt` *(new)*
- Inspect/change `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`
  - carry UI-facing check/checkmate state
  - ensure `canRecommend` and compact status text derive from terminal-state data instead of `isGameOver` alone
- Inspect/change `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`
  - propagate richer snapshot state with no new legality logic
  - keep recommendation invalidation behavior intact
- Inspect/change `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`
  - add a clear checked-king visual treatment that coexists with selection/legal-target/last-move highlights
- Inspect/change `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`
  - show concise `Check` / `Checkmate` / `Stalemate` status in the existing compact chrome/banner
- Inspect/confirm `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveTranslator.kt`
  - no behavior change expected beyond preserving the current legal-move validation of engine output
- Inspect/change `app/src/test/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStoreTest.kt`
  - add store-level legality/status assertions
- Likely add `app/src/test/java/com/rachitgoyal/chesshelper/domain/chess/ChessRulesTest.kt` *(new)*
  - add focused rules regressions for check, checkmate, stalemate, and self-check filtering
- Inspect/change `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`
  - assert UI-state propagation for check/checkmate and recommendation gating in terminal positions
- Optional follow-up only if text semantics change materially: `app/src/androidTest/java/com/rachitgoyal/chesshelper/OverlayPanelUiTest.kt`

### Ordered tasks
| # | Task | Owner | Depends on | Repo refs | Acceptance check |
|---|---|---|---|---|---|
| 1 | Lock the authority decision: domain rules stay authoritative for legality; Stockfish remains recommendation-only and continues to be validated through legal-move translation. | planner/reviewer | brief + current code | `docs/chess-overlay-assistant/brief.md`, `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessRules.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveTranslator.kt` | The plan/status docs explicitly state that invalid-move prevention does **not** move into Stockfish. |
| 2 | Add explicit game-state classification in the domain layer and surface it through snapshots. | android-dev | 1 | `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessRules.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStore.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameSnapshot.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/domain/chess/model/GameStatus.kt` *(new)* | The app can distinguish normal/check/checkmate/stalemate without inferring from `isGameOver` alone. |
| 3 | Surface the new state in overlay UI with the smallest clear gameplay treatment: compact status text plus checked-king emphasis on the board, while preserving current selection/last-move/recommendation visuals. | android-dev | 2 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/ChessBoard.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt` | During live play, check is clearly visible and terminal states label checkmate vs stalemate correctly without regressing the existing overlay chrome. |
| 4 | Back the slice with regression coverage for legality and state propagation, especially positions where the king is in check or a move would expose the king. | android-dev | 2,3 | `app/src/test/java/com/rachitgoyal/chesshelper/domain/chess/ChessRulesTest.kt` *(new)*, `app/src/test/java/com/rachitgoyal/chesshelper/domain/chess/ChessGameStoreTest.kt`, `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt` | Tests cover self-check filtering, forced-response-while-in-check behavior, checkmate/stalemate classification, and UI recommendation gating in terminal states. |
| 5 | Re-review the implementation against the authority decision and the open review risk around broader chess-rules coverage, then hand off to testing with a concise manual sanity checklist. | reviewer/tester | 4 | `review-inputs.md`, `docs/chess-overlay-assistant/status.md`, above code refs | Reviewer confirms Stockfish did not become the legality gate, and testing gets a short checklist for check, checkmate, stalemate, and invalid-move attempts. |

### Decisions locked for this slice
- **D21 resolved (2026-03-18)**: Stockfish should **not** directly prevent user moves. The legal-move gate remains `ChessRules`/`ChessGameStore`; Stockfish is asynchronous, optional, and already safely translated back through the legal move list.
- **D22 resolved (2026-03-18)**: The smallest clear UI treatment is a compact status signal plus a checked-king board highlight, not a new modal/banner flow.
- **D23 open**: Decide whether the non-check terminal state should now show explicit `Stalemate` text everywhere, or whether only checkmate gets special wording while other terminal states remain `Game over`.

### Reviewer focus
- Verify that the implementation does not duplicate legality logic between `ChessRules`, `ChessGameStore`, `OverlayBoardViewModel`, and the UI.
- Verify that checkmate is not inferred from `isGameOver` alone; the side-to-move must also be in check.
- Verify that any engine-suggested move still goes through store/domain legality checks before application.

### Next reader
- Start in `ChessRules.kt` and `GameSnapshot.kt`, not in `OverlayPanel.kt`: the UI cannot show check/checkmate clearly until the domain layer exposes the correct status and checked-king signal.



## Plan v2026-03-19.1

### Route choice: smallest complete enhancement
- Stay on the existing `chess-overlay-assistant` slug.
- Treat this as a **targeted engine-quality enhancement** to the current Stockfish-backed recommendation path, not a new engine architecture and not a domain-rules rewrite.
- Keep `StockfishMoveTranslator` / legal-move validation in place so stronger analysis does not bypass app legality checks.

### Why this is the lightest route
- The repo already packages Stockfish assets and constructs `StockfishMoveRecommendationEngine` in both production overlay entry points:
  - `OverlayBoardViewModelFactory.kt`
  - `feature/overlay/service/OverlayWindowHost.kt`
- The biggest quality problem is not missing engine integration; it is the **current engine lifecycle and failure policy**:
  - a brand-new Stockfish process is started for every request
  - search defaults are short/weak for analysis quality
  - any engine failure silently drops to the heuristic engine
  - the overlay compresses recommendation failures into generic `No move`
- Fixing those points directly yields a stronger user-facing result with the smallest code delta.

### Implementation shape
- Replace the per-request process model in `StockfishMoveRecommendationEngine` with a **persistent reusable UCI session** that:
  - installs the binary once as today
  - starts Stockfish once per engine instance
  - performs `uci`/`isready` handshake once
  - reuses the same process for subsequent `position fen ...` + `go ...` requests
  - tears the process down when the owning view-model/host is cleared
- Strengthen default analysis settings for recommendation quality, favoring stronger output over latency. Expected levers for this pass:
  - longer search time per move
  - larger hash size
  - explicit analysis-mode UCI options where available
- Remove heuristic fallback from the production Stockfish path. Engine startup/search/parse failures should become explicit surfaced errors instead of silently returning `LocalHeuristicMoveRecommendationEngine` moves.
- Keep the overlay UI compact but explicit:
  - loading: `Analyzing…`
  - ready: current best-move behavior
  - failure: compact engine-error status plus an expanded-mode detail line/message

### Exact file refs to inspect/change
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
- Likely add `app/src/test/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngineTest.kt` *(new if a pure unit-test seam is extracted)*

### Ordered tasks
| # | Task | Owner | Depends on | Repo refs | Acceptance check |
|---|---|---|---|---|---|
| 1 | Lock the production behavior change: Stockfish remains the only production recommender, no heuristic fallback is allowed, and engine failures must surface distinctly from `No move`. | planner/reviewer | brief + current code | `docs/chess-overlay-assistant/brief.md`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt` | The plan/status/docs explicitly state that heuristic fallback is removed from production recommendation flow. |
| 2 | Refactor the Stockfish engine lifecycle to reuse a persistent UCI process across requests, with clean setup, synchronization, and teardown. | android-dev | 1 | `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishAssetInstaller.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/engine/MoveRecommendationEngine.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | Repeated recommendations for one overlay session reuse the same Stockfish process and still shut down cleanly when the owner is destroyed. |
| 3 | Raise Stockfish analysis-strength defaults for stronger output while keeping request handling single-flight and UI-visible. | android-dev | 2 | `app/src/main/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngine.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt` | The engine uses meaningfully stronger defaults than the old short per-request search, and loading remains visible until the result lands. |
| 4 | Surface engine failures clearly in overlay state and compact UI without regressing the existing ready/loading/apply recommendation flow. | android-dev | 1,2,3 | `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardUiState.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModel.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayPanel.kt`, `app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/components/OverlayControls.kt` | Users see an explicit engine failure status/message instead of a misleading heuristic move or generic `No move`. |
| 5 | Add focused regression coverage for failure surfacing and persistent-session state flow, then run reviewer → tester handoff on the new engine contract. | android-dev/reviewer/tester | 2,3,4 | `app/src/test/java/com/rachitgoyal/chesshelper/feature/overlay/OverlayBoardViewModelTest.kt`, `app/src/test/java/com/rachitgoyal/chesshelper/engine/stockfish/StockfishMoveRecommendationEngineTest.kt` *(if added)*, `docs/chess-overlay-assistant/review-inputs.md`, `docs/chess-overlay-assistant/status.md` | Tests prove engine exceptions surface in UI state, review confirms no hidden fallback remains, and testing gets a small manual checklist for repeated recommend requests + engine failure surfacing. |

### Decisions locked for this slice
- **D24 resolved (2026-03-19)**: Production recommendation flow must not fall back to `LocalHeuristicMoveRecommendationEngine`; if Stockfish fails, the overlay should surface the failure.
- **D25 resolved (2026-03-19)**: Persistent process reuse is required for this pass because stronger search settings on a spawn-per-request engine would increase latency without delivering the intended quality win.
- **D26 resolved (2026-03-19)**: Keep legality authority unchanged—Stockfish suggestions still pass through `StockfishMoveTranslator.legalMoveFromUci(...)` before preview/apply.
- **D27 open**: Choose the exact user-facing compact error copy (`Engine error`, `Engine unavailable`, or a similarly short label) once the overlay state shape is finalized.

### Reviewer focus
- Verify that `StockfishMoveRecommendationEngine` no longer instantiates a heuristic fallback in production flow.
- Verify that the engine process is actually reused across calls and closed when the owner is destroyed.
- Verify that the overlay can now distinguish engine failure from `No move` / terminal-position gating.

### Next reader
- Start in `StockfishMoveRecommendationEngine.kt`, then thread the resulting explicit failure state through `OverlayBoardViewModel.kt` and `OverlayBoardUiState.kt`. Do not broaden the change into a larger engine abstraction rewrite unless the persistent-session implementation proves impossible inside the current file boundaries.
