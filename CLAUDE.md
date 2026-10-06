# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

Issues 0–7 are done (skeleton, ArchUnit, PIT/JaCoCo gates, the `Thought` aggregate, the Panache persistence adapter, the random-thought page, voting, the JSON REST API — see `git log --oneline` for one commit per issue). Issues 8–13 (admin UI, in-process domain events, health/metrics, the dev-mode runbook doc, the Dockerfile) are not started. Treat `spec.md` as the source of truth for anything below, especially the acceptance criteria for issues 8–13. `AGENTS.md` mirrors this file; keep the two in sync.

The target repo is `jeremyrdavis/thoughtsapp-monolith`. The app is a prop for the "Meet the Agentic Team" talk: a modular-monolith Quarkus re-implementation of the Positive Thoughts microservices demo (github.com/jeremyrdavis/thoughtsapp).

## Commands

- `./mvnw verify` is the single gate. It runs unit tests, `@QuarkusTest` integration tests, ArchUnit, PIT mutation testing (threshold 80%, `domain`/`application` only) and the JaCoCo report. CI (`.github/workflows/ci.yml`) and the review agent run the same command, so an issue is done only when this passes.
- `./mvnw quarkus:dev` runs the app. Quarkus Dev Services starts PostgreSQL 17 automatically in dev and test. A container runtime (Docker or Podman) must be available. There is no Docker Compose file, and `application.properties` currently has no datasource config — don't add one by hand; Dev Services supplies it.
- `./mvnw test -Dtest=ClassName#method` runs a single test.
- No Dockerfile exists yet (issue 13). Once it does: `docker build` packages the app; the container needs PostgreSQL 17 passed in through `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME` and `QUARKUS_DATASOURCE_PASSWORD`, since Dev Services doesn't run in the packaged app.
- `./scripts/test-in-sandbox.sh` runs the full `./mvnw verify` gate inside a pinned JDK 25 + Maven container via the local Docker daemon — no local JDK 25 needed. Extra arguments pass through to Maven before `verify`, same convention as `-Dtest=...`. This is also what the AI PR reviewer (below) runs.
- `./tests/run.sh` runs the PR-reviewer workflow's own script tests (no sbx, Docker or network needed), or one file, e.g. `bash tests/resolve-pr.test.sh`.

## Stack

Quarkus 3.31.x on Java 25, Maven, PostgreSQL 17, Hibernate ORM with Panache, Qute + htmx (server-rendered, no SPA), SmallRye Health, Micrometer. Generate the REST client contract from the OpenAPI document instead of writing it by hand.

## Architecture: hexagonal, enforced by ArchUnit

Base package: `io.arrogantprogrammer.thoughts`. Dependencies point inward only:

| Layer | Package | May depend on |
| --- | --- | --- |
| Domain | `domain` | JDK only |
| Application | `application` | domain |
| Inbound adapters | `adapters.in.web` (Qute), `adapters.in.rest` (JAX-RS) | application |
| Outbound adapters | `adapters.out.persistence`, `adapters.out.events` | domain ports, application |

Rules checked by ArchUnit tests in `src/test/java/.../architecture/LayeredArchitectureTest.java`:
- The layer dependency rules above hold, and no `adapters.*` type is referenced from `domain` or `application`.
- Application service methods return DTOs, never domain types.

Rules enforced by the definition of done and the review agent, not (yet) by a test:
- `domain` and `application` never import Panache, Jakarta REST or CDI event types.
- Ports (`ThoughtRepository`, `DomainEventPublisher`) are interfaces in domain or application, and adapters implement them.
- The Panache entity (`ThoughtEntity`) is an adapter detail, not the aggregate — `ThoughtMapper` converts between it and `Thought`.

`adapters.out.events` currently holds only a `package-info.java`; the CDI event publisher (issue 10) hasn't been written, so nothing publishes `ThoughtCreated` or `ThoughtStatusChanged` yet even though the aggregate can raise them.

## Domain model

