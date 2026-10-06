# ArchUnit: talking points

Source: `src/test/java/io/arrogantprogrammer/thoughts/architecture/LayeredArchitectureTest.java`.

## What it is

- 5 ArchUnit rules, run as a normal JUnit test inside `./mvnw verify` — no separate linting step, no human sign-off. CI and the review agent get the same pass/fail signal.
- It checks the hexagonal boundary from `docs/adr/0001-hexagonal-monolith.md` mechanically: domain → nothing but JDK, application → domain only, inbound adapters → application only, outbound adapters never depend on inbound adapters, and nothing in `domain`/`application` references `adapters.*` at all (that last rule is a blanket backstop over the first four).
- One rule checks a *return type*, not just imports: `applicationServiceMethodsDoNotReturnDomainTypes` fails if any public method in `application` returns something from `domain` — this is what forces `ThoughtApplicationService` to hand back `ThoughtDTO`, never a `Thought` aggregate, to any adapter.

## The proof, not just the claim

- This isn't theoretical — there's a real commit that proves it: `a8a8b59` ("Deliberately violate layering to prove ArchUnit catches it") adds a two-line violation (`domain.ViolationUser` references `adapters.out.persistence.ViolationProbe`), the commit message states which two rules fail, and `d79205c` reverts it in the next commit. That's issue 1's acceptance criterion satisfied in the commit history itself — great "show, don't tell" moment for the talk: `git show a8a8b59` live.
- Good talking point on agent behavior: this means a coding agent didn't just write a test and trust it — it proved the test has teeth by breaking the rule on purpose and watching it fail, then cleaning up. That's the mutation-testing mindset (PIT, issue 2) applied informally to an architecture test before PIT even covers it.

## Why hexagonal + ArchUnit, specifically, for this talk

- The talk's abstract commits to "enforcing architectural boundaries with automated tests." Hexagonal was chosen (per ADR-001) specifically *because* its boundaries reduce to package-dependency rules a machine can check — not because it's the "right" architecture for a thoughts app.
- It converts a code-review judgment call ("does this belong in the domain layer?") into a build failure, which matters for a multi-agent workflow: the review agent doesn't need to reason about layering by reading diffs, it just runs `verify`.

## What it deliberately does NOT cover yet

- Three rules from ADR-001 / `CLAUDE.md` are enforced only by convention + review, not by ArchUnit: no Panache/JAX-RS/CDI-event imports in domain/application, ports living in domain vs. implemented in adapters, and the Panache entity being adapter-only (not the aggregate). Worth naming on stage as "here's the line between what the machine checks and what we still trust the agent/reviewer to catch" — a good segue into the PIT mutation-testing section if that's next in the talk.
- The events package (`adapters.out.events`) is currently just a `package-info.java` — there's no CDI publisher yet (issue 10), so there's nothing wired up for the outbound-adapter rule to meaningfully protect on the events side yet; it's currently only exercising the persistence adapter.

## One-liner if you need to cut for time

"We didn't just write an architecture test and hope — we broke the architecture on purpose in one commit and reverted it in the next, so the acceptance criteria is literally in the git log."
