# PIT mutation testing: talking points

Source: `pom.xml` (`pitest-maven` plugin, bound to `verify`), plus the domain test classes under `src/test/java/io/arrogantprogrammer/thoughts/domain/` and `application/`.

## What it is

- `pitest-maven`, bound to the `verify` phase in `pom.xml` (~lines 170–190), wired up in commit `66e6ff0` ("Wire up PIT mutation testing and JaCoCo coverage") — the same commit that added `.github/workflows/ci.yml`, so the mutation gate and CI were introduced together, not bolted on later.
- Scoped deliberately narrow: `targetClasses`/`targetTests` are only `domain.*` and `application.*`. Adapters are excluded on purpose — they're exercised by `@QuarkusTest` integration tests against a real (Dev Services) Postgres, which is the wrong cost profile for PIT's run-every-test-per-mutant model.
- `mutationThreshold=80` — a hard gate. Unlike JaCoCo (also wired in the same commit, but only produces a report — "a floor, not a target," no enforced minimum), PIT actually fails the build if the mutation score drops below 80%.

## A nice sequencing story

- `failWhenNoMutations=false` was a necessary, deliberate choice: this gate was committed in issue 2, *before* issue 3 added the first real domain class (`66e6ff0` predates `c9c716a`, "Add Thought aggregate and value objects"). The commit message says so explicitly: "neither package has real classes yet." That's a good talking point about how this team built infrastructure-first — the gate existed and was already running in CI before there was anything to mutate.
- The 80% number is explicitly a placeholder, not a measured target — `spec.md`'s open questions say so verbatim: "PIT threshold of 80% is a guess; run PIT once on the skeleton's first real aggregate and set the number from that." That question is still open; worth being upfront on stage that nobody has re-calibrated it against real mutation data yet.

## The tests are written like they expect to be mutated, not just covered

This is the strongest evidence-based point, and it shows in the actual test code:

- `ContentTest` tests exactly 10 and 500 chars (valid) *and* exactly 9 and 501 (invalid) — that's a textbook boundary-mutator kill (PIT flips `<` to `<=`; these tests catch it on both edges).
- `ThoughtTest` doesn't just test the happy path through each status transition — it enumerates every legal transition *and* every illegal one (`approveFromApprovedFails`, `approveFromRemovedFails`, `restoreFromInReviewFails`, `restoreFromApprovedFails`, etc.), which kills negated-conditional and removed-guard mutants that a "does it work" test suite would miss.
- `RatingTest` checks `approvalRate()` at zero votes, all-up, all-down, and mixed — directly targeting arithmetic/conditional mutants in that calculation.

That's a good "show, don't tell": pull up `ContentTest` or `ThoughtTest` on screen and point at the paired valid/invalid boundary tests as the reason PIT can plausibly hit 80%, rather than asserting it does.

## A real nuance/caveat worth naming

- `application.*` is in PIT's scope, but `ThoughtApplicationServiceTest` is a `@QuarkusTest` hitting a real database (Dev Services Postgres), not a pure unit test. That means a PIT run against `application` will re-execute DB round-trips once per surviving mutant — a much heavier cost than the pure-JVM `domain` tests. This is a genuine open tension between "PIT covers domain and application" (the stated rule) and "PIT works best on fast, pure unit tests" (PIT's own design assumption). Good moment to be honest about a tradeoff rather than claim it's free.

## What's missing compared to the ArchUnit story

- There's no PIT equivalent of the ArchUnit "break it on purpose, watch it fail, revert" commit (`a8a8b59`/`d79205c`). Nobody has yet weakened a test and shown PIT/the threshold catching the surviving mutant. If you want a live-demo moment as strong as the ArchUnit one, that's the move: temporarily delete e.g. `rejectsNineCharactersAfterTrim` from `ContentTest`, run `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`, show a surviving boundary mutant in `target/pit-reports`, then restore the test.
- No mutation report has actually been generated or reviewed yet in this environment (sandbox is offline, can't pull the Quarkus BOM) — so there's no real score to quote on stage. Frame the 80% threshold as "the gate we've set," not "the score we've hit."
