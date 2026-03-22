---
feature: app-improvements
stage: review-blocked
status: changes-requested
input_refs:
  - docs/app-improvements/brief.md
  - docs/app-improvements/plan.md
  - review-inputs.md
last_updated: 2026-03-22
---
- active_owner: coder
- current_state: >-
    Reviewer pass on close-confirmation dialog, match history persistence + replay, and
    Play-as dropdown migration. Three new issues found (review-match-history-004 through -006).
    One is high severity (double-save producing duplicate match records), one is medium (FEN-loaded
    game replay starts from wrong position), and one is low (loadAll on every recomposition).
    The high-severity item blocks tester handoff.
- next_reader: coder
- next_step: >-
    Fix review-match-history-004 (double-save guard) and review-match-history-005 (FEN replay
    starting position). Return for re-review once both are addressed.
- reviewer_note: >-
    Feature 1 (close dialog) - Clean. AlertDialog intercepts X correctly in both modes.
    Feature 2 (match history) - Repository, model, screens well-structured. Two bugs block
    approval: (1) no double-save guard, (2) replay starts from wrong position for FEN-loaded games.
    Feature 3 (play-as dropdown) - Clean single-row layout, no issues.
    Architecture fit - New files follow existing patterns. Thread safety acceptable.
- unresolved_risks:
    - review-match-history-004 (HIGH) double-save on game-over + close/reset
    - review-match-history-005 (MEDIUM) FEN-loaded game replay produces wrong board
    - review-match-history-006 (LOW) loadAll() called on every recomposition (deferrable)
- progress_log:
    - owner: reviewer
      stage: focused review of close-dialog, match-history, play-as-dropdown
      date: 2026-03-22
      outcome: changes-requested
    - owner: reviewer
      stage: focused SettingsScreen review
      date: 2026-03-21
      outcome: approved-with-notes
    - owner: coder
      stage: P2 implementation
      date: 2026-03-21
    - owner: orchestrator
      stage: discovery + planning
      date: 2026-03-19
