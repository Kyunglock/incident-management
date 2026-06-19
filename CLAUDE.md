# CLAUDE.md

Guidance for working in this repo. For setup/usage details, see [README.md](README.md) (Korean).

## Overview

Internal tool managing the Edunet operations (SM) hierarchy: **work plan → release history (SR) → incident → incident analysis**.
- Backend: Spring Boot 3.2.5 / Java 17 / Gradle (`backend/`)
- Frontend: Vue 3 (`<script setup>`) + Vite + Tailwind (`frontend/`)
- DB: PostgreSQL (`spring.jpa.hibernate.ddl-auto: update` — adding an entity field auto-creates the column)
- LLM: local Ollama (`ai.llm.url`, default `http://localhost:11434/api/chat`)

Data hierarchy: `ReleasePlan → ReleaseHistory(SR) → Incident → IncidentAnalysis`

## Build / Run / Verify

```bash
# Backend (port 8080)
cd backend && ./gradlew bootRun
./gradlew compileJava -q      # quick compile check (run after edits)
./gradlew build -x test       # full build (no tests — there is no src/test directory)

# Frontend (port 3000, proxies /api → localhost:8080)
cd frontend && npm run dev
npm run build                 # production build + verification
```

- After edits, verify with backend `compileJava` and frontend `npm run build`.
- **Backend code/config changes require a restart**; frontend changes apply on refresh (or dev-server HMR).

## Configuration (important)

- `application.yml` (shared, tracked) + `application-local.yml` (secrets, **gitignored**) + `application-local.yml.example` (template, tracked).
- Default profile is `local`. Put secrets in env vars or directly in `application-local.yml` (env vars are not seen when running from an IDE, so the file is more reliable).

### Git integration (`git.*`)
- `git.provider: local | gitlab` switches the mode. (`config/GitProperties.java`, `git/GitProvider` abstraction → `LocalGitProvider` (CLI) / `GitLabApiProvider` (REST))
- **GitLab tokens are per project**: `git.gitlab.tokens.<projectPath>`. Shared fallback is `git.gitlab.token`.
- **YAML map key caveat**: keys containing `/` (e.g. `root/cnedu-front-edunet`) must be **wrapped in brackets** to bind under Spring's property naming rules → `"[root/cnedu-front-edunet]": ...`. (Korean keys like `에듀넷` bind fine as-is.)
- One "system" = N GitLab projects. Commit lists are fetched per project and merged by date. Commit tokens are stored as `project@hash` (local mode stores just `hash`).
- Commit-listing branch: `git.gitlab.branch`.

## LLM gotchas (hard-won)

- A small local model (e.g. `gemma4:e4b`) returns **empty/garbage on large inputs** — usually due to **context overflow** or **no room left for output tokens**.
- `ai.llm.num-ctx` controls the Ollama context size (default 2048 truncates large diffs; `LlmClient` sends `options.num_ctx`).
- **Cap diff length before sending to the LLM** (side-effect and test-case both cap at ~12,000 chars). If the diff fills the whole context, output comes back as 0 chars.
- LLM output is taken as free text and stored/displayed as-is (do NOT force strict JSON — the small model fails to honor it and the whole call breaks).

## Naming conventions (avoid confusion)

- **UI labels say "작업 계획서"** (renamed from the old "반영 계획서"). But **internal classes/entities/API paths stay English** (`ReleasePlan`, `/api/release-plans`) — do not rename them.
- **"반영 이력" (ReleaseHistory = SR) is unchanged** — only the plan was renamed.

## Code map

| Concern | Location |
|---------|----------|
| Git abstraction/impl | `backend/.../git/` (GitProvider, LocalGitProvider, GitLabApiProvider, GitCommitRef) |
| LLM calls | `common/LlmClient.java` (WebClient; relaxed-TLS bean in `config/WebClientConfig`) |
| Prompts | `common/PromptBuilder.java` (side-effect / vuln / test-case / plan) |
| docx generation | `common/DocxRenderer.java` (`renderWorkPlan` exists but the current UI uses text only) |
| Document metadata | `entity/Document.java` — `type` (e.g. SIDE_EFFECT) + `refType` (RELEASE_PLAN/RELEASE_HISTORY) + `refId` to identify the target; `content` holds the raw LLM text |
| Main frontend screen | `frontend/src/views/ReleasePlanListView.vue` (commit recommend/link, side-effect, test cases, work-content generation — most logic lives here) |
| API client | `frontend/src/services/api.js` |

## Frontend patterns

- Dropdowns/modals that must float above content get clipped by table `overflow`, so render them with **`<Teleport to="body">` + fixed positioning** (the commit-picker panel and result modals all do this).
- Commit recommendation: combined score of work-content similarity (character-bigram cosine) + a worker-match weight; recommended items sort to the top. Tune via the constants at the top of `ReleasePlanListView.vue` (`REC_THRESHOLD`, `WORKER_WEIGHT`).

## Working rules

- Commit/push only when the user asks. Default branch is `main`; do work on a feature branch (`feature/...`).
- LLM/diff changes are hard to verify locally (small model) — confirm the build passes and the length/context guards are in place, and ask the user for the result logs to validate the live behavior.
