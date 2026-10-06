# thoughtsapp-monolith-scaffold

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/thoughtsapp-monolith-scaffold-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Running the full gate without a local JDK

`./mvnw verify` (unit tests, `@QuarkusTest` integration tests, ArchUnit, PIT mutation testing, JaCoCo) needs a JDK 25. If you don't have one installed, `./scripts/test-in-sandbox.sh` runs the same gate inside a pinned Maven + JDK 25 container, using only your local Docker daemon (Quarkus Dev Services' PostgreSQL still starts as a sibling container through the usual Testcontainers mechanism). Extra arguments pass through to Maven before the `verify` goal, e.g.:

```shell script
./scripts/test-in-sandbox.sh -Dtest=ThoughtTest#approveFromInReviewSucceeds
```

This is also the command the AI PR reviewer below runs.

## PR review (AI, in a Docker Sandbox)

When a pull request is opened, a Copilot agent reviews it **inside a Docker Sandbox microVM** and posts its findings as one sticky comment. The agent reads the diff, checks it against `spec.md`'s acceptance criteria and the architecture rules in `CLAUDE.md`/`AGENTS.md`, runs `./scripts/test-in-sandbox.sh` (the project's full `./mvnw verify` gate), and writes a review. It never gets a token that can write to the repository.

Ported from the sibling `pr-reviewer` demo (same building blocks: a sandboxed Copilot agent, a sticky comment, a trusted/untrusted job split), adapted so the review checks this project's actual definition of done instead of a generic `REQUIREMENTS.md`.

```
pull_request_target ──► prepare ──► review ──► comment
(or workflow_dispatch)  decide      agent in    sanitize, then create or
                        (gates)     microVM     update ONE bot comment
```

| Job | Token | What it does |
| :-- | :-- | :-- |
| `prepare` | `contents: read`, `pull-requests: read` | Decides whether to review (kill switch, bots, drafts, size, trust mode). Runs only trusted code. |
| `review` | `contents: read` | Checks out the trusted scripts and the pull request (separately), starts a Docker Sandbox with a clone of the pull request, runs the agent, and copies out one file, `REVIEW.md`. |
| `comment` | `pull-requests: write` | Sanitizes the review and creates or updates a single comment owned by the Actions bot. Runs no agent. |

### Why it is safe to run on pull requests from strangers

The workflow uses `pull_request_target`, so pull requests from forks can use the repository's secrets. That is only safe because of how the work is split.

| Risk | Control |
| :-- | :-- |
| The pull request's code runs on the runner | It never does. It is checked out into `./pr` and only handed to `sbx create --clone`, so it runs only in the microVM. Scripts, prompt and actions always come from the base branch. |
| Secrets stolen by the pull request's code | No secret is inside the sandbox. The Copilot token lives in sbx's host-side proxy (the sandbox sees a placeholder), and the Docker token stays on the runner. The Copilot token only has the Copilot Requests permission. |
| Prompt injection in the diff, title or description | The output is advisory text only. The agent job has a read-only token. The prompt marks pull request text as untrusted, and the comment is sanitized before it is posted. |
| Abusive or spammy comment text | `sanitize.sh` caps the length, breaks `@mentions`, escapes HTML comments so the marker can't be forged, and removes images. Only comments owned by `github-actions[bot]` that start with the marker are ever edited. |
| Quota burn | Per-PR concurrency (a new push cancels the old review), a 35 minute agent timeout, a size cap, draft and bot skips, a kill switch, and a trust mode. Code in the sandbox could still call Copilot with the injected token until the timeout. |
| Shell injection from pull request text | Title, body and branch names reach scripts only through `env:`, never inside `run:`. `resolve-pr.sh` validates identifiers and single-lines free text. |

### Setup

1. **Secrets** (`gh secret set <NAME>`):
   - `DOCKER_USERNAME`, `DOCKER_PAT`: a Docker Hub account and a token with at least **Read** scope. sbx needs these to start.
   - `COPILOT_GITHUB_TOKEN`: a fine-grained PAT owned by a personal account with the **Copilot Requests** permission and nothing else. The account needs Copilot. Classic `ghp_` tokens don't work with Copilot CLI.
