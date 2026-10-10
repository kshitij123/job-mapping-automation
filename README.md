# Company mapping agent — business logic

This is the developer reference for **how the agent decides**, not how the Java is structured. If a rule, threshold, sheet column, or outcome changes in code, this file must change in the same change.

**Last updated:** 2026-10-10 (start/stop lifecycle)

---

## What the product does

Job seekers type keywords into a search box (monopoly.com / Naukri-style dump). Those keywords are messy: real company names, abbreviations, extra words like “jobs”, or junk such as emails and phone numbers.

The agent takes each dump keyword and tries to map it to **one** company in the canonical sheet (the unique, official list). It prefers cheap local matching. It searches Google only when local matching is not confident. It calls an LLM only when matching is still uncertain after search.

---

## The three Excel sheets

### 1. Canonical sheet (source of truth)

Default path: `data/canonical.xlsx` (override with `CANONICAL_FILE`).

| Column | Meaning |
|---|---|
| `canonical_id` | Stable unique ID for one company (sample uses `C1001`, `C1002`, …) |
| `canonical_name` | The one official name we map to. A company appears **once**. The name may be a full name (`Infosys Limited`) or a short form (`TCS`). |

Header row is skipped. Empty id/name rows are ignored. First sheet only.

### 2. Dump sheet (keyword search log)

Default path: `data/dump.xlsx` (override with `DUMP_FILE`).

| Column | Meaning |
|---|---|
| `company_id` | System-generated ID for that search keyword (sample uses 5-digit IDs like `67435`) |
| `company_name` | **Exactly what the user typed**, not a cleaned company name |

If these files are missing on first run, the app writes a small sample canonical + dump so you can try the agent.

### 3. Mapping sheet (agent output)

Default path: `data/mapping.xlsx` (override with `MAPPING_FILE`). One row per dump keyword.

| Column | Meaning |
|---|---|
| `dump_company_id` | From dump |
| `dump_company_name` | From dump (raw keyword) |
| `canonical_id` / `canonical_name` | Filled only when status is `MAPPED` |
| `match_status` | See outcomes below |
| `match_score` | 0.0–1.0 confidence from local matching (LLM maps are stored as `0.9`) |
| `resolved_company_name` | Company name taken from the first Google/search title, if search ran |
| `used_llm` | `true` if the LLM judge ran |
| `used_google` | `true` if the search step ran (live Google **or** offline fallback) |
| `reason` | Short explanation of which rule fired |

---

## Outcomes

| Status | Meaning |
|---|---|
| `MAPPED` | Keyword is treated as a company and linked to one canonical row |
| `NOT_A_COMPANY` | Keyword is not a company (email, phone, too short, no letters, or LLM said it is not a company) |
| `UNMATCHED` | It might be a company, but we could not confidently pick a canonical row |

---

## Step-by-step flow (one dump keyword)

The agent processes **one keyword at a time**.

```
Keyword
  → 1. Classify (is this even a company-like string?)
       no  → NOT_A_COMPANY   (stop; no Google, no LLM)
       yes → 2. Local match against canonical
               score ≥ accept threshold → MAPPED   (stop)
               else → 3. Search (Google if configured, else offline demo)
                      → 4. Local match again, now also using the search title
                            score ≥ accept threshold → MAPPED   (stop)
                            still weak, LLM is on, score ≥ LLM threshold → 5. LLM judge
                            otherwise → UNMATCHED   (stop)
```

Search never runs for emails/phones. LLM never runs if local matching already accepted a hit, or if `OPENAI_API_KEY` is missing.

---

## Step 1 — Classify (not a company?)

A keyword is **rejected immediately** as `NOT_A_COMPANY` if any of these is true:

- null / blank
- shorter than 2 characters after trim
- looks like an email (`name@domain.tld`)
- looks like a phone: starts with `+` or a digit, then digits/spaces/`()`/`.`/`-`, and has **at least 8 digits**
- contains **no letters at all** (so a pure number that did not match the phone pattern still fails)

