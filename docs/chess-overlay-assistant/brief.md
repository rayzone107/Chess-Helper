---
feature: chess-overlay-assistant
stage: discovery
status: bug-fix-scoped
input_refs:
  - user-request: 2026-03-17
  - user-bug-fix-request: 2026-03-18
  - user-enhancement-request: 2026-03-18
code_refs:
  - app/src/main/java/com/rachitgoyal/chesshelper/MainActivity.kt
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/rachitgoyal/chesshelper/feature/overlay/service/OverlayWindowService.kt
last_updated: 2026-03-18
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

## Open questions
- Should the final overlay always launch immediately after permission grant, or should the app keep a manual launch/stop control center after grant?
- Should move analysis use a local engine such as Stockfish or an external service?
- What is the minimal board/state architecture needed before engine integration?
