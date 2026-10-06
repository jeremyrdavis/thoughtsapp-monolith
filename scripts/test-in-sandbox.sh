#!/usr/bin/env bash
# Runs the full `./mvnw verify` gate (unit tests, @QuarkusTest integration tests, ArchUnit,
# PIT mutation testing, JaCoCo) in a pinned container, so it needs only Docker, not a local
# JDK 25. Extra arguments are passed to Maven before the goal:
#   ./scripts/test-in-sandbox.sh -Dtest=ThoughtTest#approveFromInReviewSucceeds
set -euo pipefail

MAVEN_IMAGE="maven:3.9.11-eclipse-temurin-25@sha256:407c4423cec0cf2981055bc2c6c0dc211d9605b6669279b95997f2d1c7e91e2c"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Quarkus Dev Services starts PostgreSQL 17 as a sibling container (via Testcontainers),
# reached through the host gateway, the same pattern the registration-demo sample app uses.
# The image supplies curl + tar (no unzip, but mvnw falls back to the .tar.gz distribution)
# and Java 25, so the Maven wrapper (./mvnw) self-downloads its own Maven and runs the build
# with it, not the image's bundled Maven.
exec docker run --rm \
  -v "${repo_root}:/workspace" \
  -w /workspace \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v thoughtsapp-monolith-m2:/root/.m2 \
  --add-host=host.testcontainers.internal:host-gateway \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.testcontainers.internal \
  "${MAVEN_IMAGE}" \
  ./mvnw --batch-mode "$@" verify