Anything that still has letters (including junk like `xyzabc123 not a firm`) continues to matching. Junk is not classified as “not a company” here; it usually ends as `UNMATCHED` after search finds nothing useful.

---

## Step 2 — How names are cleaned before matching

All comparisons use cleaned text:

1. Trim, lowercase
2. Replace every non-letter/non-digit with a space
3. Collapse to a **normalized** string (`infosys limited`) and a **compact** string with spaces removed (`infosyslimited`)

**Stop words** (dropped when building tokens and acronyms):

`ltd`, `limited`, `pvt`, `private`, `inc`, `incorporated`, `llc`, `corp`, `corporation`, `co`, `company`, `the`, `and`, `of`, `india`, `jobs`, `job`, `careers`

Words such as `consultancy`, `services`, `technologies` are **kept**, so `Tata Consultancy Services` can become acronym `tcs`.

---

## Step 3 — Local matching rules (exact order)

The matcher scores the dump keyword, and after search it also scores the **resolved name** from the search title. The **higher** of those two scores wins.

It stops at the **first** rule that hits:

| Order | Rule | Score | Example |
|---|---|---|---|
| 1 | **Exact** — normalized dump name equals a canonical name | `1.00` | `TCS` → canonical `TCS` |
| 2 | **Compact** — compact dump name equals compact canonical | `0.97` | `TCS` vs `t c s` if that ever appeared |
| 3 | **Acronym index** — compact dump name is in the acronym index | `0.93` | `tcs` → canonical `TCS`; `hdfc` → `HDFC Bank`; `hcl` → `HCL Technologies` |
| 4 | **Token-acronym** — any significant token of the dump hits the acronym index | `0.91` | `tcs jobs` → `TCS`; `IBM India` → `IBM` (`india` is a stop word) |
| 5 | **Generated acronym** — first letters of significant dump tokens match a canonical compact/normalized/acronym form | `0.93` | `Tata Consultancy Services` → `tcs` → canonical `TCS` |
| 6 | **Fuzzy** — best of token Jaccard vs compact Levenshtein, plus an acronym boost | `0.00–1.00` | `Infosys` vs `Infosys Limited` (after dropping `limited`, tokens match) |

### How the acronym index is built (from canonical names)

For each canonical company, the index stores (first write wins if two companies collide):

- generated acronym if it has at least 2 letters (e.g. `reliance industries` → `ri`)
- compact name if it is 2–5 characters (e.g. `TCS` → `tcs`, `IBM` → `ibm`)
- first significant token if that token is 2–5 characters (e.g. `HCL Technologies` → `hcl`, `HDFC Bank` → `hdfc`)

### Fuzzy score (only if nothing above matched)

```
score = min(1.0, max(token_jaccard, compact_levenshtein_ratio) + acronym_boost)
```

- **Token Jaccard:** overlap of significant tokens (stop words removed)
- **Compact Levenshtein ratio:** `1 - edit_distance / longer_length` on compact strings
- **Acronym boost `+0.25`** (capped at 1.0) if:
  - dump acronym equals canonical compact or canonical acronym, or
  - canonical acronym equals dump compact

### Accept vs “ask LLM” vs unmatched

After a best score is known:

| Condition | What happens |
|---|---|
| score **≥ `MATCH_ACCEPT_SCORE`** (default **0.82**) | `MAPPED` immediately |
| search has **not** run yet | go to search, then match again |
| search **has** run, score **≥ `MATCH_LLM_SCORE`** (default **0.50**), and local LLM is enabled and reachable | go to LLM |
| otherwise | `UNMATCHED` |

All of exact (`1.00`), compact (`0.97`), acronym (`0.93`), and token-acronym (`0.91`) sit **above** the default accept threshold, so they map without Google or LLM.

---

## Step 4 — Search (Google, or offline fallback)

Runs only when local matching did not accept.

**Live Google** when both are set:

- `GOOGLE_API_KEY` (or `GOOGLE_CSE_API_KEY`)
- `GOOGLE_CSE_ID` (or `GOOGLE_SEARCH_ENGINE_ID`)

Query sent: `{keyword} company official`

