# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

This directory contains only `spec.md`, the feature spec for **thoughtsapp-monolith**. No code exists yet. The target repo is `jeremyrdavis/thoughtsapp-monolith`. The app is a prop for the "Meet the Agentic Team" talk: a modular-monolith Quarkus re-implementation of the Positive Thoughts microservices demo (github.com/jeremyrdavis/thoughtsapp). Treat `spec.md` as the source of truth. Update this file once the Quarkus skeleton (issue 0) lands.

## Commands (once the skeleton exists)

- `./mvnw verify` is the single gate. It runs unit tests, `@QuarkusTest` integration tests, ArchUnit, PIT mutation testing (threshold starts at 80%) and the JaCoCo report. CI and the review agent run the same command, so an issue is done only when this passes.
- `./mvnw quarkus:dev` runs the app. Quarkus Dev Services start PostgreSQL 17 automatically in dev and test. A container runtime (Docker or Podman) must be available. There is no Docker Compose file.
- `docker build` uses the multi-stage `Dockerfile` to package the app. Dev Services does not run in the packaged app, so the container needs a PostgreSQL 17 database passed in through `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME` and `QUARKUS_DATASOURCE_PASSWORD`.
- `./mvnw test -Dtest=ClassName#method` runs a single test.

## Stack

Quarkus 3.31.x on Java 25, Maven, PostgreSQL 17, Hibernate ORM with Panache, Qute + htmx (server-rendered, no SPA), SmallRye Health, Micrometer. Generate the REST client contract from the OpenAPI document instead of writing it by hand.

## Architecture: hexagonal, enforced by ArchUnit

Base package: `io.arrogantprogrammer.thoughts` (the groupId is still an open question in the spec). Dependencies point inward only:

| Layer | Package | May depend on |
| --- | --- | --- |
| Domain | `domain` | JDK only |
| Application | `application` | domain |
| Inbound adapters | `adapters.in.web` (Qute), `adapters.in.rest` (JAX-RS) | application |
| Outbound adapters | `adapters.out.persistence`, `adapters.out.events` | domain ports, application |

Rules checked by ArchUnit tests in `src/test/java/.../architecture`:
- The layer dependency rules above hold, and no `adapters.*` type is referenced from `domain` or `application`.
- Application service methods return DTOs, never domain types.

Rules enforced by the definition of done and the review agent:
- `domain` and `application` never import Panache, Jakarta REST or CDI event types.
- Ports (`ThoughtRepository`, `DomainEventPublisher`) are interfaces in domain or application, and adapters implement them.
- The Panache entity is an adapter detail. It is not the aggregate, so the code needs an entity-to-aggregate mapper.

## Domain model

One bounded context with a single aggregate root, `Thought`:
- `Content` is trimmed, non-blank and 10–500 chars. `Author` has a name ≤ 200 and an optional bio ≤ 200. `Rating` holds up/down counters ≥ 0, and `approvalRate()` returns 0 when there are no votes.
- `ThoughtStatus` is `IN_REVIEW` (initial), `APPROVED` or `REMOVED`. Only `APPROVED` thoughts are shown to the public.
- New thoughts stay `IN_REVIEW` until an admin approves them. There is no AI evaluation.
- Allowed transitions are IN_REVIEW→APPROVED, IN_REVIEW→REMOVED, APPROVED→REMOVED, REMOVED→IN_REVIEW (restore) and APPROVED→IN_REVIEW (send back). Illegal transitions surface as HTTP 409.
- The aggregate raises the domain events `ThoughtCreated` and `ThoughtStatusChanged`. They are published after commit through `DomainEventPublisher` (a CDI adapter; there is no external broker).

Hibernate ORM generates the schema (a single `thoughts` table) from the Panache entity with `drop-and-create` in dev, test and the container. There is no Flyway. `import.sql` loads the `quotes.json` quotes as `APPROVED` on every start, so data does not survive a restart. Don't configure a datasource URL or credentials in `application.properties`. Dev Services provides them in dev and test, and the container gets them from environment variables.

## Testing conventions

- PIT and the mutation threshold cover `domain` and `application` only. Adapters are tested with `@QuarkusTest` integration tests against the Dev Services database. Don't add Testcontainers directly.
- JaCoCo coverage is a floor, not a target.

## Definition of done (every PR)

- `./mvnw verify` passes.
- At least one test names each acceptance criterion in the issue.
- The PR body links the issue, lists the tests added and states the mutation score.

## Work backlog

Issues 0–13 in `spec.md` map one-to-one to GitHub issues, with acceptance criteria copied verbatim. Issues 0–2 (skeleton, ArchUnit, PIT/JaCoCo gates) are committed by hand. Agents pick up issues 3 onward, in order. Out of scope: AI evaluation (embeddings, pgvector, Langchain4j, Ollama), Flyway, Testcontainers, Docker Compose, deployment manifests, Kafka or any broker, a separate evaluation service, SPA frontends, OpenShift manifests, native image, and user accounts beyond basic auth for admin.
