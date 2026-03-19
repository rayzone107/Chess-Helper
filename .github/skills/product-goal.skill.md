---
kind: skill
name: product-goal
purpose: Stable product direction for the current app idea.
provides:
  - primary feature slug
  - core UX goals
  - unresolved product decisions
primary_feature_slug: chess-overlay-assistant
seed_doc: docs/chess-overlay-assistant/brief.md
---
- The app is meant to help the user study online chess with an overlay-style helper UI.
- The visible surface should show a full chess board with pieces and allow play for both sides.
- After the opponent move is entered, the app should recommend the best reply on demand.
- The board UI should support: piece selection state, last-move highlight, undo, expand/collapse, and drag/move of the overlay surface.
- Engine/provider is unresolved; planner should compare local engine options (for example Stockfish) versus external services before coding that part.
- The UX must stay usable as an overlay: compact when minimized, precise and clickable when expanded.