**Otherwise** a small offline dictionary is used (demo only). Unknown keywords return no hits.

From search results:

- `resolved_company_name` = first result **title**, cut at the first ` - `, ` – `, ` \| `, or `:`
- snippets are kept as evidence for the LLM

Then matching runs again using **dump keyword and resolved name**. Example: dump `Reliance` may not exact-match `Reliance Industries`, search title becomes `Reliance Industries`, then exact match maps it.

---

## Step 5 — LLM judge (last resort, local MLX — no paid API)

Runs only when:

- search already ran
- best local score is still below accept, but **≥ `MATCH_LLM_SCORE`**
- `LOCAL_LLM_ENABLED` is `true` (default)
- the local OpenAI-compatible server answers at `LOCAL_LLM_BASE_URL` (health check: `GET …/v1/models`)

**Default stack on Mac (Apple Silicon):** [MLX](https://github.com/ml-explore/mlx) via `mlx-lm`, model **`mlx-community/Qwen2.5-3B-Instruct-4bit`**. Chosen for this POC because the judge only emits a tiny JSON object (`isCompany` + optional `canonicalId`). A 3B instruct 4-bit model is fast on 24 GB, follows JSON instructions well, and does not spend tokens on “thinking” (unlike Qwen3 8B). Bigger models (`Qwen3-8B-4bit`, `Qwen3.5-9B-MLX-4bit`) are optional via `MLX_MODEL` if you later want more recall at the cost of speed.

The agent owns the local LLM lifecycle. Start installs MLX + downloads the model if this machine does not have them yet, then serves `http://127.0.0.1:8080/v1`. Stop (or JVM exit) kills the mapping process **and** `mlx_lm.server`. Do not start the LLM by hand.

Endpoint: OpenAI-compatible chat over HTTP/1.1. No cloud API key.

Inference settings: temperature `0`, max **256** output tokens, timeout **120s** per judge call.

The model sees: the raw keyword, search evidence, and the **top 5** fuzzy canonical candidates. Prompt file: `src/main/resources/prompts/company-judge.txt`.

LLM rules:

- say whether the keyword is a real company
- if yes, pick **one `canonicalId` from the candidate list only** (e.g. TCS vs Tata Consultancy Services is allowed)
- do not invent a company that is not in the list
- JSON only. The prompt’s format example uses an empty `canonicalId` so a small local model does not copy `C1001` as the answer. Real shape: `{"isCompany":true,"canonicalId":"C1001","reason":"..."}`

How the agent uses that JSON:

| LLM result | Outcome |
|---|---|
| `isCompany` is false | `NOT_A_COMPANY` |
| `canonicalId` exists in the canonical sheet | `MAPPED` (score stored as `0.9`) |
| `canonicalId` missing or unknown | `UNMATCHED` |

If matching asks for LLM but the server is not running (or `LOCAL_LLM_ENABLED=false`), status becomes `UNMATCHED` with reason `llm requested but not configured`.

---

## Matching configuration

All of these are **environment variables**. Defaults are what the agent uses today.

| Variable | Default | Role |
|---|---|---|
| `MATCH_ACCEPT_SCORE` | `0.82` | Map without LLM if local score is at least this |
| `MATCH_LLM_SCORE` | `0.50` | After search, call LLM if score is at least this and below accept |
| `CANONICAL_FILE` | `data/canonical.xlsx` | Canonical input |
| `DUMP_FILE` | `data/dump.xlsx` | Dump input |
| `MAPPING_FILE` | `data/mapping.xlsx` | Mapping output |
| `GOOGLE_API_KEY` / `GOOGLE_CSE_API_KEY` | unset | Enables live Google |
| `GOOGLE_CSE_ID` / `GOOGLE_SEARCH_ENGINE_ID` | unset | Google Custom Search engine id |
| `LOCAL_LLM_ENABLED` | `true` | Turn off LLM judge entirely when `false` |
| `LOCAL_LLM_BASE_URL` | `http://127.0.0.1:8080/v1` | MLX / OpenAI-compatible chat API |
| `LOCAL_LLM_MODEL` | `mlx-community/Qwen2.5-3B-Instruct-4bit` | Model id sent in chat requests (must match `mlx_lm.server --model`) |
| `MLX_MODEL` | — | Alias for `LOCAL_LLM_MODEL` (used by `scripts/start-mlx-llm.sh`) |
| `MLX_PORT` / `MLX_HOST` | `8080` / `127.0.0.1` | Only for the shell script, not read by Java |
| `LOCAL_LLM_API_KEY` | `mlx-local` | Placeholder; MLX server does not require a real key |

**Hard-coded (not env) today** — change code *and* this file if you change them:

- Email / phone / length / “must contain a letter” classify rules
- Stop-word list
- Match method scores: exact `1.00`, compact `0.97`, acronym `0.93`, token-acronym `0.91`
- Acronym index: compact/first-token length 2–5, generated acronym length ≥ 2
- Fuzzy acronym boost `0.25`
- Google query suffix ` company official`
- Title split on ` - ` / ` – ` / ` \| ` / `:`
- LLM: top 5 candidates, mapped score `0.9`, prompt file, temperature `0`, max 256 tokens, HTTP/1.1, strip `<think>` if present
- Sample IDs and names if you regenerate sample Excel

---

## Worked examples (current rules)

| Dump keyword | Result | Why |
|---|---|---|
| `Tata Consultancy Services` | `MAPPED` → `TCS` | Generated acronym `tcs` |
| `TCS` | `MAPPED` → `TCS` | Exact |
| `tcs jobs` | `MAPPED` → `TCS` | Token `tcs` (`jobs` dropped) |
| `Infosys` / `infosys ltd` | `MAPPED` → `Infosys Limited` | Fuzzy after stop words |
| `9876543210`, `john.doe@gmail.com`, `+91 9988776655` | `NOT_A_COMPANY` | Classify; no search, no LLM |
| `Reliance` | `MAPPED` → `Reliance Industries` | Search title, then exact |
| `xyzabc123 not a firm` | `UNMATCHED` | Looks like text, search has no evidence, score too low |
| `HCL` | `MAPPED` → `HCL Technologies` | Acronym index from first token |

---

## How to run the whole system

Need on this Mac: **Java 21**, **Maven**, **Python 3**. Apple Silicon (MLX).

### Start (agent + local LLM)

From the repo root:

```bash
chmod +x scripts/start-agent.sh scripts/stop-agent.sh scripts/start-mlx-llm.sh
./scripts/start-agent.sh
```

Equivalent (same lifecycle — Java starts the LLM, then stops it when the JVM exits):

```bash
mvn exec:java
```

**First run on a machine** (automatic, no extra commands):

1. Create `.venv-mlx` and `pip install mlx-lm` if missing.
2. Download **Qwen2.5-3B-Instruct-4bit** from Hugging Face if the weights are not cached.
3. Start `mlx_lm.server` on `127.0.0.1:8080`.
4. If sample Excel files are missing, write `data/canonical.xlsx` and `data/dump.xlsx`.
5. Map every dump keyword → write `data/mapping.xlsx`.
6. When the job finishes (or you interrupt it), shut down the LLM as well.

### Stop (agent + local LLM)

```bash
./scripts/stop-agent.sh
```

That kills:

- the mapping Java/Maven process
- `mlx_lm.server` (and anything listening on port **8080**)
- pid files under `.run/`

Use this if a previous run was interrupted, or if you still see the model occupying RAM after the mapping job.

Ctrl+C during `./scripts/start-agent.sh` also runs the same shutdown.

### Optional

```bash
mvn test
```

`LOCAL_LLM_ENABLED=false mvn exec:java` skips starting/using the LLM (uncertain rows stay `UNMATCHED`). Google Custom Search is optional; without keys the agent uses the offline search fallback.

---

## Keeping this file true

Treat this README as the **business spec**. After any change to matching, classify rules, search, LLM, Excel columns, env vars, prompts, or sample data, update the sections above in the same change. Do not leave developers to reverse-engineer the code.
