#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

atlas-version
# Extra arguments are passed to Maven, e.g. -pl '!module' to leave a module out
atlas-mvn --batch-mode --fail-at-end verify -P jacoco "$@"
