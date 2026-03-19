---
kind: prompt
name: enhancement
entry_agent: orchestrator
reads:
  - .github/agents/orchestrator.agent.md
  - .github/agents/planner.agent.md
  - .github/agents/designer.agent.md
  - .github/skills/docs-contract.skill.md
  - .github/skills/product-goal.skill.md
inputs:
  - feature_slug
  - request
route:
  - orchestrator -> planner using the latest docs/{feature}
  - planner -> designer if the change alters UI or interaction
  - planner/designer -> coder
  - coder <-> reviewer until review items are resolved
  - reviewer -> tester
---
Run the enhancement route as the orchestrator.

- Read the latest `docs/{feature_slug}` files first.
- Capture the delta in `brief.md`, then create the next versioned plan/design docs as needed.
- Keep implementation, review, and testing scoped to the changed behavior and likely regression path.

## Feature slug
`<feature-slug>`

## Enhancement request
<paste request here>
