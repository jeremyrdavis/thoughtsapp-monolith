# ADR-001: Hexagonal monolith

## Status

Accepted

## Context

Thoughtsapp-monolith re-implements the Positive Thoughts microservices demo as a single Quarkus application, built by a team of coding agents. The abstract for the talk commits to "enforcing architectural boundaries with automated tests" and "mutation testing to verify that tests actually detect defects" — the architecture needs boundaries a machine can check, not just a style convention agents are told to follow.

## Decision

Structure the application as a hexagonal (ports and adapters) monolith with one bounded context (`thoughts`) and a single aggregate root (`Thought`), under the base package `io.arrogantprogrammer.thoughts`:

| Layer | Package | May depend on |
| --- | --- | --- |
| Domain | `domain` | JDK only |
| Application | `application` | domain |
| Inbound adapters | `adapters.in.web` (Qute), `adapters.in.rest` (JAX-RS) | application |
| Outbound adapters | `adapters.out.persistence`, `adapters.out.events` | domain ports, application |

Dependencies point inward only. Outbound ports (`ThoughtRepository`, `DomainEventPublisher`) are interfaces owned by `domain`/`application`; adapters implement them. Application services return DTOs, never domain types. An ArchUnit test suite (issue 1) asserts these rules and runs in `./mvnw verify`, so a boundary violation fails the build rather than relying on review.

## Consequences

- The Panache entity is an adapter detail, not the aggregate — persistence code needs an entity-to-aggregate mapper (issue 4).
- `domain` and `application` can never import Panache, Jakarta REST, or CDI event types; this is enforced by both ArchUnit and the definition of done.
- One Maven module is enough — there is no need for module boundaries to enforce the layering since ArchUnit does it at the package level.
- In-process CDI events replace Kafka for `ThoughtCreated` and `ThoughtStatusChanged` (issue 10); there is no external broker.
