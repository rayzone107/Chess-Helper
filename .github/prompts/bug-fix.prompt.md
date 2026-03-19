---
kind: prompt
name: bug-fix
entry_agent: orchestrator
reads:
  - .github/agents/orchestrator.agent.md
  - .github/agents/coder.agent.md
  - .github/agents/reviewer.agent.md
  - .github/agents/tester.agent.md
  - .github/skills/repo-context.skill.md
  - .github/skills/docs-contract.skill.md
inputs:
  - feature_slug
  - bug_report
route:
  - orchestrator -> coder by default
  - orchestrator -> planner if the bug changes scope or architecture
  - orchestrator -> designer if the bug is visual or interaction-heavy
  - coder <-> reviewer until review items are resolved
  - reviewer -> tester
---
Run the bug-fix route as the orchestrator.

- Start with a terse repro, expected result, and suspected file refs in `docs/{feature_slug}/brief.md`.
- Skip planner and designer unless the bug warrants them.
- Drive coder -> reviewer loop -> tester with the narrowest effective scope.

## Feature slug
`<feature-slug>`

## Bug report
<paste bug report here>

