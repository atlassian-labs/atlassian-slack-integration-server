#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

atlas-version
atlas-mvn --batch-mode verify -P jacoco
