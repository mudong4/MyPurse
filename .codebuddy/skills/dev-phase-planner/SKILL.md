---
name: dev-phase-planner
description: >
  This skill analyzes requirements documents and produces a structured development phase plan.
  It should be used when an agent needs to: (1) design a development phase plan from a
  requirements document, (2) review and critique an existing phase plan, (3) break down a
  requirements document into implementable, dependency-ordered stages. Triggers include
  user requests like "设计开发阶段", "分析需求文档规划阶段", "这个阶段划分合理吗",
  "根据需求文档拆分开发任务", or when starting a new project and needing a phased roadmap.
---

# Development Phase Planner

## Overview

Analyze a requirements document (or a set of requirements) and produce a structured, dependency-ordered development phase plan. The output follows a fixed template (see `references/phase-template.md`) and incorporates lessons from real project experience (see `references/lessons-learned.md`).

## When to Use

- User asks to design development phases from a requirements document
- User asks to review or critique an existing phase plan
- User asks to break down a requirements document into implementable stages
- Starting a new project and needing a phased development roadmap
- Evaluating whether an existing phase plan is reasonable

## Workflow

### Step 1: Read Requirements

Read the requirements document thoroughly. If the project has a `docs/` directory, first read `docs/README.md` for the document index, then also read:
- `1-项目概览/项目介绍.md` — project architecture and key decisions
- `4-规范与指南/通用开发规范.md` — universal development standards (if exists)
- `4-规范与指南/开发规范.md` — project-specific coding standards (if exists)

Identify:
- All functional modules and their descriptions
- Dependencies between modules (e.g., Module B needs Module A's data layer)
- Non-functional requirements (performance, security, UX)
- Platform/technology constraints

### Step 2: Load References

Read the bundled reference files for methodology and lessons:

1. `references/lessons-learned.md` — anti-patterns and lessons from real projects
2. `references/phase-template.md` — the exact output format to follow

### Step 3: Decompose Features

Extract every functional unit from the requirements. For each unit, determine:

| Attribute | How to Determine |
|-----------|-----------------|
| Dependencies | What must exist before this can be built? (data layer? another feature?) |
| Complexity | Low (simple UI or single DAO), Medium (full screen + ViewModel), High (multi-screen + complex logic) |
| Priority | P0 (core value proposition), P1 (important but not blocking), P2 (nice to have) |

### Step 4: Group into Phases

Apply these grouping rules (from `references/lessons-learned.md`):

1. **Phase 0 is always project initialization**: build system, DI framework, database, navigation shell, theme
2. **Phase 1 is always data layer foundation**: all entities, DAOs, repositories, domain models — before any UI
3. **Group by capability, not by page**: one phase = one independently verifiable capability. A phase that contains "home page + add transaction + list" is too coarse.
4. **The last phase must NOT be a grab-bag**: "polish + performance + testing + edge cases" is an anti-pattern. Split it into 2-3 sub-phases, each with its own number and acceptance criteria.
5. **Include testing in every phase**: after the prototype phase, each phase must include unit tests for core DAOs and UseCases as a sub-task.
6. **Database schema changes get their own migration sub-task**: never assume `fallbackToDestructiveMigration()` is safe.

### Step 5: Validate Phase Plan

Before output, check:

- [ ] Every requirement section is covered by at least one phase
- [ ] Dependencies are correct (no phase depends on a later phase)
- [ ] No phase is a "grab-bag" — each has a clear theme
- [ ] Each phase has specific, verifiable acceptance criteria
- [ ] Testing strategy is explicitly stated (prototype vs production)
- [ ] The plan accounts for the project's maturity (prototype → feature dev → maintenance)

### Step 6: Output

Output exactly in the format defined by `references/phase-template.md`:

1. Feature decomposition table with dependency graph
2. Phase overview table
3. Detailed per-phase breakdown (tasks, files, tests, checklist)
4. Risk assessment
5. Requirements coverage cross-reference

## Critical Rules

- **Never create a "Phase N: Miscellaneous" phase.** Split it into numbered sub-phases.
- **Acceptance criteria must be verifiable.** Not "UI complete" but "home page displays balance with positive/negative color, today/week/month/year summaries, budget progress bar".
- **Distinguish prototype from production phases.** Prototype phases can be coarser and skip tests; production phases must be fine-grained with mandatory tests.
- **Database schema changes are never "trivial".** Always include Migration as an explicit sub-task in the relevant phase.

## Interaction with Project Documentation

After outputting the phase plan, remind the user to:
1. Save it as `3-开发过程/开发阶段.md` (or equivalent)
2. Include it in the project's document index (`docs/README.md`)
3. Follow `4-规范与指南/通用开发规范.md` Chapter 0 for agent handoff procedures