2. **Label:** `gh label create ai-review --description "Request an AI review"`.
3. **Repository variables** (all optional, in Settings → Secrets and variables → Actions → Variables):

   | Variable | Values | Effect |
   | :-- | :-- | :-- |
   | `AI_REVIEW_ENABLED` | `false` | Kill switch: nothing is reviewed. |
   | `AI_REVIEW_TRUST` | `everyone` (default) or `trusted` | `trusted` reviews owners, members and collaborators automatically; other authors wait for a maintainer to add the `ai-review` label. **Recommended for this repo:** set this to `trusted` — unlike the `pr-reviewer` demo, this is a real, actively-developed repo with no public-contribution surface to showcase. |
   | `AI_REVIEW_TEST_COMMAND` | a command, or `none` | What the reviewer runs. Default `./scripts/test-in-sandbox.sh`. `none` means read-only review. |

4. `pull_request_target` only uses the workflow on the **default branch**, so merge it there before expecting automatic reviews.

### Who can trigger a review

With `AI_REVIEW_TRUST=trusted` (the recommended setting above): owners, members and collaborators are reviewed automatically; anyone else waits for a maintainer to add the `ai-review` label. Changing it needs no code change.

### How to use it

- **Automatic:** open or update a pull request. Draft pull requests wait until they are ready for review.
- **Re-review on demand:** add (or re-add) the `ai-review` label.
- **Manual:** `gh workflow run pr-review.yml -f pr-number=<number>`. Use this to try it on an existing pull request, or to review pull requests opened by `GITHUB_TOKEN` (GitHub doesn't start workflows for those).
- **Result:** one comment titled "AI review (advisory)" with Summary, Risk, Findings, Tests and Notes. It is edited in place on every push. It never approves or blocks the pull request.

### Runner requirements

KVM (`/dev/kvm`), passwordless sudo, Docker Engine and Ubuntu 24.04 or later. Nested virtualization on GitHub-hosted runners isn't officially guaranteed: run `kvm-probe.yml` first. Pin sbx with the `sbx-version` input of the `sbx-agent` action, because sbx releases often.

### Status and open items

**Not yet run on GitHub**, but `./scripts/test-in-sandbox.sh` has been verified locally end-to-end against the real app (Docker only, no local JDK): `./tests/run.sh` (the workflow's own script tests, 169 checks) and the full `./mvnw verify` gate both pass. From that local run:

- **Total time: 10:44 min** for `./mvnw verify` with a cold dependency cache and a cold PostgreSQL image pull — comfortably inside the 35 minute agent timeout / 40 minute job timeout. All 101 unit/integration tests passed, including `VotingConcurrencyTest` and `LayeredArchitectureTest`. PIT scored **100% (55/55 mutations killed, 76 tests, 100% test strength)**, and JaCoCo's report generated cleanly.
- Quarkus Dev Services pulls `docker.io/library/postgres:17` — plain Docker Hub, already covered by the existing `registry-1.docker.io`/`auth.docker.io` allow-rules. No `quay.io` entry needed (confirmed, not just assumed).
- `registry.quarkus.io` was never contacted during the build (confirmed by grepping the full log).

Still to confirm on the first live GitHub run: wall-clock time inside the actual `sbx` microVM (the agent also reads files and writes the review, and the GitHub Actions runner won't have a warm image/dependency cache like this local run did), and that the sanitized review renders well in a comment.

## Related Guides

- Qute ([guide](https://quarkus.io/guides/qute)): Offer templating support for web, email, etc in a build time, type-safe way
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplify your persistence code for Hibernate ORM via the active record or the repository pattern
- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Monitor service health
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes with Swagger UI
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
- Micrometer metrics ([guide](https://quarkus.io/guides/micrometer)): Instrument the runtime and your application with dimensional metrics using Micrometer.

## Provided Code

### Hibernate ORM

Create your first JPA entity

[Related guide section...](https://quarkus.io/guides/hibernate-orm)

[Related Hibernate with Panache section...](https://quarkus.io/guides/hibernate-orm-panache)


### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### SmallRye Health

Monitor your application's health using SmallRye Health

[Related guide section...](https://quarkus.io/guides/smallrye-health)
