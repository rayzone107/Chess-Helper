---
kind: skill
name: docs-contract
purpose: Keep `docs/{feature}` explicit, compact, and cheap for agents to reread.
provides:
  - required doc frontmatter
  - minimal doc set
  - versioning rules
required_frontmatter:
  - feature
  - stage
  - status
  - input_refs
  - code_refs
  - last_updated
---
- Create docs under `docs/{feature}/` only; do not scatter design notes elsewhere.
- Required minimum docs:
  - `brief.md`: normalized request, acceptance bullets, open questions, exact repo refs.
  - `plan.md` or `plan-vN.md`: task breakdown, owners, order, dependencies.
  - `status.md`: current stage, active owner, latest code/docs touched, next step.
- Add only when needed:
  - `design.md`: UI/interaction details or visual decisions.
  - `review-inputs.md`: reviewer issues with `status: open|resolved` per item.
  - `test-plan.md`: only if test scope is non-trivial.
- Prefer links and file paths over pasted code.
- Keep each doc focused: 3-7 bullets per section, no narrative filler.
- Enhancements append versioned plans (`plan-v2.md`, `design-v2.md`) instead of rewriting history.
- Every doc must list exact files/symbols an agent should read next.

