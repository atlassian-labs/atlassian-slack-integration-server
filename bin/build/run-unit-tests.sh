#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

# TODO: Remove -Denforcer.skip=true after moving from milestone versions

atlas-version
atlas-mvn --batch-mode verify -P jacoco -Denforcer.skip=true
