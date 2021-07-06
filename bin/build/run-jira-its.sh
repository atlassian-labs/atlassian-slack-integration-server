#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

TOMCAT_VERSION=${TOMCAT_VERSION:-tomcat8x}

# Support Jira 8
if [[ ${VERSION} == 8* ]] ; then
    TESTKIT_VERSION=${TESTKIT_VERSION:-8.1.28}
fi

# Version less than or equal
# https://stackoverflow.com/a/4024263
verlte() {
    [  "$1" = "$(echo -e "$1\n$2" | sort -V | head -n1)" ]
}

VERSION_ARG=$([[ -z ${VERSION} ]] && echo "" || echo "-Djira.version=${VERSION} -Dproduct.version=${VERSION}")
TESTKIT_VERSION_ARG=$([[ -z ${TESTKIT_VERSION} ]] && echo "" || echo "-Dtestkit.version=${TESTKIT_VERSION}")
# override Selenium version for Jira versions starting with 8.17.1
SELENIUM_VERSION_ARG=$([[ -z ${VERSION} ]] || verlte ${VERSION} '8.17.0' && echo "" || echo "-Datlassian.selenium.version=3.2.4")

export DANGER_MODE=true

if [[ ${XVFB_ENABLE} != false ]] ; then
    export DISPLAY=:20
fi

atlas-mvn --batch-mode verify \
  ${VERSION_ARG} \
  ${TESTKIT_VERSION_ARG} \
  ${SELENIUM_VERSION_ARG} \
  -Dut.test.skip=true \
  -Dit.test.skip=false \
  -Dcontainer=${TOMCAT_VERSION} \
  -Dxvfb.enable=${XVFB_ENABLE:-true} \
  -Datlassian.plugins.enable.wait=300 \
  -Dserver=${HOST_NAME:-localhost} \
  -Dfailsafe.rerunFailingTestsCount=${RETRY_COUNT:-2} \
  -Dfailsafe.forkedProcessExitTimeoutInSeconds=360 \
  -Dfailsafe.exitTimeout=360 \
  -pl jira-slack-server-integration/jira-slack-server-integration-plugin \
  "$@"
