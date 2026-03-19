---
kind: prompt
name: planning
entry_agent: orchestrator
reads:
  - .github/agents/orchestrator.agent.md
  - .github/agents/planner.agent.md
  - .github/skills/docs-contract.skill.md
  - .github/skills/product-goal.skill.md
inputs:
  - feature_slug
  - request
route:
  - orchestrator -> planner
---
Run the planning-only route as the orchestrator.

- Normalize the ask into `docs/{feature_slug}/brief.md` first.
- Hand off to the planner to create or update `plan.md` or `plan-vN.md`.
- Optimize for an implementation-ready plan with minimal repo reads and explicit next-reader refs.

## Feature slug
`<feature-slug>`

## Request
<paste request here>

