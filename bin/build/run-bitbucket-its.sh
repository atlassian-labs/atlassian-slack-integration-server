#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

VERSION_ARG=$([[ -z ${VERSION} ]] && echo "" || echo "-Dbitbucket.version=${VERSION}")

if [[ ${XVFB_ENABLE} != false ]] ; then
    export DISPLAY=:20
fi

atlas-mvn --batch-mode verify \
  ${VERSION_ARG} \
  -Dut.test.skip=true \
  -Dit.test.skip=false \
  -Dxvfb.enable=${XVFB_ENABLE:-true} \
  -Datlassian.plugins.enable.wait=300 \
  -Dlogging.level.it.com.atlassian.bitbucket.plugins.slack=TRACE \
  -Dlogging.level.com.atlassian.bitbucket.plugins.slack=TRACE \
  -Dlogging.level.com.atlassian.plugins.slack=TRACE \
  -Dlogging.level.com.github.seratach.jslack.maintainer.json=DEBUG \
  -Dfailsafe.rerunFailingTestsCount=${RETRY_COUNT:-2} \
  -Dfailsafe.forkedProcessExitTimeoutInSeconds=360 \
  -Dfailsafe.exitTimeout=360 \
  --projects bitbucket-slack-server-integration-plugin \
  --activate-profiles include-common \
  "$@"
