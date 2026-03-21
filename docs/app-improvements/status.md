---
feature: app-improvements
stage: planning-complete
status: ready-for-implementation
input_refs:
  - docs/app-improvements/brief.md
  - docs/app-improvements/plan.md
last_updated: 2026-03-19
---
- active_owner: orchestrator → coder
- current_state: >-
    Brief and plan (v1) are complete. No code has been changed.
    14 improvements are enumerated across 3 priority tiers (P0 quick-wins, P1 high-value, P2 stretch).
    Open questions Q1–Q7 in brief.md require product decisions before the affected tasks can begin
    (Q5 piece-set licence is the only P1 blocker; all P0 tasks are unblocked).
- next_reader: coder
- next_step: >-
    Implement P0 tasks in order T2 → T3 → T1 per plan.md.
    No repo-wide scans needed — file refs are explicit in the plan.
- touched_docs:
    - docs/app-improvements/brief.md
    - docs/app-improvements/plan.md
    - docs/app-improvements/status.md
- touched_code: []
- blockers:
    - "Q5 (piece-set licence) must be resolved before T6 (vector pieces) can begin"
    - "Q2 (opening book source) must be resolved before T5 (opening name) can begin"
    - "Q7 (PGN import scope) must be resolved before T11 (FEN/PGN import) is scheduled"
- open_questions:
    - id: Q1
      topic: Move notation format (SAN vs long-algebraic)
      affects: T1
      status: open
    - id: Q2
      topic: Opening book data source (Lichess CC0 vs hand-curated)
      affects: T5
      status: open — proposed answer is Lichess chess-openings (CC0)
    - id: Q3
      topic: Evaluation bar orientation (vertical vs horizontal)
      affects: T4
      status: open — plan proposes vertical strip beside board
    - id: Q4
      topic: Multi-PV default count (1 vs 3)
      affects: T7
      status: open — plan defaults to 1 (unchanged today)
    - id: Q5
      topic: Piece-set licence (Wikimedia CC-BY-SA vs other)
      affects: T6
      status: open — BLOCKS T6
    - id: Q6
      topic: Sound effects default (opt-in vs opt-out)
      affects: T13
      status: open — plan proposes opt-in (default false)
    - id: Q7
      topic: PGN import scope in initial pass
      affects: T11
      status: open — plan defers PGN to follow-up
- progress_log:
    - owner: orchestrator
      stage: discovery + planning
      date: 2026-03-19
      touched_docs:
        - docs/app-improvements/brief.md
        - docs/app-improvements/plan.md
        - docs/app-improvements/status.md
      touched_code: []
      blockers:
        - Q5 piece-set licence (T6)
        - Q2 opening book source (T5)
      next_step: implement T2 → T3 → T1 (P0 tier, all unblocked)

