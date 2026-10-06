# JaCoCo: talking points

Source: `pom.xml` (`jacoco-maven-plugin`, lines ~149–168), compared against the PIT and ArchUnit wiring.

## What it is

- `jacoco-maven-plugin`, two executions: `prepare-agent` (attaches the coverage javaagent before tests run) and `report`, bound to the `verify` phase. Added in the same commit as PIT (`66e6ff0`, issue 2) — coverage and mutation testing were introduced as one unit of work, not two separate efforts.
- Critically: there's **no `check` goal/execution**. JaCoCo only generates `target/site/jacoco` — it never fails the build on a coverage number. That's the literal mechanism behind the `CLAUDE.md`/`AGENTS.md` line "JaCoCo coverage is a floor, not a target": there's nothing in the pom that *could* enforce a floor even if the team wanted to. The only enforced gate in this build is PIT's 80% mutation threshold.

## Scope is the opposite of PIT's, and that's worth spelling out on stage

- PIT's `targetClasses`/`targetTests` are explicitly restricted to `domain.*` and `application.*`. JaCoCo has no such restriction — it instruments the agent globally via `@{argLine}` shared by both `maven-surefire-plugin` and `maven-failsafe-plugin`, so its report covers the whole codebase: domain, application, *and* the adapters (REST, web, persistence).
- Net effect: JaCoCo and PIT are deliberately answering different questions. JaCoCo tells you "how much of the whole app did any test touch" (line coverage, descriptive); PIT tells you "do the domain/application tests actually assert anything" (mutation score, enforced). Presenting both side by side is a good way to explain why a team needs more than one coverage metric — line coverage alone can't tell you a test is a no-op assertion-free pass-through, which is the standard critique of coverage-as-a-target.

## A quiet fact underneath both

- Every current test class is named `*Test.java` (including the `@QuarkusTest` integration-style ones like `ThoughtApplicationServiceTest`, `PanacheThoughtRepositoryTest`), so they all run under Surefire. `maven-failsafe-plugin` is configured (bound to `integration-test`/`verify`) but there are zero `*IT.java` files, and `<skipITs>true</skipITs>` is set anyway — so Failsafe is currently inert scaffolding. That means right now, JaCoCo's one `prepare-agent` execution captures 100% of what actually runs; the Failsafe wiring is prepared for later (maybe issue 8–9's admin tests, or genuine integration tests) but isn't doing anything yet. Worth a one-line mention if someone asks "why is there a failsafe plugin with nothing using it" — it's forward scaffolding, not dead config to clean up.

## What's missing / open

- Like PIT, no report has actually been generated or reviewed in this environment (offline sandbox, can't pull the Quarkus BOM) — there's no real coverage percentage to quote. Say "here's the report it produces," not "here's our coverage number."
- Unlike the ArchUnit story, there's no commit demonstrating JaCoCo doing anything — which is consistent with it being a report, not a gate. If someone in the audience asks "what happens if coverage drops," the honest answer is: nothing automated does; `./mvnw verify` still passes. That's a deliberate design choice stated in the docs, not an oversight, and it's a good contrast to lead with right after the PIT talking points, since it's the one guardrail in this project that's explicitly descriptive rather than enforced.
