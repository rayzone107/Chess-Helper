---
kind: prompt
name: new-feature
entry_agent: orchestrator
reads:
  - .github/agents/orchestrator.agent.md
  - .github/skills/repo-context.skill.md
  - .github/skills/docs-contract.skill.md
  - .github/skills/product-goal.skill.md
inputs:
  - feature_slug
  - request
route:
  - orchestrator -> planner
  - planner -> designer if UI or interaction is non-trivial
  - planner/designer -> coder
  - coder <-> reviewer until review items are resolved
  - reviewer -> tester
---
Run the new-feature route as the orchestrator.

- Create or update `docs/{feature_slug}/brief.md` and `docs/{feature_slug}/status.md` first.
- Keep every handoff scoped to exact file refs written into the docs.
- Use the lightest path that still captures plan, design, implementation, review, and testing.
- Keep docs terse, version only when needed, and do not duplicate repo context.

## Feature slug
`<feature-slug>`

## Request
<paste request here>

