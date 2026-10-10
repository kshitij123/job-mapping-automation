---
name: update-agent-logic-readme
description: >-
  Keeps README.md aligned with the company-mapping agent's current business
  logic after any code change. Use whenever this repo's matching, classify,
  search, LLM, Excel, config, prompts, sample data, thresholds, or graph flow
  change; when editing src/main/java, src/main/resources/prompts, AppConfig,
  CanonicalIndex, NameNormalizer, KeywordClassifier, or CompanyMappingGraph;
  or when finishing a feature/fix in job-mapping-automation.
---

# Update agent logic README

`README.md` at the repo root is the developer business spec. Developers should not need to read Java to know how mapping works. After you change behavior, update `README.md` in the **same change**. Do not wait for the user to ask.

## When this applies

Apply if the change touches any of:

- Classify / “not a company” rules
- Name cleaning, stop words, acronyms, local match order, scores, fuzzy formula
- Accept / LLM thresholds or other env config
- Google query, title extraction, offline search fallback
- LLM prompt, candidate count, how JSON is applied
- Excel columns, statuses, sample data
- Graph step order (classify → match → search → match → llm)

If the change is purely mechanical (rename, formatting, logging) and **no user-visible rule changed**, leave README examples/rules as-is but still bump **Last updated** only if you already edited README for another reason. Skip README when behavior is truly unchanged.

## Workflow

1. Read the current `README.md`.
2. Diff the business behavior (not class names). Ask: would a taxonomy developer get a wrong answer if they only read README?
3. Update README in place. Keep the same section structure and the same plain-language style.
4. Set **Last updated** to today's date.
5. Remove rules that the code no longer follows. Add new rules with the real default values and scores from the code.
6. Refresh worked examples if outcomes would change.

## README must stay a business spec

Write for a developer who will **not** open the source. Prefer:

- numbered steps and decision tables
- current default numbers (`0.82`, `0.93`, stop-word list, env var names)
- concrete keyword examples (`TCS` vs `Tata Consultancy Services`)

Do not turn README into an API tour (class names, LangGraph node IDs, package paths) unless a path is needed to find a prompt or sheet (`src/main/resources/prompts/company-judge.txt`, `data/canonical.xlsx`).

## Required sections (keep these headings)

Preserve this outline unless the product itself changed:

- What the product does
- The three Excel sheets
- Outcomes
- Step-by-step flow
- Classify rules
- Name cleaning / stop words
- Local matching rules (order, scores, acronym index, fuzzy formula, accept vs LLM)
- Search
- LLM judge
- Matching configuration (env + hard-coded)
- Worked examples
- How to run
- Keeping this file true

## Accuracy checks before you finish

- [ ] Every new/changed threshold, status, column, or rule is in README
- [ ] Deleted code paths were removed from README
- [ ] Worked examples still match current rules
- [ ] Env var names and defaults match `AppConfig`
- [ ] Last updated date is today
- [ ] README is still skimmable; no code dump