One bounded context with a single aggregate root, `Thought` (`domain/Thought.java`):
- `Content` is trimmed, non-blank and 10–500 chars. `Author` has a name ≤ 200 and an optional bio ≤ 200. `Rating` holds up/down counters ≥ 0, and `approvalRate()` returns 0 when there are no votes.
- `ThoughtStatus` is `IN_REVIEW` (initial), `APPROVED` or `REMOVED`. Only `APPROVED` thoughts are shown to the public.
- New thoughts stay `IN_REVIEW` until an admin approves them. There is no AI evaluation.
- Allowed transitions are IN_REVIEW→APPROVED, IN_REVIEW→REMOVED, APPROVED→REMOVED, REMOVED→IN_REVIEW (restore) and APPROVED→IN_REVIEW (send back). Illegal transitions raise `IllegalThoughtStatusTransitionException`, surfaced as HTTP 409 (mapped via `IllegalArgumentExceptionMapper`).
- The aggregate raises the domain events `ThoughtCreated` and `ThoughtStatusChanged`, but nothing publishes them yet (see above).

Hibernate ORM generates the schema (a single `thoughts` table) from `ThoughtEntity` with `drop-and-create` in dev, test and the container. There is no Flyway. `import.sql` loads the `quotes.json` quotes as `APPROVED` on every start, so data does not survive a restart. Don't configure a datasource URL or credentials in `application.properties`. Dev Services provides them in dev and test, and the container gets them from environment variables.

## Current endpoints

- `GET /` — Qute page showing one random `APPROVED` thought (`HomeResource`).
- `GET /thoughts/random`, `POST /thoughts/{id}/thumbs-up`, `POST /thoughts/{id}/thumbs-down` — htmx partials, return the `card` template fragment.
- `GET/POST/PUT/DELETE /api/thoughts[...]` — JSON API (`ThoughtsRestResource`): list (paged, default size 20), get, random, create, update, delete, thumbs-up/down.
- No admin routes yet (issue 8–9) and no health/metrics endpoints beyond what `quarkus-smallrye-health`/`quarkus-micrometer` provide out of the box (issue 11).

## Testing conventions

- PIT and the mutation threshold cover `domain` and `application` only. Adapters are tested with `@QuarkusTest` integration tests against the Dev Services database. Don't add Testcontainers directly.
- JaCoCo coverage is a floor, not a target.
- `VotingConcurrencyTest` exercises the concurrent-votes acceptance criterion (issue 6) with parallel requests — don't weaken it when touching voting.

## `.claude/skills/`

DDD and Quarkus skills (`ddd-aggregates`, `ddd-foundations`, `ddd-persistence`, `ddd-services`, `ddd-value-objects`, `quarkus-logging`, `quarkus-persistence`, `quarkus-testing`) are available and should trigger automatically on matching work (new aggregates/value objects/repositories/services, logging, Panache code, tests). They encode the conventions this project expects beyond what ArchUnit checks mechanically.

## Definition of done (every PR)

- `./mvnw verify` passes.
- At least one test names each acceptance criterion in the issue.
- The PR body links the issue, lists the tests added and states the mutation score.

## AI PR review

`.github/workflows/pr-review.yml` reviews every pull request with a Copilot agent running inside a Docker Sandbox (`sbx`) microVM, and posts findings as one sticky comment. Ported from the sibling `pr-reviewer` demo; `README.md` has the full setup and threat model. Rules that protect the security model:

- **Never execute anything from the pull request on the runner.** It is checked out to `./pr` and only passed to `sbx create --clone`. Scripts, prompt and actions come from `./trusted` (the base branch). Don't add a step that runs a file from `./pr`.
- **Never put pull request text (title, body, branch names, labels) inside a `run:` script or an `${{ }}` that ends up in one.** Pass it through `env:`.
- The agent job keeps `contents: read` only. The comment job runs no agent and no sandbox.
- The agent's output always goes through `sanitize.sh` before it is posted. Only comments from `github-actions[bot]` that start with the marker are ever edited.
- Third-party actions are pinned by commit SHA resolved with `git ls-remote`; never invent a SHA.
- The workflow triggers on `pull_request_target`, so it only takes effect from the default branch (`main`).

The reviewer's test command is `./scripts/test-in-sandbox.sh` (the full `./mvnw verify` gate), so a review checks this project's actual definition of done, not just a quick smoke test.

## Work backlog

Issues 0–13 in `spec.md` map one-to-one to GitHub issues, with acceptance criteria copied verbatim. Remaining, in order: 8 (admin list/create), 9 (admin edit/moderate/delete), 10 (in-process domain events), 11 (health and metrics), 12 (dev-mode runbook doc), 13 (Dockerfile). Out of scope throughout: AI evaluation (embeddings, pgvector, Langchain4j, Ollama), Flyway, Testcontainers, Docker Compose, deployment manifests, Kafka or any broker, a separate evaluation service, SPA frontends, OpenShift manifests, native image, and user accounts beyond basic auth for admin.
