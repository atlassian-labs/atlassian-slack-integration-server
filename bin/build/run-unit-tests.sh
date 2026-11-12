#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

atlas-version
atlas-mvn --batch-mode --fail-at-end verify -P jacoco
