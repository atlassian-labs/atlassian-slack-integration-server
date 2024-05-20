#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

VERSION_ARG=$([[ -z ${VERSION} ]] && echo "" || echo "-Dconfluence.version=${VERSION}")

if [[ ${XVFB_ENABLE} != false ]] ; then
    export DISPLAY=:20
fi

# TODO: Remove -Denforcer.skip=true after moving from milestone versions

atlas-mvn --batch-mode verify \
  ${VERSION_ARG} \
  -Dut.test.skip=true \
  -Dit.test.skip=false \
  -Dxvfb.enable=${XVFB_ENABLE:-true} \
  -Datlassian.plugins.enable.wait=30 \
  -Dserver=${HOST_NAME:-localhost} \
  -Dfailsafe.rerunFailingTestsCount=${RETRY_COUNT:-2} \
  -Dfailsafe.forkedProcessExitTimeoutInSeconds=360 \
  -Dfailsafe.exitTimeout=360 \
  -Denforcer.skip=true \
  -pl confluence-slack-integration/confluence-slack-server-integration-plugin \
  "$@"
