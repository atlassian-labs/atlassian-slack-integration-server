#!/usr/bin/env bash

# 'baseurl.<productId>' address ends up with port 0 if app.startup.skip=true, so we need to define it here
BASE_URL="http://127.0.0.1:7990/bitbucket"

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;

    atlas-mvn --batch-mode verify \
      -Dut.test.skip=true \
      -Dit.test.skip=false \
      -Dapp.startup.skip=true \
      -Dbaseurl.bitbucket=${BASE_URL} \
      -Dxvfb.enable=${XVFB_ENABLE:-true} \
      -Datlassian.plugins.enable.wait=300 \
      -Dlogging.level.it.com.atlassian.bitbucket.plugins.slack=TRACE \
      -Dlogging.level.com.atlassian.bitbucket.plugins.slack=TRACE \
      -Dlogging.level.com.atlassian.plugins.slack=TRACE \
      -Dlogging.level.com.github.seratach.jslack.maintainer.json=DEBUG \
      --projects bitbucket-slack-server-integration-plugin \
      --activate-profiles include-common \
      "$@" \
      | tee bitbucket-int.log
)
