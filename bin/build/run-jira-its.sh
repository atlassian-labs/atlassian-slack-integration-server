#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

VERSION_ARG=$([[ -z ${VERSION} ]] && echo "" || echo "-Djira.version=${VERSION}")
TESTKIT_VERSION_ARG=$([[ -z ${TESTKIT_VERSION} ]] && echo "" || echo "-Dtestkit.version=${TESTKIT_VERSION}")

export DANGER_MODE=true

if [[ ${XVFB_ENABLE} != false ]] ; then
    export DISPLAY=:20
fi

atlas-mvn --batch-mode verify \
  ${VERSION_ARG} \
  ${TESTKIT_VERSION_ARG} \
  -Dut.test.skip=true \
  -Dit.test.skip=false \
  -Dxvfb.enable=${XVFB_ENABLE:-true} \
  -Datlassian.plugins.enable.wait=300 \
  -Dserver=${HOST_NAME:-localhost} \
  -Dfailsafe.rerunFailingTestsCount=${RETRY_COUNT:-2} \
  -Dfailsafe.forkedProcessExitTimeoutInSeconds=360 \
  -Dfailsafe.exitTimeout=360 \
  -pl jira-slack-server-integration/jira-slack-server-integration-plugin \
  "$@"
